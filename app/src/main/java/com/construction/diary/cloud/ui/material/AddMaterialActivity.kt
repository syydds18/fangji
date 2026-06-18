package com.construction.diary.cloud.ui.material

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Material
import com.construction.diary.cloud.util.DateUtils
import androidx.appcompat.app.AlertDialog
import com.construction.diary.cloud.util.DialogUtils
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch

class AddMaterialActivity : AppCompatActivity() {
    private var materialId: Long = 0
    private var selectedDate = System.currentTimeMillis()
    private var editingMaterial: Material? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_material)

        materialId = intent.getLongExtra("material_id", 0)
        findViewById<TextView>(R.id.tvTitle).text = if (materialId > 0) "编辑材料" else "添加材料"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etName)
        val etSpec = findViewById<EditText>(R.id.etSpec)
        val etQuantity = findViewById<EditText>(R.id.etQuantity)
        val etBudget = findViewById<EditText>(R.id.etBudget)
        val etActual = findViewById<EditText>(R.id.etActual)
        val etBuyer = findViewById<EditText>(R.id.etBuyer)
        val spinnerStatus = findViewById<Spinner>(R.id.spinnerStatus)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        // Status spinner
        spinnerStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("待购", "已购", "已到货"))

        // Load existing
        if (materialId > 0) {
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@AddMaterialActivity)
                val all = db.materialDao().getAllList()
                editingMaterial = all.find { it.id == materialId }
                val m = editingMaterial ?: return@launch
                etName.setText(m.name)
                etSpec.setText(m.spec)
                etQuantity.setText(m.quantity)
                etBudget.setText(m.budgetAmount.toString())
                etActual.setText(m.actualAmount.toString())
                etBuyer.setText(m.buyer)
                spinnerStatus.setSelection(m.status)
                etNote.setText(m.note)
                selectedDate = m.createdAt
            }
        }

        btnDelete.setOnClickListener {
            editingMaterial?.let { m ->
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("确定删除材料「${m.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            AppDatabase.getInstance(this@AddMaterialActivity).materialDao().delete(m)
                            finish()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .let { DialogUtils.showRoundedCompat(it) }
            }
        }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) { etName.error = "请输入材料名称"; return@setOnClickListener }
            val material = Material(
                id = materialId,
                name = name,
                spec = etSpec.text.toString().trim(),
                quantity = etQuantity.text.toString().trim(),
                budgetAmount = etBudget.text.toString().toDoubleOrNull() ?: 0.0,
                actualAmount = etActual.text.toString().toDoubleOrNull() ?: 0.0,
                buyer = etBuyer.text.toString().trim(),
                status = spinnerStatus.selectedItemPosition,
                note = etNote.text.toString().trim()
            )
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@AddMaterialActivity)
                    if (materialId > 0) db.materialDao().update(material)
                    else db.materialDao().insert(material)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@AddMaterialActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
