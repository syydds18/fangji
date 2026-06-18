package com.construction.diary.cloud.ui.cloud

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.cloud.SupabaseSyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CloudLoginActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_login)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = "☁️ 云同步"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }



        val etUrl = findViewById<EditText>(R.id.etSupabaseUrl)
        val etKey = findViewById<EditText>(R.id.etSupabaseKey)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

        // Load existing config (auto-fill default values)
        val prefs = getSharedPreferences("cloud_config", MODE_PRIVATE)
        val defaultUrl = ""
        val defaultKey = ""
        if (prefs.getString("supabase_url", "").isNullOrEmpty()) {
            prefs.edit().putString("supabase_url", defaultUrl).putString("supabase_key", defaultKey).apply()
        }
        etUrl.setText(prefs.getString("supabase_url", defaultUrl))
        etKey.setText(prefs.getString("supabase_key", defaultKey))

        // Config section - user enters their own Supabase URL/Key
        etUrl.isEnabled = true
        etKey.isEnabled = true
        findViewById<View>(R.id.btnSaveConfig)?.visibility = View.VISIBLE
        findViewById<View>(R.id.btnSaveConfig)?.setOnClickListener {
            val url = etUrl.text.toString().trim()
            val key = etKey.text.toString().trim()
            if (url.isEmpty() || key.isEmpty()) {
                Toast.makeText(this, "请输入 Supabase URL 和 Key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("supabase_url", url).putString("supabase_key", key).apply()
            Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
        }

        // Auto-fill saved email
        val savedEmail = SupabaseSyncManager.getEmail(this)
        if (savedEmail.isNotEmpty()) {
            etEmail.setText(savedEmail)
        }

        // Try auto-login with saved refresh token
        if (SupabaseSyncManager.isLoggedIn(this)) {
            updateLoginStatus()
            appendLog("✅ 已有登录状态，自动恢复")
            // Try refresh token silently
            lifecycleScope.launch {
                val refreshed = SupabaseSyncManager.tryRefreshToken(this@CloudLoginActivity)
                if (refreshed) {
                    appendLog("🔄 Token 已刷新")
                }
            }
        } else if (savedEmail.isNotEmpty()) {
            // Has saved email but not logged in (e.g. after reinstall with backup)
            tvStatus.text = "📧 已填充邮箱，请输入密码登录"
            tvStatus.setTextColor(getColor(R.color.mi_text_secondary))
            etPassword.requestFocus()
        } else {
            updateLoginStatus()
        }

        // Copy log
        findViewById<View>(R.id.btnCopyLog).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("sync_log", logBuilder.toString()))
            Toast.makeText(this, "日志已复制", Toast.LENGTH_SHORT).show()
        }

        // Clear log
        findViewById<View>(R.id.btnClearLog).setOnClickListener {
            logBuilder.clear()
            tvLog.text = "等待操作..."
        }

        // Sign up
        findViewById<View>(R.id.btnSignUp).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入邮箱和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                appendLog("=== 注册 ===")
                appendLog("邮箱: $email")
                tvStatus.text = "🔄 注册中..."
                tvStatus.setTextColor(getColor(R.color.mi_text_secondary))
                try {
                    val success = SupabaseSyncManager.signUp(this@CloudLoginActivity, email, password)
                    if (success) {
                        tvStatus.text = "✅ 注册成功，请登录"
                        tvStatus.setTextColor(getColor(R.color.mi_success))
                        appendLog("✅ 注册成功")
                    } else {
                        tvStatus.text = "❌ 注册失败"
                        tvStatus.setTextColor(getColor(R.color.mi_danger))
                        appendLog("❌ 注册失败，请检查邮箱格式或密码长度(至少6位)")
                    }
                } catch (e: Exception) {
                    tvStatus.text = "❌ 注册异常"
                    tvStatus.setTextColor(getColor(R.color.mi_danger))
                    appendLog("❌ 注册异常: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }

        // Sign in
        findViewById<View>(R.id.btnSignIn).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入邮箱和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                appendLog("=== 登录 ===")
                appendLog("邮箱: $email")
                tvStatus.text = "🔄 登录中..."
                tvStatus.setTextColor(getColor(R.color.mi_text_secondary))
                try {
                    val success = SupabaseSyncManager.signIn(this@CloudLoginActivity, email, password)
                    if (success) {
                        updateLoginStatus()
                        appendLog("✅ 登录成功")
                    } else {
                        tvStatus.text = "❌ 登录失败"
                        tvStatus.setTextColor(getColor(R.color.mi_danger))
                        appendLog("❌ 登录失败，请检查邮箱和密码")
                    }
                } catch (e: Exception) {
                    tvStatus.text = "❌ 登录异常"
                    tvStatus.setTextColor(getColor(R.color.mi_danger))
                    appendLog("❌ 登录异常: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }

        // Sync
        findViewById<View>(R.id.btnSync).setOnClickListener {
            if (!SupabaseSyncManager.isLoggedIn(this)) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
                appendLog("⚠️ 请先登录再同步")
                return@setOnClickListener
            }
            lifecycleScope.launch {
                appendLog("=== 开始同步 ${dateFormat.format(Date())} ===")
                findViewById<View>(R.id.btnSync).isEnabled = false
                try {
                    val result = SupabaseSyncManager.syncAll(this@CloudLoginActivity,
                        onProgress = { tableName, current, total ->
                            runOnUiThread {
                                tvStatus.text = "🔄 同步中 ($current/$total) $tableName..."
                                tvStatus.setTextColor(getColor(R.color.mi_text_secondary))
                            }
                        },
                        onLog = { msg ->
                            runOnUiThread { appendLog(msg) }
                        }
                    )

                    findViewById<View>(R.id.btnSync).isEnabled = true

                    if (result.success) {
                        val sb = StringBuilder()
                        sb.appendLine("✅ 同步完成")
                        sb.appendLine(result.summary)
                        for (tr in result.tableResults) {
                            val icon = if (tr.errors > 0) "⚠️" else "✅"
                            sb.appendLine("  $icon ${tr.displayName}: 拉${tr.pulled} 推${tr.pushed}" +
                                    if (tr.errors > 0) " 错${tr.errors}" else "")
                            if (tr.errors > 0) {
                                appendLog("⚠️ ${tr.displayName}: ${tr.errors} 条错误")
                            } else {
                                appendLog("✅ ${tr.displayName}: 拉${tr.pulled} 推${tr.pushed}")
                            }
                        }
                        tvStatus.text = sb.toString()
                        tvStatus.setTextColor(getColor(R.color.mi_success))
                        appendLog("=== 同步完成 ===")
                    } else {
                        tvStatus.text = "❌ 同步失败: ${result.errorMessage ?: "未知错误"}"
                        tvStatus.setTextColor(getColor(R.color.mi_danger))
                        appendLog("❌ 同步失败: ${result.errorMessage}")
                    }
                } catch (e: Exception) {
                    findViewById<View>(R.id.btnSync).isEnabled = true
                    tvStatus.text = "❌ 同步异常"
                    tvStatus.setTextColor(getColor(R.color.mi_danger))
                    appendLog("❌ 同步异常: ${e.javaClass.simpleName}")
                    appendLog("  详情: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        // Sign out
        findViewById<View>(R.id.btnSignOut).setOnClickListener {
            SupabaseSyncManager.signOut(this)
            tvStatus.text = "已退出登录"
            tvStatus.setTextColor(getColor(R.color.mi_text_secondary))
            appendLog("已退出登录")
        }
    }

    private fun appendLog(msg: String) {
        val timestamp = dateFormat.format(Date()).substring(11, 19) // HH:mm:ss only
        logBuilder.appendLine("[$timestamp] $msg")
        runOnUiThread {
            tvLog.text = logBuilder.toString()
            logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun updateLoginStatus() {
        val email = SupabaseSyncManager.getEmail(this)
        val lastSync = SupabaseSyncManager.getLastSyncTime(this)
        val sb = StringBuilder()
        sb.append("✅ 已登录: $email")
        if (lastSync > 0) {
            sb.append("\n🕐 上次同步: ${dateFormat.format(Date(lastSync))}")
        }
        tvStatus.text = sb.toString()
        tvStatus.setTextColor(getColor(R.color.mi_success))
    }
}
