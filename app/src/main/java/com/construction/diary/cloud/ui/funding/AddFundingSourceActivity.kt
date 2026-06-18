package com.construction.diary.cloud.ui.funding

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.FundingSource
import com.construction.diary.cloud.util.DateUtils
import com.google.android.material.datepicker.MaterialDatePicker
import com.construction.diary.cloud.util.DialogUtils
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch

class AddFundingSourceActivity : AppCompatActivity() {

    private lateinit var etContributorName: EditText
    private lateinit var etRelationship: EditText
    private lateinit var etAmount: EditText
    private lateinit var tvDate: TextView
    private lateinit var etNote: EditText
    private lateinit var btnSave: TextView
    private lateinit var btnDelete: TextView
    private lateinit var tvTitle: TextView

    private var selectedDate: Long = System.currentTimeMillis()
    private var editingItem: FundingSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_funding_source)

        etContributorName = findViewById(R.id.etContributorName)
        etRelationship = findViewById(R.id.etRelationship)
        etAmount = findViewById(R.id.etAmount)
        tvDate = findViewById(R.id.tvDate)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        tvTitle = findViewById(R.id.tvTitle)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        tvDate.text = DateUtils.formatDate(selectedDate)
        tvDate.setOnClickListener { showDatePicker() }

        val fundingId = intent.getLongExtra("funding_id", -1)
        if (fundingId > 0) {
            tvTitle.text = "编辑出资"
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@AddFundingSourceActivity)
                val all = db.fundingSourceDao().getAllList()
                editingItem = all.find { it.id == fundingId }
                editingItem?.let { populateFields(it) }
            }
        }

        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { deleteItem() }
    }

    private fun populateFields(item: FundingSource) {
        etContributorName.setText(item.contributorName)
        etRelationship.setText(item.relationship)
        etAmount.setText(item.amount.toString())
        etNote.setText(item.note)
        selectedDate = item.date
        tvDate.text = DateUtils.formatDate(item.date)
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择日期")
            .setSelection(selectedDate)
            .build()
        picker.addOnPositiveButtonClickListener { timestamp ->
            selectedDate = timestamp
            tvDate.text = DateUtils.formatDate(timestamp)
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun save() {
        val name = etContributorName.text.toString().trim()
        if (name.isEmpty()) {
            etContributorName.error = "请输入姓名"
            return
        }
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

        val item = (editingItem ?: FundingSource()).copy(
            contributorName = name,
            relationship = etRelationship.text.toString().trim(),
            amount = amount,
            date = selectedDate,
            note = etNote.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AddFundingSourceActivity)
                if (editingItem != null) {
                    db.fundingSourceDao().update(item)
                } else {
                    db.fundingSourceDao().insert(item)
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddFundingSourceActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteItem() {
        editingItem?.let { item ->
            AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage("确定删除「${item.contributorName}」的出资记录？")
                .setPositiveButton("删除") { _, _ ->
                    lifecycleScope.launch {
                        AppDatabase.getInstance(this@AddFundingSourceActivity)
                            .fundingSourceDao().delete(item)
                        finish()
                    }
                }
                .setNegativeButton("取消", null)
                .let { DialogUtils.showRoundedCompat(it) }
        }
    }
}
