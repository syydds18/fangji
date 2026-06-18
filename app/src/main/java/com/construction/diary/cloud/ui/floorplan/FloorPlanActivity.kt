package com.construction.diary.cloud.ui.floorplan

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.construction.diary.cloud.R
import java.io.File
import java.io.FileOutputStream

class FloorPlanActivity : AppCompatActivity() {
    private lateinit var ivFloorPlan: ImageView
    private lateinit var tvEmpty: LinearLayout
    private val floorPlanFile: File by lazy { File(getExternalFilesDir(null), "floor_plan.jpg") }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveFloorPlan(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floor_plan)
        findViewById<TextView>(R.id.tvTitle).text = "🏠 户型图"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        ivFloorPlan = findViewById(R.id.ivFloorPlan)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.btnImport).setOnClickListener { pickImage.launch("image/*") }
        loadFloorPlan()
    }

    private fun saveFloorPlan(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(floorPlanFile).use { out -> input.copyTo(out) }
        }
        loadFloorPlan()
    }

    private fun loadFloorPlan() {
        if (floorPlanFile.exists()) {
            ivFloorPlan.setImageBitmap(BitmapFactory.decodeFile(floorPlanFile.absolutePath))
            ivFloorPlan.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        } else {
            ivFloorPlan.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        }
    }
}
