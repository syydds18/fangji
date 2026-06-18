package com.construction.diary.cloud.util

import android.content.Context
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ExportData(
    val fundingSources: List<FundingSource> = emptyList(),
    val version: Int = 2,
    val exportTime: Long = System.currentTimeMillis(),
    val expenses: List<Expense> = emptyList(),
    val gifts: List<GiftMoney> = emptyList(),
    val materials: List<Material> = emptyList(),
    val workers: List<Worker> = emptyList(),
    val milestones: List<Milestone> = emptyList(),
    val config: List<ProjectConfig> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val dailyLogs: List<DailyLog> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList(),
    val rooms: List<com.construction.diary.cloud.data.entity.HouseRoom> = emptyList(),
    val images: List<String> = emptyList()
)

object ExportHelper {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportAll(context: Context, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val data = ExportData(
                expenses = db.expenseDao().getAllList(),
                gifts = db.giftMoneyDao().getAllList(),
                materials = db.materialDao().getAllList(),
                workers = db.workerDao().getAllList(),
                milestones = db.milestoneDao().getAllList(),
                config = db.projectConfigDao().getAll(),
                suppliers = db.supplierDao().getAllList(),
                dailyLogs = db.dailyLogDao().getAllList(),
                checklistItems = db.checklistDao().getAllList(),
                rooms = db.roomDao().getAllList(),
                fundingSources = db.fundingSourceDao().getAllList()
            )

            val imagesDir = File(context.getExternalFilesDir(null), "images")
            val imageFiles = mutableListOf<String>()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zip ->
                val json = gson.toJson(data)
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(json.toByteArray())
                zip.closeEntry()

                if (imagesDir.exists()) {
                    imagesDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            imageFiles.add(file.name)
                            zip.putNextEntry(ZipEntry("images/${file.name}"))
                            FileInputStream(file).use { fis -> fis.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importAll(context: Context, inputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val imagesDir = File(context.getExternalFilesDir(null), "images")
            imagesDir.mkdirs()

            var jsonData = ""

            ZipInputStream(BufferedInputStream(FileInputStream(inputFile))).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "data.json") {
                        jsonData = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (entry.name.startsWith("images/") && !entry.isDirectory) {
                        val fileName = entry.name.removePrefix("images/")
                        val outFile = File(imagesDir, fileName)
                        FileOutputStream(outFile).use { fos -> zip.copyTo(fos) }
                    }
                    entry = zip.nextEntry
                }
            }

            if (jsonData.isNotEmpty()) {
                val data = gson.fromJson(jsonData, ExportData::class.java)

                // Clear all
                db.expenseDao().deleteAll()
                db.giftMoneyDao().deleteAll()
                db.materialDao().deleteAll()
                db.workerDao().deleteAll()
                db.milestoneDao().deleteAll()
                db.projectConfigDao().deleteAll()
                db.supplierDao().deleteAll()
                db.dailyLogDao().deleteAll()
                db.checklistDao().deleteAll()
                db.roomDao().deleteAll()
                db.fundingSourceDao().deleteAll()

                // Insert
                data.expenses.forEach { db.expenseDao().insert(it) }
                data.gifts.forEach { db.giftMoneyDao().insert(it) }
                data.materials.forEach { db.materialDao().insert(it) }
                data.workers.forEach { db.workerDao().insert(it) }
                data.milestones.forEach { db.milestoneDao().insert(it) }
                data.config.forEach { db.projectConfigDao().set(it) }
                data.suppliers.forEach { db.supplierDao().insert(it) }
                data.dailyLogs.forEach { db.dailyLogDao().insert(it) }
                data.checklistItems.forEach { db.checklistDao().insert(it) }
                data.rooms.forEach { db.roomDao().insert(it) }
                data.fundingSources.forEach { db.fundingSourceDao().insert(it) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
