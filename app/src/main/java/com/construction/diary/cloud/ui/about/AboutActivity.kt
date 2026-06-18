package com.construction.diary.cloud.ui.about

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.construction.diary.cloud.R

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<TextView>(R.id.tvTitle).text = "关于房迹"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }
}
