package com.construction.diary.cloud.ui.maintenance

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
import com.construction.diary.cloud.data.entity.Maintenance
import com.construction.diary.cloud.util.ImageUtils
import androidx.appcompat.app.AlertDialog
import com.construction.diary.cloud.util.DialogUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddMaintenanceActivity : AppCompatActivity() {
    private var maintenanceId: Long = 0
    private var imagePath: String = ""
    private var editingMaintenance: Maintenance? = null
    private var selectedDate: Long = System.currentTimeMillis()
    private lateinit var ivImage: ImageView
    private lateinit var tvImageHint: TextView
    private lateinit var btnRemoveImage: View
    private lateinit var tvRepairDate: TextView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveImage(it) }
    }

    private val categories = listOf("水电", "墙面", "地面", "门窗", "管道", "电器", "屋顶", "其他")
    private val statuses = listOf("待维修", "维修中", "已完成")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_maintenance)
        maintenanceId = intent.getLongExtra("maintenance_id", 0)
        findViewById<TextView>(R.id.tvTitle).text = if (maintenanceId > 0) "编辑维修" else "添加维修"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        tvRepairDate = findViewById(R.id.tvRepairDate)
        val etCost = findViewById<EditText>(R.id.etCost)
        val etWorkerName = findViewById<EditText>(R.id.etWorkerName)
        val etWorkerPhone = findViewById<EditText>(R.id.etWorkerPhone)
        val spinnerStatus = findViewById<Spinner>(R.id.spinnerStatus)
        val etCause = findViewById<EditText>(R.id.etCause)
        val etNote = findViewById<EditText>(R.id.etNote)
        ivImage = findViewById(R.id.ivImage)
        tvImageHint = findViewById(R.id.tvImageHint)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        val btnDelete = findViewById<TextView>(R.id.btnDelete)

        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("（选择类别）") + categories)
        spinnerStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)

        // Date picker
        updateDateDisplay()
        tvRepairDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            DatePickerDialog(this, { _, year, month, day ->
                cal.set(year, month, day)
                selectedDate = cal.timeInMillis
                updateDateDisplay()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Image picker
        findViewById<View>(R.id.layoutImages).setOnClickListener {
            pickImage.launch("image/*")
        }
        btnRemoveImage.setOnClickListener {
            imagePath = ""
            updateImageDisplay()
        }

        if (maintenanceId > 0) {
            btnDelete.visibility = View.VISIBLE
            lifecycleScope.launch {
                editingMaintenance = AppDatabase.getInstance(this@AddMaintenanceActivity).maintenanceDao().getAllList().find { it.id == maintenanceId }
                val m = editingMaintenance ?: return@launch
                etTitle.setText(m.title); etLocation.setText(m.location)
                etDescription.setText(m.description); etCost.setText(if (m.cost > 0) m.cost.toString() else "")
                etWorkerName.setText(m.workerName); etWorkerPhone.setText(m.workerPhone)
                etCause.setText(m.cause); etNote.setText(m.note)
                imagePath = m.imagePaths.split(",").firstOrNull() ?: m.imagePaths
                selectedDate = m.repairDate
                updateDateDisplay()
                val catIdx = categories.indexOf(m.category)
                if (catIdx >= 0) spinnerCategory.setSelection(catIdx + 1)
                val statusIdx = statuses.indexOf(m.status)
                if (statusIdx >= 0) spinnerStatus.setSelection(statusIdx)
                updateImageDisplay()
            }
        }

        btnDelete.setOnClickListener {
            editingMaintenance?.let { m ->
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("确定删除维修记录「${m.title}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            AppDatabase.getInstance(this@AddMaintenanceActivity).maintenanceDao().delete(m)
                            finish()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .let { DialogUtils.showRoundedCompat(it) }
            }
        }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) { etTitle.error = "请输入维修标题"; return@setOnClickListener }
            val cat = if (spinnerCategory.selectedItemPosition > 0) categories[spinnerCategory.selectedItemPosition - 1] else ""
            val status = statuses[spinnerStatus.selectedItemPosition]
            val costStr = etCost.text.toString().trim()
            val cost = costStr.toDoubleOrNull() ?: 0.0
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getInstance(this@AddMaintenanceActivity)
                    val m = Maintenance(
                        id = maintenanceId, title = title, category = cat,
                        location = etLocation.text.toString().trim(),
                        description = etDescription.text.toString().trim(),
                        repairDate = selectedDate, cost = cost,
                        workerName = etWorkerName.text.toString().trim(),
                        workerPhone = etWorkerPhone.text.toString().trim(),
                        status = status, cause = etCause.text.toString().trim(),
                        note = etNote.text.toString().trim(), imagePaths = imagePath
                    )
                    if (maintenanceId > 0) db.maintenanceDao().update(m) else db.maintenanceDao().insert(m)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@AddMaintenanceActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tvRepairDate.text = sdf.format(Date(selectedDate))
    }

    private fun saveImage(uri: Uri) {
        lifecycleScope.launch {
            val saved = ImageUtils.saveImage(this@AddMaintenanceActivity, uri)
            if (saved != null) {
                imagePath = saved
                updateImageDisplay()
            }
        }
    }

    private fun updateImageDisplay() {
        if (imagePath.isNotEmpty()) {
            val file = ImageUtils.getImageFile(this, imagePath)
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
