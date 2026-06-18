package com.construction.diary.cloud.ui.maintenance

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Maintenance
import com.construction.diary.cloud.util.DialogUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MaintenanceListActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maintenance_list)
        findViewById<TextView>(R.id.tvTitle).text = "🔧 维修记录"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddMaintenanceActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            AppDatabase.getInstance(this@MaintenanceListActivity).maintenanceDao().getAll().collectLatest { list ->
                container.removeAllViews()
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                list.forEach { addRow(it) }
            }
        }
    }

    private fun addRow(m: Maintenance) {
        val dp = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.card_bg)
            setPadding((20*dp).toInt(), (16*dp).toInt(), (20*dp).toInt(), (16*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8*dp).toInt() }
        }

        // Header row: title + status chip
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = m.title; textSize = 16f
            setTextColor(getColor(R.color.mi_text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = m.status; textSize = 11f
            setTextColor(when (m.status) {
                "待维修" -> getColor(R.color.mi_danger)
                "维修中" -> getColor(R.color.mi_orange)
                "已完成" -> getColor(R.color.mi_success)
                else -> getColor(R.color.mi_orange)
            })
            background = getDrawable(R.drawable.bg_chip)
            setPadding((10*dp).toInt(), (4*dp).toInt(), (10*dp).toInt(), (4*dp).toInt())
        })
        card.addView(header)

        // Category
        if (m.category.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "📂 ${m.category}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 13f
                setPadding(0, (6*dp).toInt(), 0, 0)
            })
        }

        // Location
        if (m.location.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "📍 ${m.location}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
                setPadding(0, (4*dp).toInt(), 0, 0)
            })
        }

        // Cost
        if (m.cost > 0) {
            card.addView(TextView(this).apply {
                text = "💰 ¥${String.format("%.2f", m.cost)}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
                setPadding(0, (4*dp).toInt(), 0, 0)
            })
        }

        // Worker
        if (m.workerName.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "👤 ${m.workerName}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
                setPadding(0, (4*dp).toInt(), 0, 0)
            })
        }

        card.setOnClickListener {
            startActivity(Intent(this, AddMaintenanceActivity::class.java).putExtra("maintenance_id", m.id))
        }
        card.setOnLongClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this).setTitle("删除维修记录").setMessage("确定删除「${m.title}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch { AppDatabase.getInstance(this@MaintenanceListActivity).maintenanceDao().delete(m) }
                    }.setNegativeButton("取消", null)
            )
            true
        }
        container.addView(card)
    }
}
