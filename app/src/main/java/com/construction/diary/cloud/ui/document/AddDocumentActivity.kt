package com.construction.diary.cloud.ui.document

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.construction.diary.cloud.util.DialogUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Document
import com.construction.diary.cloud.util.ImageUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddDocumentActivity : AppCompatActivity() {
    private var documentId: Long = 0
    private var imagePaths: String = ""
    private var editingDocument: Document? = null
    private var issueDate: Long = 0
    private var expireDate: Long = 0
    private lateinit var ivImage: ImageView
    private lateinit var tvImageHint: TextView
    private lateinit var btnRemoveImage: View
    private lateinit var tvIssueDate: TextView
    private lateinit var tvExpireDate: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val documentCategories = listOf("房产证", "土地证", "施工许可证", "竣工验收", "购房合同", "保险单", "发票", "其他")

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_document)
        documentId = intent.getLongExtra("document_id", 0)
        findViewById<TextView>(R.id.tvTitle).text = if (documentId > 0) "编辑证件" else "添加证件"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etName)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val etStorageLocation = findViewById<EditText>(R.id.etStorageLocation)
        val etRelatedParty = findViewById<EditText>(R.id.etRelatedParty)
        tvIssueDate = findViewById(R.id.tvIssueDate)
        tvExpireDate = findViewById(R.id.tvExpireDate)
        val etNote = findViewById<EditText>(R.id.etNote)
        ivImage = findViewById(R.id.ivImage)
        tvImageHint = findViewById(R.id.tvImageHint)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("（选择类别）") + documentCategories)

        // Date pickers
        tvIssueDate.setOnClickListener {
            val cal = Calendar.getInstance()
            if (issueDate > 0) cal.timeInMillis = issueDate
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                issueDate = cal.timeInMillis
                tvIssueDate.text = dateFormat.format(Date(issueDate))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        tvExpireDate.setOnClickListener {
            val cal = Calendar.getInstance()
            if (expireDate > 0) cal.timeInMillis = expireDate
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                expireDate = cal.timeInMillis
                tvExpireDate.text = dateFormat.format(Date(expireDate))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Image click
        findViewById<View>(R.id.layoutImages).setOnClickListener {
            pickImage.launch("image/*")
        }

        btnRemoveImage.setOnClickListener {
            imagePaths = ""
            updateImageDisplay()
        }

        if (documentId > 0) {
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                editingDocument = AppDatabase.getInstance(this@AddDocumentActivity).documentDao().getAllList().find { it.id == documentId }
                val d = editingDocument ?: return@launch
                etName.setText(d.name)
                etStorageLocation.setText(d.storageLocation)
                etRelatedParty.setText(d.relatedParty)
                etNote.setText(d.note)
                imagePaths = d.imagePaths.split(",").firstOrNull() ?: d.imagePaths
                issueDate = d.issueDate
                expireDate = d.expireDate
                if (issueDate > 0) tvIssueDate.text = dateFormat.format(Date(issueDate))
                if (expireDate > 0) tvExpireDate.text = dateFormat.format(Date(expireDate))
                val catIdx = documentCategories.indexOf(d.category)
                if (catIdx >= 0) spinnerCategory.setSelection(catIdx + 1)
                updateImageDisplay()
            }
        }

        btnDelete.setOnClickListener {
            editingDocument?.let { d ->
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("确定删除证件「${d.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            AppDatabase.getInstance(this@AddDocumentActivity).documentDao().delete(d)
                            finish()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .let { DialogUtils.showRoundedCompat(it) }
            }
        }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) { etName.error = "请输入文档名称"; return@setOnClickListener }
            val storageLocation = etStorageLocation.text.toString().trim()
            if (storageLocation.isEmpty()) { etStorageLocation.error = "请输入存放位置"; return@setOnClickListener }
            val cat = if (spinnerCategory.selectedItemPosition > 0) documentCategories[spinnerCategory.selectedItemPosition - 1] else ""
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@AddDocumentActivity)
                    val d = Document(
                        id = documentId, name = name, category = cat,
                        storageLocation = storageLocation,
                        relatedParty = etRelatedParty.text.toString().trim(),
                        issueDate = issueDate, expireDate = expireDate,
                        note = etNote.text.toString().trim(), imagePaths = imagePaths
                    )
                    if (documentId > 0) db.documentDao().update(d) else db.documentDao().insert(d)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@AddDocumentActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveImage(uri: Uri) {
        lifecycleScope.launch {
            val saved = ImageUtils.saveImage(this@AddDocumentActivity, uri)
            if (saved != null) {
                imagePaths = saved
                updateImageDisplay()
            }
        }
    }

    private fun updateImageDisplay() {
        if (imagePaths.isNotEmpty()) {
            val file = ImageUtils.getImageFile(this, imagePaths)
            if (file.exists()) {
                ivImage.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                ivImage.visibility = View.VISIBLE
                tvImageHint.visibility = View.GONE
                btnRemoveImage.visibility = View.VISIBLE
                return
            }
        }
        ivImage.visibility = View.GONE
        tvImageHint.visibility = View.VISIBLE
        btnRemoveImage.visibility = View.GONE
    }
}
