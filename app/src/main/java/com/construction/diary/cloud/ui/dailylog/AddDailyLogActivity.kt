package com.construction.diary.cloud.ui.dailylog

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.DailyLog
import com.construction.diary.cloud.util.DateUtils
import com.construction.diary.cloud.util.PhaseUtils
import androidx.appcompat.app.AlertDialog
import com.construction.diary.cloud.util.DialogUtils
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.util.*

class AddDailyLogActivity : AppCompatActivity() {
    private var logId: Long = 0
    private var selectedDate = System.currentTimeMillis()
    private var editingLog: DailyLog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_dailylog)
        logId = intent.getLongExtra("log_id", 0)
        findViewById<TextView>(R.id.tvTitle).text = if (logId > 0) "编辑日志" else "写施工日志"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val etContent = findViewById<EditText>(R.id.etContent)
        val etWeather = findViewById<EditText>(R.id.etWeather)
        val etWorkers = findViewById<EditText>(R.id.etWorkers)
        val spinnerPhase = findViewById<Spinner>(R.id.spinnerPhase)
        val seekProgress = findViewById<SeekBar>(R.id.seekProgress)
        val tvProgressValue = findViewById<TextView>(R.id.tvProgressValue)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        spinnerPhase.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("（选择阶段）") + PhaseUtils.PHASES)

        seekProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvProgressValue.text = "${progress}%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        tvDate.text = DateUtils.formatDate(selectedDate)
        tvDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("选择日期")
                .setSelection(selectedDate).build()
            picker.show(supportFragmentManager, "date")
            picker.addOnPositiveButtonClickListener { millis ->
                selectedDate = millis
                tvDate.text = DateUtils.formatDate(millis)
            }
        }

        if (logId > 0) {
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                editingLog = AppDatabase.getInstance(this@AddDailyLogActivity).dailyLogDao().getAllList().find { it.id == logId }
                val log = editingLog ?: return@launch
                etContent.setText(log.content)
                etWeather.setText(log.weather)
                etWorkers.setText(log.workers)
                seekProgress.progress = log.progress
                tvProgressValue.text = "${log.progress}%"
                selectedDate = log.date
                tvDate.text = DateUtils.formatDate(log.date)
                val phaseIdx = PhaseUtils.PHASES.indexOf(log.phase)
                if (phaseIdx >= 0) spinnerPhase.setSelection(phaseIdx + 1)
            }
        }

        btnDelete.setOnClickListener {
            editingLog?.let { log ->
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("确定删除这条施工日志？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            AppDatabase.getInstance(this@AddDailyLogActivity).dailyLogDao().delete(log)
                            finish()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .let { DialogUtils.showRoundedCompat(it) }
            }
        }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            val content = etContent.text.toString().trim()
            if (content.isEmpty()) { etContent.error = "请输入施工内容"; return@setOnClickListener }
            val phase = if (spinnerPhase.selectedItemPosition > 0) PhaseUtils.PHASES[spinnerPhase.selectedItemPosition - 1] else ""
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@AddDailyLogActivity)
                    val log = DailyLog(
                        id = logId, date = selectedDate, content = content,
                        weather = etWeather.text.toString().trim(),
                        workers = etWorkers.text.toString().trim(),
                        progress = seekProgress.progress, phase = phase
                    )
                    if (logId > 0) db.dailyLogDao().update(log) else db.dailyLogDao().insert(log)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@AddDailyLogActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
