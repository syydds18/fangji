package com.construction.diary.cloud.ui.expense

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Expense
import com.construction.diary.cloud.util.DateUtils
import com.construction.diary.cloud.util.ImageUtils
import com.construction.diary.cloud.util.DialogUtils
import androidx.appcompat.app.AlertDialog
import com.construction.diary.cloud.util.PhaseUtils
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etItemName: EditText
    private lateinit var etBrand: EditText
    private lateinit var etAmount: EditText
    private lateinit var etBuyer: EditText
    private lateinit var spinnerPhase: Spinner
    private lateinit var etCategory: EditText
    private lateinit var tvDate: TextView
    private lateinit var etNote: EditText
    private lateinit var layoutImages: LinearLayout
    private lateinit var ivImage: ImageView
    private lateinit var tvImageHint: TextView
    private lateinit var btnRemoveImage: TextView
    private lateinit var btnSave: TextView
    private lateinit var btnDelete: TextView
    private lateinit var tvTitle: TextView

    private var selectedDate: Long = System.currentTimeMillis()
    private var editingExpense: Expense? = null
    private var selectedImage: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val fileName = ImageUtils.saveImage(this, it)
            if (fileName != null) {
                selectedImage = fileName
                updateImageDisplay()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        etItemName = findViewById(R.id.etItemName)
        etBrand = findViewById(R.id.etBrand)
        etAmount = findViewById(R.id.etAmount)
        etBuyer = findViewById(R.id.etBuyer)
        spinnerPhase = findViewById(R.id.spinnerPhase)
        etCategory = findViewById(R.id.etCategory)
        tvDate = findViewById(R.id.tvDate)
        etNote = findViewById(R.id.etNote)
        layoutImages = findViewById(R.id.layoutImages)
        ivImage = findViewById(R.id.ivImage)
        tvImageHint = findViewById(R.id.tvImageHint)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        tvTitle = findViewById(R.id.tvTitle)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        // Setup phase spinner
        val phases = listOf("请选择阶段") + PhaseUtils.PHASES
        spinnerPhase.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, phases)

        // Setup date
        tvDate.text = DateUtils.formatDate(selectedDate)
        tvDate.setOnClickListener { showDatePicker() }

        // Photo picker
        layoutImages.setOnClickListener { pickImage.launch("image/*") }
        btnRemoveImage.setOnClickListener {
            selectedImage = null
            updateImageDisplay()
        }

        // Check if editing
        val expenseId = intent.getLongExtra("expense_id", -1)
        if (expenseId > 0) {
            tvTitle.text = "编辑花销"
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@AddExpenseActivity)
                val all = db.expenseDao().getAllList()
                editingExpense = all.find { it.id == expenseId }
                editingExpense?.let { populateFields(it) }
            }
        }

        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { deleteExpense() }
    }

    private fun updateImageDisplay() {
        if (selectedImage != null) {
            val file = ImageUtils.getImageFile(this, selectedImage!!)
            if (file.exists()) {
                ivImage.setImageBitmap(android.graphics.BitmapFactory.decodeFile(file.absolutePath))
                ivImage.visibility = View.VISIBLE
                tvImageHint.visibility = View.GONE
                btnRemoveImage.visibility = View.VISIBLE
            } else {
                selectedImage = null
                ivImage.visibility = View.GONE
                tvImageHint.visibility = View.VISIBLE
                btnRemoveImage.visibility = View.GONE
            }
        } else {
            ivImage.visibility = View.GONE
            tvImageHint.visibility = View.VISIBLE
            btnRemoveImage.visibility = View.GONE
        }
    }

    private fun populateFields(expense: Expense) {
        etItemName.setText(expense.itemName)
        etBrand.setText(expense.brand)
        etAmount.setText(expense.amount.toString())
        etBuyer.setText(expense.buyer)
        etCategory.setText(expense.category)
        etNote.setText(expense.note)
        selectedDate = expense.date
        tvDate.text = DateUtils.formatDate(expense.date)

        val phaseIndex = PhaseUtils.PHASES.indexOf(expense.phase)
        if (phaseIndex >= 0) spinnerPhase.setSelection(phaseIndex + 1)

        if (expense.imagePaths.isNotEmpty()) {
            selectedImage = expense.imagePaths.split(",").first()
            updateImageDisplay()
        }
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
        val name = etItemName.text.toString().trim()
        if (name.isEmpty()) {
            etItemName.error = "请输入项目名称"
            return
        }
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val phaseIndex = spinnerPhase.selectedItemPosition
        val phase = if (phaseIndex > 0) PhaseUtils.PHASES[phaseIndex - 1] else ""

        val expense = (editingExpense ?: Expense()).copy(
            itemName = name,
            brand = etBrand.text.toString().trim(),
            amount = amount,
            buyer = etBuyer.text.toString().trim(),
            phase = phase,
            category = etCategory.text.toString().trim(),
            date = selectedDate,
            note = etNote.text.toString().trim(),
            imagePaths = selectedImage ?: ""
        )

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@AddExpenseActivity)
                if (editingExpense != null) {
                    db.expenseDao().update(expense)
                } else {
                    db.expenseDao().insert(expense)
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddExpenseActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteExpense() {
        editingExpense?.let { expense ->
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage("确定删除「${expense.itemName}」？")
                .setPositiveButton("删除") { _, _ ->
                    lifecycleScope.launch {
                        AppDatabase.getInstance(this@AddExpenseActivity)
                            .expenseDao().delete(expense)
                        finish()
                    }
                }
                .setNegativeButton("取消", null)
                .let { DialogUtils.showRoundedCompat(it) }
        }
    }
}
