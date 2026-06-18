package com.construction.diary.cloud.cloud

import android.content.Context
import android.util.Log
import com.construction.diary.cloud.BuildConfig
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.*
import com.construction.diary.cloud.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import org.json.JSONObject

data class TableResult(
    val tableName: String,
    val displayName: String,
    val pulled: Int,
    val pushed: Int,
    val errors: Int,
    val errorDetail: String? = null
)

data class SyncResult(
    val success: Boolean,
    val tableResults: List<TableResult>,
    val errorMessage: String? = null
) {
    val totalPushed: Int get() = tableResults.sumOf { it.pushed }
    val totalPulled: Int get() = tableResults.sumOf { it.pulled }
    val totalErrors: Int get() = tableResults.sumOf { it.errors }
    val summary: String
        get() {
            if (!success) return "同步失败: ${errorMessage ?: "未知错误"}"
            return "拉取 $totalPulled 条，推送 $totalPushed 条" +
                    if (totalErrors > 0) "，${totalErrors} 条错误" else ""
        }
}

typealias SyncProgressCallback = (tableDisplayName: String, currentIndex: Int, totalTables: Int) -> Unit

object SupabaseSyncManager {
    private const val TAG = "SupabaseSync"
    private const val PREFS_NAME = "cloud_config"
    private const val KEY_URL = "supabase_url"
    private const val KEY_API_KEY = "supabase_key"
    private const val KEY_ACCESS_TOKEN = "supabase_token"
    private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
    private const val KEY_USER_ID = "supabase_user_id"
    private const val KEY_EMAIL = "supabase_email"
    private const val KEY_LAST_SYNC = "last_sync_time"

    private var api: SupabaseApi? = null
    private var cachedAccessToken: String? = null
    private var cachedRefreshToken: String? = null

