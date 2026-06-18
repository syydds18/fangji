package com.construction.diary.cloud.ui.photo

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.PhotoViewActivity
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Expense
import com.construction.diary.cloud.data.entity.Milestone
import com.construction.diary.cloud.data.entity.StandalonePhoto
import com.construction.diary.cloud.util.DateUtils
import com.construction.diary.cloud.util.ImageUtils
import com.construction.diary.cloud.util.DialogUtils
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch
import android.content.Intent
import android.graphics.BitmapFactory

class PhotoGalleryActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { saveAndShow(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_gallery)
        findViewById<TextView>(R.id.tvTitle).text = "📸 进度相册"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.fabAdd).setOnClickListener { pickImage.launch("image/*") }
    }

    override fun onResume() {
        super.onResume()
        loadPhotos()
    }

    private fun saveAndShow(uri: Uri) {
        lifecycleScope.launch {
            val fileName = ImageUtils.saveImage(this@PhotoGalleryActivity, uri)
            if (fileName != null) {
                val db = AppDatabase.getInstance(this@PhotoGalleryActivity)
                val photo = StandalonePhoto(fileName = fileName)
                db.standalonePhotoDao().insert(photo)
            }
            loadPhotos()
        }
    }

    private fun loadPhotos() {
        lifecycleScope.launch {
            container.removeAllViews()
            val db = AppDatabase.getInstance(this@PhotoGalleryActivity)

            // Collect all image filenames from database
            val dbImages = mutableSetOf<String>()
            db.expenseDao().getAllList().forEach { e: Expense ->
                e.imagePaths.split(",").map { s: String -> s.trim() }.filter { s: String -> s.isNotEmpty() }.forEach { s: String -> dbImages.add(s) }
            }
            db.milestoneDao().getAllList().forEach { m: Milestone ->
                m.imagePaths.split(",").map { s: String -> s.trim() }.filter { s: String -> s.isNotEmpty() }.forEach { s: String -> dbImages.add(s) }
            }
            db.dailyLogDao().getAllList().forEach { l ->
                l.imagePaths.split(",").map { s: String -> s.trim() }.filter { s: String -> s.isNotEmpty() }.forEach { s: String -> dbImages.add(s) }
            }
            db.supplierDao().getAllList().forEach { s ->
                s.qrCodePath.split(",").map { t: String -> t.trim() }.filter { t: String -> t.isNotEmpty() }.forEach { t: String -> dbImages.add(t) }
            }
            db.documentDao().getAllList().forEach { d ->
                d.imagePaths.split(",").map { s: String -> s.trim() }.filter { s: String -> s.isNotEmpty() }.forEach { s: String -> dbImages.add(s) }
            }
            db.maintenanceDao().getAllList().forEach { m ->
                m.imagePaths.split(",").map { s: String -> s.trim() }.filter { s: String -> s.isNotEmpty() }.forEach { s: String -> dbImages.add(s) }
            }
            db.applianceDao().getAllList().forEach { a ->
                a.imagePaths.split(",").map { s: String -> s.trim() }.filter { s: String -> s.isNotEmpty() }.forEach { s: String -> dbImages.add(s) }
            }

            // Also collect standalone photos from database
            val standalonePhotos = db.standalonePhotoDao().getAllList()
            standalonePhotos.forEach { p ->
                if (p.fileName.isNotEmpty()) dbImages.add(p.fileName)
            }

            // Convert DB image names to files, filter out missing ones
            val seenPaths = mutableSetOf<String>()
            val allFiles = dbImages.mapNotNull { name ->
                val f = ImageUtils.getImageFile(this@PhotoGalleryActivity, name)
                if (f.exists() && seenPaths.add(f.absolutePath)) f else null
            }.sortedByDescending { it.lastModified() }

            tvEmpty.visibility = if (allFiles.isEmpty()) View.VISIBLE else View.GONE

            // Grid: 3 columns
            val dp = resources.displayMetrics.density
            val itemSize = ((resources.displayMetrics.widthPixels - (40 + 16) * dp) / 3).toInt()
            var row: LinearLayout? = null

            allFiles.forEachIndexed { index, file ->
                if (index % 3 == 0) {
                    row = LinearLayout(this@PhotoGalleryActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = (8*dp).toInt() }
                    }
                    container.addView(row)
                }
                val itemView = layoutInflater.inflate(R.layout.item_photo, row, false)
                val iv = itemView.findViewById<ImageView>(R.id.ivPhoto)
                val bm = BitmapFactory.decodeFile(file.absolutePath)
                iv.setImageBitmap(bm)
                itemView.layoutParams = LinearLayout.LayoutParams(itemSize, itemSize).apply {
                    marginEnd = if (index % 3 < 2) (8*dp).toInt() else 0
                }
                itemView.setOnClickListener {
                    startActivity(Intent(this@PhotoGalleryActivity, PhotoViewActivity::class.java)
                        .putExtra("image_file", file.name))
                }
                itemView.setOnLongClickListener {
                    AlertDialog.Builder(this@PhotoGalleryActivity)
                        .setTitle("删除照片")
                        .setMessage("确定删除这张照片？")
                        .setPositiveButton("删除") { _, _ ->
                            file.delete()
                            loadPhotos()
                        }
                        .setNegativeButton("取消", null)
                        .let { DialogUtils.showRoundedCompat(it) }
                    true
                }
                row?.addView(itemView)
            }
        }
    }
}
