package com.construction.diary.cloud.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.ProjectConfig
import com.construction.diary.cloud.ui.cloud.CloudLoginActivity
import com.construction.diary.cloud.ui.about.AboutActivity
import com.construction.diary.cloud.ui.export.ExportImportActivity
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<TextView>(R.id.tvTitle).text = "⚙️ 设置"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val etAddress = findViewById<EditText>(R.id.etAddress)
        val etDate = findViewById<EditText>(R.id.etDate)
        val etBudget = findViewById<EditText>(R.id.etBudget)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@SettingsActivity)
            etAddress.setText(db.projectConfigDao().getValue("address") ?: "")
            etDate.setText(db.projectConfigDao().getValue("start_date") ?: "")
            etBudget.setText(db.projectConfigDao().getValue("budget") ?: "")
        }

        etDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("选择开工日期").build()
            picker.show(supportFragmentManager, "date")
            picker.addOnPositiveButtonClickListener { millis ->
                etDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis)))
            }
        }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@SettingsActivity)
                db.projectConfigDao().set(ProjectConfig("address", etAddress.text.toString().trim()))
                db.projectConfigDao().set(ProjectConfig("start_date", etDate.text.toString().trim()))
                db.projectConfigDao().set(ProjectConfig("budget", etBudget.text.toString().trim()))
                Toast.makeText(this@SettingsActivity, "保存成功", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Cloud sync button - opens Supabase login
        findViewById<View>(R.id.btnCloudSync)?.setOnClickListener {
            startActivity(Intent(this, CloudLoginActivity::class.java))
        }

        // Export/Import button
        findViewById<View>(R.id.btnExportImport)?.setOnClickListener {
            startActivity(Intent(this, ExportImportActivity::class.java))
        }

        // About button
        findViewById<View>(R.id.btnAbout)?.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