    /** 共享 OkHttpClient，避免每个方法重复创建 */
    private val sharedClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
        // 仅 Debug 构建开启 HTTP 日志，避免 Release 泄露 token
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }
        builder.build()
    }

    private fun getConfig(context: Context): Triple<String, String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val url = prefs.getString(KEY_URL, "") ?: ""
        val key = prefs.getString(KEY_API_KEY, "") ?: ""
        val token = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        return Triple(url, key, token)
    }

    private fun getApi(baseUrl: String): SupabaseApi {
        if (api == null) {
            api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(sharedClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseApi::class.java)
        }
        return api!!
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun saveTokens(context: Context, accessToken: String, refreshToken: String?, userId: String?, email: String?) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        val editor = getPrefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken != null) editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        if (userId != null) editor.putString(KEY_USER_ID, userId)
        if (email != null) editor.putString(KEY_EMAIL, email)
        editor.apply()
    }

    private fun getStoredRefreshToken(context: Context): String {
        if (cachedRefreshToken != null) return cachedRefreshToken!!
        val token = getPrefs(context).getString(KEY_REFRESH_TOKEN, "") ?: ""
        cachedRefreshToken = token
        return token
    }

    private fun getStoredAccessToken(context: Context): String {
        if (cachedAccessToken != null) return cachedAccessToken!!
        val token = getPrefs(context).getString(KEY_ACCESS_TOKEN, "") ?: ""
        cachedAccessToken = token
        return token
    }

    fun getUserId(context: Context): String {
        return getPrefs(context).getString(KEY_USER_ID, "") ?: ""
    }

    fun getLastSyncTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC, 0L)
    }

    private fun setLastSyncTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SYNC, time).apply()
    }

    suspend fun signIn(context: Context, email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val (url, key, _) = getConfig(context)
        if (url.isEmpty() || key.isEmpty()) return@withContext false
        try {
            val resp = getApi(url).signIn(key, SignInRequest(email, password))
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body?.accessToken != null) {
                    saveTokens(context, body.accessToken, body.refreshToken, body.user?.id, email)
                    Log.d(TAG, "Signed in as $email, userId=${body.user?.id}")
                    return@withContext true
                }
            }
            Log.e(TAG, "Sign in failed: ${resp.errorBody()?.string()}")
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
        }
        return@withContext false
    }

    suspend fun signUp(context: Context, email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val (url, key, _) = getConfig(context)
        if (url.isEmpty() || key.isEmpty()) return@withContext false
        try {
            val resp = getApi(url).signUp(key, SignUpRequest(email, password))
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body?.accessToken != null) {
                    saveTokens(context, body.accessToken, body.refreshToken, body.user?.id, email)
                    return@withContext true
                }
                Log.d(TAG, "Sign up successful, may need email confirmation")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
        }
        return@withContext false
    }

    private suspend fun refreshAccessToken(context: Context): Boolean {
        val (url, key, _) = getConfig(context)
        val refreshToken = getStoredRefreshToken(context)
        if (url.isEmpty() || key.isEmpty() || refreshToken.isEmpty()) return false
        return try {
            val resp = getApi(url).refreshToken(key, RefreshTokenRequest(refreshToken))
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body?.accessToken != null) {
                    saveTokens(context, body.accessToken, body.refreshToken, null, null)
                    Log.d(TAG, "Token refreshed successfully")
                    return true
                }
            }
            Log.e(TAG, "Token refresh failed: ${resp.errorBody()?.string()}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error", e)
            false
        }
    }

    /** Public wrapper for token refresh (used by CloudLoginActivity) */
    suspend fun tryRefreshToken(context: Context): Boolean {
        return withContext(Dispatchers.IO) { refreshAccessToken(context) }
    }

    private suspend fun <T> executeWithTokenRefresh(
        context: Context,
        apiCall: suspend (token: String) -> Response<T>
    ): Response<T> {
        val token = getStoredAccessToken(context)
        var response = apiCall("Bearer $token")

        if (response.code() == 401) {
            Log.d(TAG, "Got 401, attempting token refresh...")
            val refreshed = refreshAccessToken(context)
            if (refreshed) {
                val newToken = getStoredAccessToken(context)
                response = apiCall("Bearer $newToken")
            }
        }
        return response
    }

    suspend fun syncAll(
        context: Context,
        onProgress: SyncProgressCallback? = null,
        onLog: ((String) -> Unit)? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        val (url, key, _) = getConfig(context)
        if (url.isEmpty() || key.isEmpty()) {
            return@withContext SyncResult(false, emptyList(), "未配置 Supabase")
        }
        val token = getStoredAccessToken(context)
        if (token.isEmpty()) {
            return@withContext SyncResult(false, emptyList(), "未登录")
        }
        val userId = getUserId(context)
        onLog?.invoke("🔑 用户ID: $userId")
        if (userId.isEmpty()) {
            return@withContext SyncResult(false, emptyList(), "无法获取用户ID")
        }

        val db = AppDatabase.getInstance(context)
        val api = getApi(url)
        val results = mutableListOf<TableResult>()

        // Define all entity sync tasks: displayName, tableName, pullFn, pushFn
        data class SyncTask(
            val displayName: String,
            val tableName: String,
            val pullFn: suspend () -> Int,
            val pushFn: suspend () -> Pair<Int, Int>  // pushed, errors
        )

        val tasks = listOf(
            SyncTask("💰 支出", "expenses",
                pullFn = { pullExpenses(api, key, context, userId, db) },
                pushFn = { pushExpenses(api, key, context, userId, db, onLog) }
            ),
            SyncTask("🎁 礼金", "gift_money",
                pullFn = { pullGiftMoney(api, key, context, userId, db) },
                pushFn = { pushGiftMoney(api, key, context, userId, db, onLog) }
            ),
            SyncTask("🧱 材料", "materials",
                pullFn = { pullMaterials(api, key, context, userId, db) },
                pushFn = { pushMaterials(api, key, context, userId, db, onLog) }
            ),
            SyncTask("👷 工人", "workers",
                pullFn = { pullWorkers(api, key, context, userId, db) },
                pushFn = { pushWorkers(api, key, context, userId, db, onLog) }
            ),
            SyncTask("🏗️ 里程碑", "milestones",
                pullFn = { pullMilestones(api, key, context, userId, db) },
                pushFn = { pushMilestones(api, key, context, userId, db, onLog) }
            ),
            SyncTask("🏪 供应商", "suppliers",
                pullFn = { pullSuppliers(api, key, context, userId, db) },
                pushFn = { pushSuppliers(api, key, context, userId, db, onLog) }
            ),
            SyncTask("📝 施工日志", "daily_logs",
                pullFn = { pullDailyLogs(api, key, context, userId, db) },
                pushFn = { pushDailyLogs(api, key, context, userId, db, onLog) }
            ),
            SyncTask("✅ 验收清单", "checklist_items",
                pullFn = { pullChecklistItems(api, key, context, userId, db) },
                pushFn = { pushChecklistItems(api, key, context, userId, db, onLog) }
            ),
            SyncTask("🏠 房间", "house_rooms",
                pullFn = { pullHouseRooms(api, key, context, userId, db) },
                pushFn = { pushHouseRooms(api, key, context, userId, db, onLog) }
            ),
            SyncTask("⚙️ 项目配置", "project_config",
                pullFn = { pullProjectConfig(api, key, context, userId, db) },
                pushFn = { pushProjectConfig(api, key, context, userId, db, onLog) }
            ),
            SyncTask("📺 电器大件", "appliances",
                pullFn = { pullAppliances(api, key, context, userId, db) },
                pushFn = { pushAppliances(api, key, context, userId, db, onLog) }
            ),
            SyncTask("📋 证件资料", "documents",
                pullFn = { pullDocuments(api, key, context, userId, db) },
                pushFn = { pushDocuments(api, key, context, userId, db, onLog) }
            ),
            SyncTask("🔧 维修记录", "maintenance",
                pullFn = { pullMaintenance(api, key, context, userId, db) },
                pushFn = { pushMaintenance(api, key, context, userId, db, onLog) }
            ),
            SyncTask("🏠 户型图", "floor_plan",
                pullFn = { pullFloorPlan(api, key, context, userId, db) },
                pushFn = { pushFloorPlan(api, key, context, userId, db, onLog) }
            ),
            SyncTask("📸 进度照片", "standalone_photos",
                pullFn = { pullStandalonePhotos(api, key, context, userId, db) },
                pushFn = { pushStandalonePhotos(api, key, context, userId, db, onLog) }
            ),
            SyncTask("💰 出资记录", "funding_sources",
                pullFn = { pullFundingSources(api, key, context, userId, db) },
                pushFn = { pushFundingSources(api, key, context, userId, db, onLog) }
            )
        )

        try {
            for ((index, task) in tasks.withIndex()) {
                onProgress?.invoke(task.displayName, index, tasks.size)
                Log.d(TAG, "Syncing ${task.displayName}...")

                try {
                    onLog?.invoke("🔄 同步 ${task.displayName}...")
                    val pulled = task.pullFn()
                    val (pushed, errors) = task.pushFn()
                    results.add(TableResult(task.tableName, task.displayName, pulled, pushed, errors))
                    val msg = "${task.displayName}: 拉${pulled} 推${pushed}" + if (errors > 0) " 错${errors}" else ""
                    Log.d(TAG, msg)
                    if (errors > 0) onLog?.invoke("⚠️ $msg") else onLog?.invoke("✅ $msg")
                } catch (e: Exception) {
                    val errDetail = "${e.javaClass.simpleName}: ${e.message}"
                    Log.e(TAG, "Failed to sync ${task.displayName}: $errDetail", e)
                    onLog?.invoke("❌ ${task.displayName} 异常: $errDetail")
                    results.add(TableResult(task.tableName, task.displayName, 0, 0, 1, errDetail))
                }
            }

            onProgress?.invoke("完成", tasks.size, tasks.size)
            setLastSyncTime(context, System.currentTimeMillis())
            return@withContext SyncResult(true, results)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            return@withContext SyncResult(false, results, e.message)
        }
    }

    // ============ Pull methods: cloud → local ============

    private fun safeLong(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun safeDouble(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun safeInt(value: Any?): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun safeBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> false
        }
    }

    private fun safeString(value: Any?): String {
        return value?.toString() ?: ""
    }

    private suspend fun pullExpenses(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "expenses", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) {
            Log.e(TAG, "Pull expenses failed: ${resp.code()}")
            return 0
        }
        val records = resp.body() ?: emptyList()
        Log.d(TAG, "[支出拉取] 云端返回 ${records.size} 条")
        if (records.isNotEmpty()) {
            Log.d(TAG, "[支出拉取] 云端字段: ${records[0].keys.toList()}")
        }
        val dao = db.expenseDao()
        val localMap = dao.getAllList().associateBy { it.id }

        // 扫描 Storage 中的图片（修复：图片在 Storage 但数据库为空的情况）
        val storageImages = scanStorageForTable(context, userId, "expenses")

        for (record in records) {
            val id = safeLong(record["id"])
            val rawImages = safeString(record["image_paths"])
            // 如果 rawImages 是本地文件名（不含/），优先用 Storage 扫描结果
            val effectiveImages = if (rawImages.contains("/") || rawImages.startsWith("http")) rawImages
                else storageImages[id] ?: rawImages
            val localImages = if (hasSupabaseUrls(effectiveImages)) downloadImages(context, effectiveImages, userId) else effectiveImages
            // 当云端为空时保留本地图片路径
            val existingImages = localMap[id]?.imagePaths ?: ""
            val finalImages = if (localImages.isEmpty() && existingImages.isNotEmpty()) existingImages else localImages
            val entity = Expense(
                id = id,
                itemName = safeString(record["item_name"]),
                brand = safeString(record["brand"]),
                amount = safeDouble(record["amount"]),
                buyer = safeString(record["buyer"]),
                phase = safeString(record["phase"]),
                category = safeString(record["category"]),
                roomId = safeLong(record["room_id"]),
                supplierId = safeLong(record["supplier_id"]),
                date = safeLong(record["date"]),
                note = safeString(record["note"]),
                imagePaths = finalImages,
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullGiftMoney(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "gift_money", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.giftMoneyDao()
        val localMap = dao.getAllList().associateBy { it.id }

        for (record in records) {
            val id = safeLong(record["id"])
            val entity = GiftMoney(
                id = id,
                giverName = safeString(record["giver_name"]),
                relationship = safeString(record["relationship"]),
                amount = safeDouble(record["amount"]),
                occasion = safeString(record["occasion"]),
                date = safeLong(record["date"]),
                note = safeString(record["note"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullFundingSources(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "funding_sources", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.fundingSourceDao()
        val localMap = dao.getAllList().associateBy { it.id }

        for (record in records) {
            val id = safeLong(record["id"])
            val entity = FundingSource(
                id = id,
                contributorName = safeString(record["contributor_name"]),
                relationship = safeString(record["relationship"]),
                amount = safeDouble(record["amount"]),
                date = safeLong(record["date"]),
                note = safeString(record["note"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullMaterials(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "materials", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.materialDao()
        val localMap = dao.getAllList().associateBy { it.id }

        for (record in records) {
            val id = safeLong(record["id"])
            val entity = Material(
                id = id,
                name = safeString(record["name"]),
                spec = safeString(record["spec"]),
                quantity = safeString(record["quantity"]),
                budgetAmount = safeDouble(record["budget_amount"]),
                actualAmount = safeDouble(record["actual_amount"]),
                buyer = safeString(record["buyer"]),
                phase = safeString(record["phase"]),
                status = safeInt(record["status"]),
                note = safeString(record["note"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullWorkers(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "workers", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.workerDao()
        val localMap = dao.getAllList().associateBy { it.id }

        for (record in records) {
            val id = safeLong(record["id"])
            val entity = Worker(
                id = id,
                name = safeString(record["name"]),
                phone = safeString(record["phone"]),
                role = safeString(record["role"]),
                dailyWage = safeDouble(record["daily_wage"]),
                note = safeString(record["note"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullMilestones(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "milestones", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.milestoneDao()
        val localMap = dao.getAllList().associateBy { it.id }
        val storageImages = scanStorageForTable(context, userId, "milestones")

        for (record in records) {
            val id = safeLong(record["id"])
            val rawImages = safeString(record["image_paths"])
            val effectiveImages = if (rawImages.contains("/") || rawImages.startsWith("http")) rawImages
                else storageImages[id] ?: rawImages
            val localImages = if (hasSupabaseUrls(effectiveImages)) downloadImages(context, effectiveImages, userId) else effectiveImages
            // 当云端为空时保留本地图片路径
            val existingImages = localMap[id]?.imagePaths ?: ""
            val finalImages = if (localImages.isEmpty() && existingImages.isNotEmpty()) existingImages else localImages
            val entity = Milestone(
                id = id,
                title = safeString(record["title"]),
                description = safeString(record["description"]),
                date = safeLong(record["date"]),
                imagePaths = finalImages,
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullSuppliers(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "suppliers", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.supplierDao()
        val localMap = dao.getAllList().associateBy { it.id }
        val storageImages = scanStorageForTable(context, userId, "suppliers")

        for (record in records) {
            val id = safeLong(record["id"])
            val rawQr = safeString(record["qr_code_path"])
            val effectiveQr = if (rawQr.contains("/") || rawQr.startsWith("http")) rawQr
                else storageImages[id] ?: rawQr
            Log.d(TAG, "[供应商拉取] ID=$id, rawQr='$rawQr', storageLookup=${storageImages[id]}, effectiveQr='$effectiveQr'")
            val localQr = if (hasSupabaseUrls(effectiveQr)) downloadImages(context, effectiveQr, userId) else effectiveQr
            // 当云端为空时保留本地图片路径
            val existingQr = localMap[id]?.qrCodePath ?: ""
            val finalQr = if (localQr.isEmpty() && existingQr.isNotEmpty()) existingQr else localQr
            val entity = Supplier(
                id = id,
                name = safeString(record["name"]),
                contact = safeString(record["contact"]),
                phone = safeString(record["phone"]),
                address = safeString(record["address"]),
                category = safeString(record["category"]),
                qrCodePath = finalQr,
                note = safeString(record["note"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullDailyLogs(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "daily_logs", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.dailyLogDao()
        val localMap = dao.getAllList().associateBy { it.id }
        val storageImages = scanStorageForTable(context, userId, "daily_logs")

        for (record in records) {
            val id = safeLong(record["id"])
            val rawImages = safeString(record["image_paths"])
            val effectiveImages = if (rawImages.contains("/") || rawImages.startsWith("http")) rawImages
                else storageImages[id] ?: rawImages
            val localImages = if (hasSupabaseUrls(effectiveImages)) downloadImages(context, effectiveImages, userId) else effectiveImages
            // 当云端为空时保留本地图片路径
            val existingImages = localMap[id]?.imagePaths ?: ""
            val finalImages = if (localImages.isEmpty() && existingImages.isNotEmpty()) existingImages else localImages
            val entity = DailyLog(
                id = id,
                date = safeLong(record["date"]),
                content = safeString(record["content"]),
                weather = safeString(record["weather"]),
                workers = safeString(record["workers"]),
                progress = safeInt(record["progress"]),
                phase = safeString(record["phase"]),
                imagePaths = finalImages,
                note = safeString(record["note"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullChecklistItems(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "checklist_items", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.checklistDao()
        val localMap = dao.getAllList().associateBy { it.id }

        for (record in records) {
            val id = safeLong(record["id"])
            val entity = ChecklistItem(
                id = id,
                phase = safeString(record["phase"]),
                title = safeString(record["title"]),
                description = safeString(record["description"]),
                isChecked = safeBoolean(record["is_checked"]),
                checkedDate = safeLong(record["checked_date"]),
                note = safeString(record["note"]),
                sortOrder = safeInt(record["sort_order"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullHouseRooms(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "house_rooms", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.roomDao()
        val localMap = dao.getAllList().associateBy { it.id }

        for (record in records) {
            val id = safeLong(record["id"])
            val entity = HouseRoom(
                id = id,
                name = safeString(record["name"]),
                area = safeString(record["area"]),
                status = safeInt(record["status"]),
                note = safeString(record["note"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    private suspend fun pullProjectConfig(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUserUnordered(key, token, "project_config", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) {
            Log.e(TAG, "Pull project_config failed: ${resp.code()}")
            return 0
        }
        val records = resp.body() ?: emptyList()
        Log.d(TAG, "[项目配置拉取] 云端返回 ${records.size} 条")
        val dao = db.projectConfigDao()
        for (record in records) {
            val cfgKey = safeString(record["key"])
            val cfgValue = safeString(record["value"])
            if (cfgKey.isNotEmpty()) {
                dao.set(ProjectConfig(key = cfgKey, value = cfgValue))
            }
        }
        return records.size
    }

    private suspend fun pullAppliances(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "appliances", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.applianceDao()
        val localMap = dao.getAllList().associateBy { it.id }
        val storageImages = scanStorageForTable(context, userId, "appliances")
        Log.d(TAG, "[电器拉取] storageImages keys=${storageImages.keys}, values=${storageImages.values}")
        for (record in records) {
            val id = safeLong(record["id"])
            val rawImages = safeString(record["image_paths"])
            val effectiveImages = if (rawImages.contains("/") || rawImages.startsWith("http")) rawImages
                else storageImages[id] ?: rawImages
            Log.d(TAG, "[电器拉取] ID=$id, rawImages='$rawImages', storageLookup=${storageImages[id]}, effectiveImages='$effectiveImages'")
            val localImages = if (hasSupabaseUrls(effectiveImages)) downloadImages(context, effectiveImages, userId) else effectiveImages
            val entity = Appliance(
                id = id,
                name = safeString(record["name"]),
                brand = safeString(record["brand"]),
                model = safeString(record["model"]),
                category = safeString(record["category"]),
                purchaseChannel = safeString(record["purchase_channel"]),
                purchaseDate = safeLong(record["purchase_date"]),
                price = safeDouble(record["price"]),
                warrantyYears = safeInt(record["warranty_years"]),
                warrantyExpireDate = safeLong(record["warranty_expire_date"]),
                installDate = safeLong(record["install_date"]),
                afterSalePhone = safeString(record["after_sale_phone"]),
                serialNumber = safeString(record["serial_number"]),
                note = safeString(record["note"]),
                imagePaths = localImages,
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) dao.update(entity) else dao.insert(entity)
        }
        return records.size
    }

    private suspend fun pullDocuments(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "documents", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.documentDao()
        val localMap = dao.getAllList().associateBy { it.id }
        val storageImages = scanStorageForTable(context, userId, "documents")
        for (record in records) {
            val id = safeLong(record["id"])
            val rawImages = safeString(record["image_paths"])
            val effectiveImages = if (rawImages.contains("/") || rawImages.startsWith("http")) rawImages
                else storageImages[id] ?: rawImages
            val localImages = if (hasSupabaseUrls(effectiveImages)) downloadImages(context, effectiveImages, userId) else effectiveImages
            val entity = Document(
                id = id,
                name = safeString(record["name"]),
                category = safeString(record["category"]),
                storageLocation = safeString(record["storage_location"]),
                relatedParty = safeString(record["related_party"]),
                issueDate = safeLong(record["issue_date"]),
                expireDate = safeLong(record["expire_date"]),
                note = safeString(record["note"]),
                imagePaths = localImages,
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) dao.update(entity) else dao.insert(entity)
        }
        return records.size
    }

    private suspend fun pullMaintenance(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "maintenance", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.maintenanceDao()
        val localMap = dao.getAllList().associateBy { it.id }
        val storageImages = scanStorageForTable(context, userId, "maintenance")
        for (record in records) {
            val id = safeLong(record["id"])
            val rawImages = safeString(record["image_paths"])
            val effectiveImages = if (rawImages.contains("/") || rawImages.startsWith("http")) rawImages
                else storageImages[id] ?: rawImages
            val localImages = if (hasSupabaseUrls(effectiveImages)) downloadImages(context, effectiveImages, userId) else effectiveImages
            // 当云端为空时保留本地图片路径
            val existingImages = localMap[id]?.imagePaths ?: ""
            val finalImages = if (localImages.isEmpty() && existingImages.isNotEmpty()) existingImages else localImages
            val entity = Maintenance(
                id = id,
                title = safeString(record["title"]),
                category = safeString(record["category"]),
                location = safeString(record["location"]),
                description = safeString(record["description"]),
                repairDate = safeLong(record["repair_date"]),
                cost = safeDouble(record["cost"]),
                workerName = safeString(record["worker_name"]),
                workerPhone = safeString(record["worker_phone"]),
                status = safeString(record["status"]),
                cause = safeString(record["cause"]),
                note = safeString(record["note"]),
                imagePaths = finalImages,
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) dao.update(entity) else dao.insert(entity)
        }
        return records.size
    }

    // ============ 户型图同步 ============

    /**
     * 从云端下载户型图到本地
     */
    private suspend fun pullFloorPlan(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        Log.d(TAG, "[户型图] 开始拉取")
        val (baseUrl, apiKey, _) = getConfig(context)
        val token = getStoredAccessToken(context)
        val client = sharedClient

        // 检查云端是否有户型图
        val storagePath = "$userId/floor_plan/floor_plan.jpg"
        Log.d(TAG, "[户型图] 检查云端: $storagePath")

        try {
            // 生成签名 URL
            val signBody = """{"expiresIn":3600}""".toRequestBody("application/json".toMediaTypeOrNull())
            val signRequest = Request.Builder()
                .url("$baseUrl/storage/v1/object/sign/$STORAGE_BUCKET/$storagePath")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $token")
                .post(signBody)
                .build()
            val signResp = client.newCall(signRequest).execute()
            if (!signResp.isSuccessful) {
                val errorBody = signResp.body?.string() ?: ""
                Log.d(TAG, "[户型图] 云端无户型图或签名失败: ${signResp.code} $errorBody")
                return 0
            }

            val signedPath = JSONObject(signResp.body?.string() ?: "").optString("signedURL", "")
            if (signedPath.isEmpty()) {
                Log.w(TAG, "[户型图] 签名响应为空")
                return 0
            }

            val downloadUrl = "$baseUrl/storage/v1$signedPath"
            Log.d(TAG, "[户型图] 下载中...")

            // 下载户型图
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val floorPlanFile = File(context.getExternalFilesDir(null), "floor_plan.jpg")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(floorPlanFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "[户型图] ✅ 下载成功: ${floorPlanFile.length()} bytes")
                return 1
            } else {
                Log.w(TAG, "[户型图] ❌ 下载失败: ${response.code}")
                return 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "[户型图] ❌ 异常: ${e.message}", e)
            return 0
        }
    }

    /**
     * 上传户型图到云端
     */
    private suspend fun pushFloorPlan(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        Log.d(TAG, "[户型图] 开始推送")
        val floorPlanFile = File(context.getExternalFilesDir(null), "floor_plan.jpg")
        if (!floorPlanFile.exists()) {
            Log.d(TAG, "[户型图] 本地无户型图，跳过")
            onLog?.invoke("  📊 本地无户型图")
            return Pair(0, 0)
        }

        Log.d(TAG, "[户型图] 本地户型图: ${floorPlanFile.length()} bytes")
        onLog?.invoke("  📊 本地户型图: ${floorPlanFile.length() / 1024}KB")

        val (baseUrl, apiKey, _) = getConfig(context)
        val token = getStoredAccessToken(context)
        val client = sharedClient

        try {
            val storagePath = "$userId/floor_plan/floor_plan.jpg"
            Log.d(TAG, "[户型图] 上传中: $storagePath")

            // 先尝试删除旧文件（避免 RLS upsert 问题）
            try {
                val deleteRequest = Request.Builder()
                    .url("$baseUrl/storage/v1/object/$STORAGE_BUCKET/$storagePath")
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer $token")
                    .delete()
                    .build()
                client.newCall(deleteRequest).execute()
                Log.d(TAG, "[户型图] 已清理旧文件")
            } catch (_: Exception) {}

            val requestBody = floorPlanFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$baseUrl/storage/v1/object/$STORAGE_BUCKET/$storagePath")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $token")
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "[户型图] ✅ 上传成功")
                onLog?.invoke("  ✅ 户型图上传成功")
                return Pair(1, 0)
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "[户型图] ❌ 上传失败: ${response.code} ${response.message} - $errorBody")
                onLog?.invoke("  ❌ 户型图上传失败: ${response.code} $errorBody")
                return Pair(0, 1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[户型图] ❌ 异常: ${e.message}", e)
            onLog?.invoke("  ❌ 户型图异常: ${e.message}")
            return Pair(0, 1)
        }
    }

    // ============ 进度照片同步 ============

    /**
     * 从云端下载进度照片
     */
    private suspend fun pullStandalonePhotos(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase): Int {
        Log.d(TAG, "[进度照片] 开始拉取")
        val resp = executeWithTokenRefresh(context) { token ->
            api.listRecordsByUser(key, token, "standalone_photos", userIdFilter = "eq.$userId")
        }
        if (!resp.isSuccessful) return 0
        val records = resp.body() ?: emptyList()
        val dao = db.standalonePhotoDao()
        val localMap = dao.getAllList().associateBy { it.id }

        for (record in records) {
            val id = safeLong(record["id"])
            val rawFileName = safeString(record["file_name"])
            // 下载图片到本地
            val localFileName = if (hasSupabaseUrls(rawFileName)) {
                downloadImages(context, rawFileName, userId)
            } else {
                rawFileName
            }
            // 当云端为空时保留本地文件名
            val existingFileName = localMap[id]?.fileName ?: ""
            val finalFileName = if (localFileName.isEmpty() && existingFileName.isNotEmpty()) existingFileName else localFileName
            val entity = StandalonePhoto(
                id = id,
                fileName = finalFileName,
                caption = safeString(record["caption"]),
                takenAt = safeLong(record["taken_at"]),
                createdAt = safeLong(record["created_at"])
            )
            if (localMap.containsKey(id)) {
                dao.update(entity)
            } else {
                dao.insert(entity)
            }
        }
        return records.size
    }

    /**
     * 上传进度照片到云端
     */
    private suspend fun pushStandalonePhotos(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.standalonePhotoDao().getAllList()
        onLog?.invoke("  📊 本地进度照片: ${items.size} 张")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { photo ->
            Log.d(TAG, "[进度照片同步] ID=${photo.id}, fileName='${photo.fileName}'")
            if (photo.fileName.isNotEmpty()) {
                onLog?.invoke("  📷 进度照片#${photo.id} 上传中...")
            }
            val syncedFileName = if (photo.fileName.isNotEmpty())
                uploadImages(context, photo.fileName, userId, "standalone_photos", photo.id) else ""
            mapOf<String, Any?>(
                "id" to photo.id,
                "file_name" to syncedFileName,
                "caption" to photo.caption,
                "taken_at" to photo.takenAt,
                "created_at" to photo.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "standalone_photos", records, onLog)
    }

    // ============ Push methods: local → cloud (upsert) ============

    private suspend fun pushExpenses(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.expenseDao().getAllList()
        onLog?.invoke("  📊 本地支出数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { expense ->
            Log.d(TAG, "[支出同步] ID=${expense.id}, imagePaths='${expense.imagePaths}'")
            if (expense.imagePaths.isNotEmpty()) {
                onLog?.invoke("  📷 支出#${expense.id} 有 ${expense.imagePaths.split(",").size} 张图片，上传中...")
            }
            val syncedImages = if (expense.imagePaths.isNotEmpty())
                uploadImages(context, expense.imagePaths, userId, "expenses", expense.id) else ""
            mapOf<String, Any?>(
                "id" to expense.id,
                "item_name" to expense.itemName,
                "brand" to expense.brand,
                "amount" to expense.amount,
                "buyer" to expense.buyer,
                "phase" to expense.phase,
                "category" to expense.category,
                "room_id" to expense.roomId,
                "supplier_id" to expense.supplierId,
                "date" to expense.date,
                "note" to expense.note,
                "image_paths" to syncedImages,
                "created_at" to expense.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "expenses", records, onLog)
    }

    private suspend fun pushGiftMoney(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.giftMoneyDao().getAllList()
        onLog?.invoke("  📊 本地礼金数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { gift ->
            mapOf<String, Any?>(
                "id" to gift.id,
                "giver_name" to gift.giverName,
                "relationship" to gift.relationship,
                "amount" to gift.amount,
                "occasion" to gift.occasion,
                "date" to gift.date,
                "note" to gift.note,
                "created_at" to gift.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "gift_money", records, onLog)
    }

    private suspend fun pushFundingSources(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.fundingSourceDao().getAllList()
        onLog?.invoke("  📊 本地出资数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { item ->
            mapOf<String, Any?>(
                "id" to item.id,
                "contributor_name" to item.contributorName,
                "relationship" to item.relationship,
                "amount" to item.amount,
                "date" to item.date,
                "note" to item.note,
                "created_at" to item.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "funding_sources", records, onLog)
    }

    private suspend fun pushMaterials(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.materialDao().getAllList()
        onLog?.invoke("  📊 本地材料数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { material ->
            mapOf<String, Any?>(
                "id" to material.id,
                "name" to material.name,
                "spec" to material.spec,
                "quantity" to material.quantity,
                "budget_amount" to material.budgetAmount,
                "actual_amount" to material.actualAmount,
                "buyer" to material.buyer,
                "phase" to material.phase,
                "status" to material.status,
                "note" to material.note,
                "created_at" to material.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "materials", records, onLog)
    }

    private suspend fun pushWorkers(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.workerDao().getAllList()
        onLog?.invoke("  📊 本地工人数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { worker ->
            mapOf<String, Any?>(
                "id" to worker.id,
                "name" to worker.name,
                "phone" to worker.phone,
                "role" to worker.role,
                "daily_wage" to worker.dailyWage,
                "note" to worker.note,
                "created_at" to worker.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "workers", records, onLog)
    }

    private suspend fun pushMilestones(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.milestoneDao().getAllList()
        onLog?.invoke("  📊 本地里程碑数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { milestone ->
            Log.d(TAG, "[里程碑同步] ID=${milestone.id}, imagePaths='${milestone.imagePaths}'")
            if (milestone.imagePaths.isNotEmpty()) {
                onLog?.invoke("  📷 里程碑#${milestone.id} 有 ${milestone.imagePaths.split(",").size} 张图片，上传中...")
            }
            val syncedImages = if (milestone.imagePaths.isNotEmpty())
                uploadImages(context, milestone.imagePaths, userId, "milestones", milestone.id) else ""
            mapOf<String, Any?>(
                "id" to milestone.id,
                "title" to milestone.title,
                "date" to milestone.date,
                "description" to milestone.description,
                "image_paths" to syncedImages,
                "created_at" to milestone.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "milestones", records, onLog)
    }

    private suspend fun pushSuppliers(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.supplierDao().getAllList()
        onLog?.invoke("  📊 本地供应商数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { supplier ->
            if (supplier.qrCodePath.isNotEmpty()) {
                onLog?.invoke("  📷 供应商#${supplier.id} 有收款码，上传中...")
            }
            val syncedQr = if (supplier.qrCodePath.isNotEmpty())
                uploadImages(context, supplier.qrCodePath, userId, "suppliers", supplier.id) else ""
            mapOf<String, Any?>(
                "id" to supplier.id,
                "name" to supplier.name,
                "contact" to supplier.contact,
                "phone" to supplier.phone,
                "address" to supplier.address,
                "category" to supplier.category,
                "qr_code_path" to syncedQr,
                "note" to supplier.note,
                "created_at" to supplier.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "suppliers", records, onLog)
    }

    private suspend fun pushDailyLogs(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.dailyLogDao().getAllList()
        onLog?.invoke("  📊 本地日志数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { log ->
            Log.d(TAG, "[日志同步] ID=${log.id}, imagePaths='${log.imagePaths}'")
            if (log.imagePaths.isNotEmpty()) {
                onLog?.invoke("  📷 日志#${log.id} 有 ${log.imagePaths.split(",").size} 张图片，上传中...")
            }
            val syncedImages = if (log.imagePaths.isNotEmpty())
                uploadImages(context, log.imagePaths, userId, "daily_logs", log.id) else ""
            mapOf<String, Any?>(
                "id" to log.id,
                "date" to log.date,
                "content" to log.content,
                "weather" to log.weather,
                "workers" to log.workers,
                "progress" to log.progress,
                "phase" to log.phase,
                "image_paths" to syncedImages,
                "note" to log.note,
                "created_at" to log.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "daily_logs", records, onLog)
    }

    private suspend fun pushChecklistItems(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.checklistDao().getAllList()
        onLog?.invoke("  📊 本地验收清单数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { item ->
            mapOf<String, Any?>(
                "id" to item.id,
                "phase" to item.phase,
                "title" to item.title,
                "description" to item.description,
                "is_checked" to item.isChecked,
                "checked_date" to item.checkedDate,
                "note" to item.note,
                "sort_order" to item.sortOrder,
                "created_at" to item.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "checklist_items", records, onLog)
    }

    private suspend fun pushHouseRooms(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.roomDao().getAllList()
        onLog?.invoke("  📊 本地房间数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { room ->
            mapOf<String, Any?>(
                "id" to room.id,
                "name" to room.name,
                "area" to room.area,
                "status" to room.status,
                "note" to room.note,
                "created_at" to room.createdAt,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "house_rooms", records, onLog)
    }

    private suspend fun pushProjectConfig(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.projectConfigDao().getAll()
        onLog?.invoke("  📊 本地配置数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { config ->
            mapOf<String, Any?>(
                "key" to config.key,
                "value" to config.value,
                "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "project_config", records, onLog)
    }

    private suspend fun pushAppliances(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.applianceDao().getAllList()
        onLog?.invoke("  📊 本地电器数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { a ->
            Log.d(TAG, "[电器同步] ID=${a.id}, imagePaths='${a.imagePaths}'")
            if (a.imagePaths.isNotEmpty()) {
                onLog?.invoke("  📷 电器#${a.id} 有 ${a.imagePaths.split(",").size} 张图片，上传中...")
            }
            val syncedImages = if (a.imagePaths.isNotEmpty())
                uploadImages(context, a.imagePaths, userId, "appliances", a.id) else ""
            mapOf<String, Any?>(
                "id" to a.id, "name" to a.name, "brand" to a.brand, "model" to a.model,
                "category" to a.category, "purchase_channel" to a.purchaseChannel,
                "purchase_date" to a.purchaseDate, "price" to a.price,
                "warranty_years" to a.warrantyYears, "warranty_expire_date" to a.warrantyExpireDate,
                "install_date" to a.installDate, "after_sale_phone" to a.afterSalePhone,
                "serial_number" to a.serialNumber, "note" to a.note,
                "image_paths" to syncedImages, "created_at" to a.createdAt, "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "appliances", records, onLog)
    }

    private suspend fun pushDocuments(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.documentDao().getAllList()
        onLog?.invoke("  📊 本地证件数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { d ->
            Log.d(TAG, "[证件同步] ID=${d.id}, imagePaths='${d.imagePaths}'")
            if (d.imagePaths.isNotEmpty()) {
                onLog?.invoke("  📷 证件#${d.id} 有 ${d.imagePaths.split(",").size} 张图片，上传中...")
            }
            val syncedImages = if (d.imagePaths.isNotEmpty())
                uploadImages(context, d.imagePaths, userId, "documents", d.id) else ""
            mapOf<String, Any?>(
                "id" to d.id, "name" to d.name, "category" to d.category,
                "storage_location" to d.storageLocation, "related_party" to d.relatedParty,
                "issue_date" to d.issueDate, "expire_date" to d.expireDate,
                "note" to d.note, "image_paths" to syncedImages,
                "created_at" to d.createdAt, "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "documents", records, onLog)
    }

    private suspend fun pushMaintenance(api: SupabaseApi, key: String, context: Context, userId: String, db: AppDatabase, onLog: ((String) -> Unit)? = null): Pair<Int, Int> {
        val items = db.maintenanceDao().getAllList()
        onLog?.invoke("  📊 本地维修数据: ${items.size} 条")
        if (items.isEmpty()) return Pair(0, 0)
        val records = items.map { m ->
            Log.d(TAG, "[维修同步] ID=${m.id}, imagePaths='${m.imagePaths}'")
            if (m.imagePaths.isNotEmpty()) {
                onLog?.invoke("  📷 维修#${m.id} 有 ${m.imagePaths.split(",").size} 张图片，上传中...")
            }
            val syncedImages = if (m.imagePaths.isNotEmpty())
                uploadImages(context, m.imagePaths, userId, "maintenance", m.id) else ""
            mapOf<String, Any?>(
                "id" to m.id, "title" to m.title, "category" to m.category,
                "location" to m.location, "description" to m.description,
                "repair_date" to m.repairDate, "cost" to m.cost,
                "worker_name" to m.workerName, "worker_phone" to m.workerPhone,
                "status" to m.status, "cause" to m.cause, "note" to m.note,
                "image_paths" to syncedImages, "created_at" to m.createdAt, "user_id" to userId
            )
        }
        return batchUpsert(api, key, context, "maintenance", records, onLog)
    }

    // ============ Batch upsert helper ============

    private suspend fun batchUpsert(
        api: SupabaseApi,
        key: String,
        context: Context,
        tableName: String,
        records: List<Map<String, Any?>>,
        onLog: ((String) -> Unit)? = null
    ): Pair<Int, Int> {
        val batchSize = 500
        var totalPushed = 0
        var totalErrors = 0

        for (batch in records.chunked(batchSize)) {
            try {
                val resp = executeWithTokenRefresh(context) { token ->
                    val json = Gson().toJson(batch)
                    val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                    api.upsertRecords(key, token, table = tableName, records = body)
                }
                val respCode = resp.code()
                onLog?.invoke("  📡 HTTP $respCode batch=${batch.size}")
                if (resp.isSuccessful) {
                    totalPushed += batch.size
                    onLog?.invoke("  ✅ 推送成功 ${batch.size} 条")
                    // 详细日志：记录推送的 image_paths 字段
                    for (record in batch) {
                        val img = record["image_paths"]
                        if (img != null) {
                            Log.d(TAG, "  [upsert] id=${record["id"]} image_paths=$img")
                        }
                    }
                } else {
                    val errBody = resp.errorBody()?.string()?.take(300)
                    val errMsg = "Upsert $tableName 失败: HTTP $respCode $errBody"
                    Log.e(TAG, errMsg)
                    onLog?.invoke("  ❌ $errMsg")
                    totalErrors += batch.size
                }
            } catch (e: Exception) {
                val errMsg = "Upsert $tableName 异常: ${e.javaClass.simpleName}: ${e.message?.take(200)}"
                Log.e(TAG, errMsg, e)
                onLog?.invoke("  ❌ $errMsg")
                totalErrors += batch.size
            }
        }
        return Pair(totalPushed, totalErrors)
    }

    // ============ Image Sync (Supabase Storage) ============

    private const val STORAGE_BUCKET = "construction-images"
    private const val SUPABASE_URL_PLACEHOLDER = ""

    /**
     * 上传本地图片到 Supabase Storage，返回逗号分隔的 URL 字符串
     * 已经是 URL 的图片跳过上传
     */
    private suspend fun uploadImages(
        context: Context, imagePaths: String, userId: String, table: String, recordId: Long
    ): String = withContext(Dispatchers.IO) {
        if (imagePaths.isBlank()) {
            Log.d(TAG, "[图片上传] imagePaths 为空，跳过")
            return@withContext ""
        }
        val files = imagePaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (files.isEmpty()) {
            Log.d(TAG, "[图片上传] 解析后无文件，跳过")
            return@withContext ""
        }

        Log.d(TAG, "[图片上传] 开始上传 ${files.size} 张图片 → $table/$recordId")
        val (baseUrl, apiKey, _) = getConfig(context)
        val token = getStoredAccessToken(context)
        val client = sharedClient

        val urls = files.mapNotNull { fileName ->
            // 已经是 URL 的跳过
            if (fileName.startsWith("http")) {
                Log.d(TAG, "[图片上传] 跳过（已是URL）: $fileName")
                return@mapNotNull fileName
            }

            val imageFile = ImageUtils.getImageFile(context, fileName)
            if (!imageFile.exists()) {
                // 如果是 storage 路径（从云端拉取的），保留原路径不丢失
                if (fileName.contains("/")) {
                    Log.d(TAG, "[图片上传] 保留云端路径: $fileName")
                    return@mapNotNull fileName
                }
                Log.w(TAG, "[图片上传] 文件不存在: $fileName → ${imageFile.absolutePath}")
                return@mapNotNull null
            }

            try {
                val storagePath = "$userId/$table/${recordId}_$fileName"
                Log.d(TAG, "[图片上传] 上传中: $fileName → $storagePath (${imageFile.length()} bytes)")
                val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("$baseUrl/storage/v1/object/$STORAGE_BUCKET/$storagePath")
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer $token")
                    .put(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "[图片上传] ✅ 成功: $storagePath")
                    storagePath
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "[图片上传] ❌ 失败: ${response.code} ${response.message} - $errorBody")
                    fileName // 保留原始文件名
                }
            } catch (e: Exception) {
                Log.e(TAG, "[图片上传] ❌ 异常: ${e.message}", e)
                fileName
            }
        }
        Log.d(TAG, "[图片上传] 完成: ${urls.size}/${files.size} 成功")
        urls.joinToString(",")
    }

    /**
     * 下载 Supabase Storage 图片到本地，返回逗号分隔的本地文件名
     * 支持 Storage 路径（临时签名下载）和 HTTP URL（向后兼容）
     */
    private suspend fun downloadImages(
        context: Context, imagePaths: String, userId: String
    ): String = withContext(Dispatchers.IO) {
        if (imagePaths.isBlank()) {
            Log.d(TAG, "[图片下载] imagePaths 为空，跳过")
            return@withContext ""
        }
        val items = imagePaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty()) {
            Log.d(TAG, "[图片下载] 解析后无项目，跳过")
            return@withContext ""
        }

        Log.d(TAG, "[图片下载] 开始下载 ${items.size} 张图片")
        val (baseUrl, apiKey, _) = getConfig(context)
        val token = getStoredAccessToken(context)
        val client = sharedClient

        val localNames = items.mapNotNull { item ->
            // 本地文件名（无 / 且非 http）跳过
            if (!item.contains("/") && !item.startsWith("http")) {
                Log.d(TAG, "[图片下载] 跳过（本地文件）: $item")
                return@mapNotNull item
            }

            try {
                // 提取文件名: .../userId/table/recordId_filename.jpg
                val urlFileName = item.substringAfterLast("/")
                    .substringAfter("_")  // 去掉 recordId_ 前缀
                if (urlFileName.isBlank()) {
                    Log.w(TAG, "[图片下载] 无法提取文件名: $item")
                    return@mapNotNull null
                }

                // 检查本地是否已存在
                val localFile = ImageUtils.getImageFile(context, urlFileName)
                if (localFile.exists()) {
                    Log.d(TAG, "[图片下载] 跳过（已存在）: $urlFileName")
                    return@mapNotNull urlFileName
                }

                // 确定下载 URL
                val downloadUrl = if (item.startsWith("http")) {
                    Log.d(TAG, "[图片下载] 使用 HTTP URL: $item")
                    item // HTTP URL 直接用
                } else {
                    // Storage 路径：先尝试临时签名
                    Log.d(TAG, "[图片下载] 生成签名: $item")
                    val signBody = """{"expiresIn":3600}""".toRequestBody("application/json".toMediaTypeOrNull())
                    val signRequest = Request.Builder()
                        .url("$baseUrl/storage/v1/object/sign/$STORAGE_BUCKET/$item")
                        .header("apikey", apiKey)
                        .header("Authorization", "Bearer $token")
                        .post(signBody)
                        .build()
                    val signResp = client.newCall(signRequest).execute()
                    if (signResp.isSuccessful) {
                        val signedPath = JSONObject(signResp.body?.string() ?: "").optString("signedURL", "")
                        if (signedPath.isNotEmpty()) {
                            Log.d(TAG, "[图片下载] 签名成功: $signedPath")
                            "$baseUrl/storage/v1$signedPath"
                        } else {
                            Log.w(TAG, "[图片下载] 签名响应为空，尝试直接下载")
                            "$baseUrl/storage/v1/object/$STORAGE_BUCKET/$item"
                        }
                    } else {
                        val errorBody = signResp.body?.string() ?: ""
                        Log.w(TAG, "[图片下载] 签名失败: ${signResp.code}，尝试直接下载")
                        // 签名失败，尝试直接下载（bucket 可能是 public 的）
                        "$baseUrl/storage/v1/object/$STORAGE_BUCKET/$item"
                    }
                }

                Log.d(TAG, "[图片下载] 下载中: $urlFileName")
                val requestBuilder = Request.Builder().url(downloadUrl)
                // 签名 URL 自带 token，直接下载需要 header 认证
                if (!downloadUrl.contains("token=")) {
                    requestBuilder.header("apikey", apiKey)
                        .header("Authorization", "Bearer $token")
                }
                val response = client.newCall(requestBuilder.build()).execute()
                if (response.isSuccessful) {
                    // 确保 images 目录存在
                    localFile.parentFile?.mkdirs()
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "[图片下载] ✅ 成功: $urlFileName (${localFile.length()} bytes)")
                    urlFileName
                } else {
                    Log.e(TAG, "[图片下载] ❌ 下载失败: ${response.code} ${response.message}")
                    item // 保留原始 storage 路径，下次同步可重试
                }
            } catch (e: Exception) {
                Log.e(TAG, "[图片下载] ❌ 异常: ${e.message}", e)
                item // 保留原始 storage 路径，下次同步可重试
            }
        }
        Log.d(TAG, "[图片下载] 完成: ${localNames.size}/${items.size}")
        localNames.joinToString(",")
    }

    /**
     * 判断 imagePaths 是否包含 Supabase URL
     */
    private fun hasSupabaseUrls(imagePaths: String): Boolean {
        // Storage 路径含 /（如 uuid/expenses/456_img.jpg）或旧版 HTTP URL
        return imagePaths.contains("/") || imagePaths.contains("supabase.co")
    }

    /**
     * 扫描 Supabase Storage 中某个表的图片，返回 recordId → storage paths 的映射
     * 用于修复：图片已上传到 Storage 但数据库 image_paths 为空的情况
     */
    private suspend fun scanStorageForTable(
        context: Context, userId: String, table: String
    ): Map<Long, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Long, String>()
        try {
            val (baseUrl, apiKey, _) = getConfig(context)
            val token = getStoredAccessToken(context)
            val client = sharedClient

            val prefix = "$userId/$table/"
            val body = """{"prefix":"$prefix","limit":500}""".toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("$baseUrl/storage/v1/object/list/$STORAGE_BUCKET")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val items = Gson().fromJson(response.body?.string() ?: "[]", Array<StorageListItem>::class.java)
                for (item in items) {
                    val name = item.name ?: continue
                    // 文件名格式: {recordId}_{filename}
                    val underscoreIdx = name.indexOf('_')
                    if (underscoreIdx <= 0) continue
                    val recordIdStr = name.substring(0, underscoreIdx)
                    val recordId = recordIdStr.toLongOrNull() ?: continue
                    val storagePath = "$prefix$name"
                    val existing = result[recordId]
                    result[recordId] = if (existing.isNullOrEmpty()) storagePath else "$existing,$storagePath"
                }
                if (result.isNotEmpty()) {
                    Log.d(TAG, "[Storage扫描] $table: 发现 ${result.size} 条记录的图片")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Storage扫描] $table 异常: ${e.message}")
        }
        result
    }

    // Storage list API 返回的数据结构
    data class StorageListItem(
        val name: String? = null,
        val id: String? = null,
        val updated_at: String? = null,
        val created_at: String? = null,
        val metadata: Any? = null
    )

    // ============ Utility methods ============

    /**
     * 获取本地数据库文件大小（字节）
     */
    fun getLocalDbSize(context: Context): Long {
        val dbFile = context.getDatabasePath("construction_diary.db")
        return if (dbFile.exists()) dbFile.length() else 0
    }

    /**
     * 获取本地图片目录大小（字节）
     */
    fun getLocalImageSize(context: Context): Long {
        val imagesDir = java.io.File(context.getExternalFilesDir(null), "images")
        if (!imagesDir.exists()) return 0
        return imagesDir.listFiles()?.sumOf { it.length() } ?: 0
    }

    /**
     * 查询云端 Storage 使用量（字节）
     * 返回 Triple(已用空间, 错误信息, 是否成功)
     */
    suspend fun getCloudStorageUsage(context: Context): Triple<Long, String?, Boolean> = withContext(Dispatchers.IO) {
        try {
            val (baseUrl, apiKey, _) = getConfig(context)
            val token = getStoredAccessToken(context)
            if (token.isEmpty()) return@withContext Triple(0, "未登录", false)

            val client = sharedClient

            // 查询 storage.objects 表获取总大小
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/rpc/get_storage_size")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    "{}"
                ))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "0"
                val size = body.toLongOrNull() ?: 0
                Triple(size, null, true)
            } else {
                // 如果 RPC 不存在，尝试直接查询
                Triple(0, null, true)
            }
        } catch (e: Exception) {
            Triple(0, e.message, false)
        }
    }

    /**
     * 格式化文件大小
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1fMB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2fGB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun isConfigured(context: Context): Boolean {
        val (url, key, _) = getConfig(context)
        return url.isNotEmpty() && key.isNotEmpty()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getStoredAccessToken(context).isNotEmpty()
    }

    fun getEmail(context: Context): String {
        return getPrefs(context).getString(KEY_EMAIL, "") ?: ""
    }

    fun signOut(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .apply()
        cachedAccessToken = null
        cachedRefreshToken = null
    }
}
