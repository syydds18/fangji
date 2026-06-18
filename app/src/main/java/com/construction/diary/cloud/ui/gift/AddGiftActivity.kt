package com.construction.diary.cloud.ui.gift

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.GiftMoney
import com.construction.diary.cloud.util.DateUtils
import com.google.android.material.datepicker.MaterialDatePicker
import com.construction.diary.cloud.util.DialogUtils
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch

class AddGiftActivity : AppCompatActivity() {

    private lateinit var etGiverName: EditText
    private lateinit var etRelationship: EditText
    private lateinit var etAmount: EditText
    private lateinit var spinnerOccasion: Spinner
    private lateinit var tvDate: TextView
    private lateinit var etNote: EditText
    private lateinit var btnSave: TextView
    private lateinit var btnDelete: TextView
    private lateinit var tvTitle: TextView

    private var selectedDate: Long = System.currentTimeMillis()
    private var editingGift: GiftMoney? = null

    private val occasions = listOf("请选择场合", "上梁", "乔迁", "其他")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_gift)

        etGiverName = findViewById(R.id.etGiverName)
        etRelationship = findViewById(R.id.etRelationship)
        etAmount = findViewById(R.id.etAmount)
        spinnerOccasion = findViewById(R.id.spinnerOccasion)
        tvDate = findViewById(R.id.tvDate)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        tvTitle = findViewById(R.id.tvTitle)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        spinnerOccasion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, occasions)

        tvDate.text = DateUtils.formatDate(selectedDate)
        tvDate.setOnClickListener { showDatePicker() }

        val giftId = intent.getLongExtra("gift_id", -1)
        if (giftId > 0) {
            tvTitle.text = "编辑礼金"
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@AddGiftActivity)
                val all = db.giftMoneyDao().getAllList()
                editingGift = all.find { it.id == giftId }
                editingGift?.let { populateFields(it) }
            }
        }

        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { deleteGift() }
    }

    private fun populateFields(gift: GiftMoney) {
        etGiverName.setText(gift.giverName)
        etRelationship.setText(gift.relationship)
        etAmount.setText(gift.amount.toString())
        etNote.setText(gift.note)
        selectedDate = gift.date
        tvDate.text = DateUtils.formatDate(gift.date)
        val occasionIndex = occasions.indexOf(gift.occasion)
        if (occasionIndex > 0) spinnerOccasion.setSelection(occasionIndex)
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
        val name = etGiverName.text.toString().trim()
        if (name.isEmpty()) {
            etGiverName.error = "请输入姓名"
            return
        }
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val occasionIndex = spinnerOccasion.selectedItemPosition
        val occasion = if (occasionIndex > 0) occasions[occasionIndex] else ""

        val gift = (editingGift ?: GiftMoney()).copy(
            giverName = name,
            relationship = etRelationship.text.toString().trim(),
            amount = amount,
            occasion = occasion,
            date = selectedDate,
            note = etNote.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AddGiftActivity)
                if (editingGift != null) {
                    db.giftMoneyDao().update(gift)
                } else {
                    db.giftMoneyDao().insert(gift)
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddGiftActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteGift() {
        editingGift?.let { gift ->
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage("确定删除「${gift.giverName}」的礼金记录？")
                .setPositiveButton("删除") { _, _ ->
                    lifecycleScope.launch {
                        AppDatabase.getInstance(this@AddGiftActivity)
                            .giftMoneyDao().delete(gift)
                        finish()
                    }
                }
                .setNegativeButton("取消", null)
                .let { DialogUtils.showRoundedCompat(it) }
        }
    }
}
