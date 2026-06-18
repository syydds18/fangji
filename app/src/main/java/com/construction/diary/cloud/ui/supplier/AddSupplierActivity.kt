package com.construction.diary.cloud.ui.supplier

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Supplier
import com.construction.diary.cloud.util.ImageUtils
import com.construction.diary.cloud.util.PhaseUtils
import androidx.appcompat.app.AlertDialog
import com.construction.diary.cloud.util.DialogUtils
import kotlinx.coroutines.launch

class AddSupplierActivity : AppCompatActivity() {
    private var supplierId: Long = 0
    private var qrCodePath: String = ""
    private var editingSupplier: Supplier? = null
    private lateinit var ivQrCode: ImageView
    private lateinit var tvQrHint: TextView
    private lateinit var btnRemoveQr: View

    private val pickQrCode = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveQrCode(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_supplier)
        supplierId = intent.getLongExtra("supplier_id", 0)
        findViewById<TextView>(R.id.tvTitle).text = if (supplierId > 0) "编辑供应商" else "添加供应商"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etName)
        val etContact = findViewById<EditText>(R.id.etContact)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAddress = findViewById<EditText>(R.id.etAddress)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val etNote = findViewById<EditText>(R.id.etNote)
        ivQrCode = findViewById(R.id.ivQrCode)
        tvQrHint = findViewById(R.id.tvQrHint)
        btnRemoveQr = findViewById(R.id.btnRemoveQr)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("（选择类别）") + PhaseUtils.SUPPLIER_CATEGORIES)

        // QR code click - pick image
        findViewById<View>(R.id.layoutQrCode).setOnClickListener {
            pickQrCode.launch("image/*")
        }

        // Remove QR code
        btnRemoveQr.setOnClickListener {
            qrCodePath = ""
            updateQrCodeDisplay()
        }

        if (supplierId > 0) {
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                editingSupplier = AppDatabase.getInstance(this@AddSupplierActivity).supplierDao().getAllList().find { it.id == supplierId }
                val s = editingSupplier ?: return@launch
                etName.setText(s.name); etContact.setText(s.contact); etPhone.setText(s.phone)
                etAddress.setText(s.address); etNote.setText(s.note)
                qrCodePath = s.qrCodePath.split(",").firstOrNull() ?: s.qrCodePath
                val catIdx = PhaseUtils.SUPPLIER_CATEGORIES.indexOf(s.category)
                if (catIdx >= 0) spinnerCategory.setSelection(catIdx + 1)
                updateQrCodeDisplay()
            }
        }

        btnDelete.setOnClickListener {
            editingSupplier?.let { s ->
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("确定删除供应商「${s.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            AppDatabase.getInstance(this@AddSupplierActivity).supplierDao().delete(s)
                            finish()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .let { DialogUtils.showRoundedCompat(it) }
            }
        }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) { etName.error = "请输入商家名称"; return@setOnClickListener }
            val cat = if (spinnerCategory.selectedItemPosition > 0) PhaseUtils.SUPPLIER_CATEGORIES[spinnerCategory.selectedItemPosition - 1] else ""
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@AddSupplierActivity)
                    val s = Supplier(id = supplierId, name = name, contact = etContact.text.toString().trim(),
                        phone = etPhone.text.toString().trim(), address = etAddress.text.toString().trim(),
                        category = cat, qrCodePath = qrCodePath, note = etNote.text.toString().trim())
                    if (supplierId > 0) db.supplierDao().update(s) else db.supplierDao().insert(s)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@AddSupplierActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveQrCode(uri: Uri) {
        lifecycleScope.launch {
            val saved = ImageUtils.saveImage(this@AddSupplierActivity, uri)
            if (saved != null) {
                qrCodePath = saved
                updateQrCodeDisplay()
            }
        }
    }

    private fun updateQrCodeDisplay() {
        if (qrCodePath.isNotEmpty()) {
            val file = ImageUtils.getImageFile(this, qrCodePath)
            if (file.exists()) {
                ivQrCode.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                ivQrCode.visibility = View.VISIBLE
                tvQrHint.visibility = View.GONE
                btnRemoveQr.visibility = View.VISIBLE
                return
            }
        }
        ivQrCode.visibility = View.GONE
        tvQrHint.visibility = View.VISIBLE
        btnRemoveQr.visibility = View.GONE
    }
}
