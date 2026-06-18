package com.construction.diary.cloud

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.construction.diary.cloud.util.ImageUtils

class PhotoViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageView = ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(getColor(R.color.black))
        }
        setContentView(imageView)

        val fileName = intent.getStringExtra("image_file") ?: return
        val file = ImageUtils.getImageFile(this, fileName)
        if (file.exists()) {
            imageView.setImageBitmap(android.graphics.BitmapFactory.decodeFile(file.absolutePath))
        }
        imageView.setOnClickListener { finish() }
    }
}
