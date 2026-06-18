package com.construction.diary.cloud.ui.appliance

import android.app.DatePickerDialog
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
import com.construction.diary.cloud.data.entity.Appliance
import com.construction.diary.cloud.util.ImageUtils
import com.construction.diary.cloud.util.DialogUtils
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddApplianceActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etBrand: EditText
    private lateinit var etModel: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerChannel: Spinner
    private lateinit var tvPurchaseDate: TextView
    private lateinit var etPrice: EditText
    private lateinit var etWarrantyYears: EditText
    private lateinit var tvInstallDate: TextView
    private lateinit var etAfterSalePhone: EditText
    private lateinit var etSerialNumber: EditText
    private lateinit var etNote: EditText
    private lateinit var layoutImages: LinearLayout
    private lateinit var ivImage: ImageView
    private lateinit var tvImageHint: TextView
    private lateinit var btnRemoveImage: TextView
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var purchaseDate: Long = 0
    private var installDate: Long = 0
    private var warrantyExpireDate: Long = 0
    private var selectedImage: String? = null
    private var editId: Long = 0
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val categories = arrayOf("空调", "电视", "冰箱", "洗衣机", "热水器", "油烟机", "其他")
    private val channels = arrayOf("京东", "天猫", "苏宁", "线下门店", "其他")

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val saved = ImageUtils.saveImage(this@AddApplianceActivity, it)
            if (saved != null) {
                selectedImage = saved
                updateImageDisplay()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_appliance)

        etName = findViewById(R.id.etName)
        etBrand = findViewById(R.id.etBrand)
        etModel = findViewById(R.id.etModel)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerChannel = findViewById(R.id.spinnerChannel)
        tvPurchaseDate = findViewById(R.id.tvPurchaseDate)
        etPrice = findViewById(R.id.etPrice)
        etWarrantyYears = findViewById(R.id.etWarrantyYears)
        tvInstallDate = findViewById(R.id.tvInstallDate)
        etAfterSalePhone = findViewById(R.id.etAfterSalePhone)
        etSerialNumber = findViewById(R.id.etSerialNumber)
        etNote = findViewById(R.id.etNote)
        layoutImages = findViewById(R.id.layoutImages)
        ivImage = findViewById(R.id.ivImage)
        tvImageHint = findViewById(R.id.tvImageHint)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)

        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerChannel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, channels)

        tvPurchaseDate.setOnClickListener { showDatePicker { date -> purchaseDate = date; tvPurchaseDate.text = dateFormat.format(Date(date)); updateWarrantyExpire() } }
        tvInstallDate.setOnClickListener { showDatePicker { date -> installDate = date; tvInstallDate.text = dateFormat.format(Date(date)) } }
        layoutImages.setOnClickListener { pickImage.launch("image/*") }
        btnRemoveImage.setOnClickListener {
            selectedImage = null
            updateImageDisplay()
        }
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        etWarrantyYears.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) updateWarrantyExpire() }

        editId = intent.getLongExtra("appliance_id", 0)
        if (editId > 0) {
            findViewById<TextView>(R.id.tvTitle).text = "编辑电器"
            btnDelete.visibility = View.VISIBLE
            loadData()
        } else {
            findViewById<TextView>(R.id.tvTitle).text = "添加电器"
        }

        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun updateImageDisplay() {
        if (selectedImage != null) {
            val file = ImageUtils.getImageFile(this, selectedImage!!)
            if (file.exists()) {
                ivImage.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
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

    private fun showDatePicker(onDateSet: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d, 0, 0, 0)
            onDateSet(cal.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateWarrantyExpire() {
        if (purchaseDate > 0) {
            val years = etWarrantyYears.text.toString().toIntOrNull() ?: 0
            if (years > 0) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = purchaseDate
                    add(Calendar.YEAR, years)
                }
                warrantyExpireDate = cal.timeInMillis
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@AddApplianceActivity).applianceDao()
            val list = dao.getAllList()
            val appliance = list.find { it.id == editId } ?: return@launch
            etName.setText(appliance.name)
            etBrand.setText(appliance.brand)
            etModel.setText(appliance.model)
            spinnerCategory.setSelection(categories.indexOf(appliance.category).coerceAtLeast(0))
            spinnerChannel.setSelection(channels.indexOf(appliance.purchaseChannel).coerceAtLeast(0))
            purchaseDate = appliance.purchaseDate
            if (purchaseDate > 0) tvPurchaseDate.text = dateFormat.format(Date(purchaseDate))
            etPrice.setText(if (appliance.price > 0) String.format("%.2f", appliance.price) else "")
            etWarrantyYears.setText(if (appliance.warrantyYears > 0) appliance.warrantyYears.toString() else "")
            warrantyExpireDate = appliance.warrantyExpireDate
            installDate = appliance.installDate
            if (installDate > 0) tvInstallDate.text = dateFormat.format(Date(installDate))
            etAfterSalePhone.setText(appliance.afterSalePhone)
            etSerialNumber.setText(appliance.serialNumber)
            etNote.setText(appliance.note)
            if (appliance.imagePaths.isNotBlank()) {
                selectedImage = appliance.imagePaths.split(",").first()
                updateImageDisplay()
            }
        }
    }

    private fun save() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            etName.error = "请输入名称"
            return
        }
        updateWarrantyExpire()

        val appliance = Appliance(
            id = editId,
            name = name,
            brand = etBrand.text.toString().trim(),
            model = etModel.text.toString().trim(),
            category = spinnerCategory.selectedItem.toString(),
            purchaseChannel = spinnerChannel.selectedItem.toString(),
            purchaseDate = purchaseDate,
            price = etPrice.text.toString().toDoubleOrNull() ?: 0.0,
            warrantyYears = etWarrantyYears.text.toString().toIntOrNull() ?: 0,
            warrantyExpireDate = warrantyExpireDate,
            installDate = installDate,
            afterSalePhone = etAfterSalePhone.text.toString().trim(),
            serialNumber = etSerialNumber.text.toString().trim(),
            note = etNote.text.toString().trim(),
            imagePaths = selectedImage ?: ""
        )

        val dao = AppDatabase.getInstance(this).applianceDao()
        lifecycleScope.launch {
            if (editId > 0) dao.update(appliance) else dao.insert(appliance)
            finish()
        }
    }

    private fun confirmDelete() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除这条电器记录吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    val dao = AppDatabase.getInstance(this@AddApplianceActivity).applianceDao()
                    dao.getAllList().find { it.id == editId }?.let { dao.delete(it) }
                    finish()
                }
            }
            .setNegativeButton("取消", null)
            .let { DialogUtils.showRoundedCompat(it) }
    }
}
