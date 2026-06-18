package com.construction.diary.cloud.ui.export

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.util.DialogUtils
import com.construction.diary.cloud.util.ExportHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportImportActivity : AppCompatActivity() {

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { importFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_import)
        findViewById<TextView>(R.id.tvTitle).text = "📤 数据管理"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnExport).setOnClickListener { exportData() }
        findViewById<View>(R.id.btnImport).setOnClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this)
                    .setTitle("导入数据")
                    .setMessage("导入将覆盖当前所有数据，确定继续？")
                    .setPositiveButton("选择文件") { _, _ -> pickFile.launch("application/zip") }
                    .setNegativeButton("取消", null)
            )
        }
    }

    private fun exportData() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(getExternalFilesDir(null), "export")
        exportDir.mkdirs()
        val outputFile = File(exportDir, "房迹_$timestamp.zip")

        lifecycleScope.launch {
            val success = ExportHelper.exportAll(this@ExportImportActivity, outputFile)
            if (success) {
                Toast.makeText(this@ExportImportActivity, "导出成功\n${outputFile.absolutePath}", Toast.LENGTH_LONG).show()
                // Share the file
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@ExportImportActivity, "$packageName.fileprovider", outputFile
                )
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "分享备份文件"))
            } else {
                Toast.makeText(this@ExportImportActivity, "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        val cacheFile = File(cacheDir, "import.zip")
        lifecycleScope.launch {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(cacheFile).use { out -> input.copyTo(out) }
                }
                val success = ExportHelper.importAll(this@ExportImportActivity, cacheFile)
                if (success) {
                    Toast.makeText(this@ExportImportActivity, "导入成功！", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ExportImportActivity, "导入失败，文件格式错误", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExportImportActivity, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                cacheFile.delete()
            }
        }
    }
}
