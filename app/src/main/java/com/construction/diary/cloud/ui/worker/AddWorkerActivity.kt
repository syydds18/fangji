package com.construction.diary.cloud.ui.worker

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Worker
import androidx.appcompat.app.AlertDialog
import com.construction.diary.cloud.util.DialogUtils
import kotlinx.coroutines.launch

class AddWorkerActivity : AppCompatActivity() {
    private var workerId: Long = 0
    private var editingWorker: Worker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_worker)
        workerId = intent.getLongExtra("worker_id", 0)
        findViewById<TextView>(R.id.tvTitle).text = if (workerId > 0) "编辑工人" else "添加工人"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etRole = findViewById<EditText>(R.id.etRole)
        val etWage = findViewById<EditText>(R.id.etWage)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        if (workerId > 0) {
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                editingWorker = AppDatabase.getInstance(this@AddWorkerActivity).workerDao().getAllList().find { it.id == workerId }
                val w = editingWorker ?: return@launch
                etName.setText(w.name); etPhone.setText(w.phone); etRole.setText(w.role)
                etWage.setText(w.dailyWage.toString()); etNote.setText(w.note)
            }
        }

        btnDelete.setOnClickListener {
            editingWorker?.let { w ->
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("确定删除工人「${w.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            AppDatabase.getInstance(this@AddWorkerActivity).workerDao().delete(w)
                            finish()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .let { DialogUtils.showRoundedCompat(it) }
            }
        }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) { etName.error = "请输入姓名"; return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@AddWorkerActivity)
                    val w = Worker(id = workerId, name = name, phone = etPhone.text.toString().trim(),
                        role = etRole.text.toString().trim(), dailyWage = etWage.text.toString().toDoubleOrNull() ?: 0.0,
                        note = etNote.text.toString().trim())
                    if (workerId > 0) db.workerDao().update(w) else db.workerDao().insert(w)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@AddWorkerActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
