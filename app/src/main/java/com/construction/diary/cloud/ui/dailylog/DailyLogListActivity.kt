package com.construction.diary.cloud.ui.dailylog

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.DailyLog
import com.construction.diary.cloud.util.DateUtils
import com.construction.diary.cloud.util.DialogUtils
import com.construction.diary.cloud.util.PhaseUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DailyLogListActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dailylog_list)
        findViewById<TextView>(R.id.tvTitle).text = "📝 施工日志"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddDailyLogActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            AppDatabase.getInstance(this@DailyLogListActivity).dailyLogDao().getAll().collectLatest { logs ->
                container.removeAllViews()
                tvEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
                logs.forEach { addRow(it) }
            }
        }
    }

    private fun addRow(log: DailyLog) {
        val dp = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.card_bg)
            setPadding((20*dp).toInt(), (16*dp).toInt(), (20*dp).toInt(), (16*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8*dp).toInt() }
        }

        // Date + weather + phase
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = DateUtils.formatDate(log.date)
            setTextColor(getColor(R.color.mi_orange)); textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        if (log.weather.isNotEmpty()) {
            header.addView(TextView(this).apply {
                text = "  ☁️ ${log.weather}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
            })
        }
        if (log.phase.isNotEmpty()) {
            header.addView(TextView(this).apply {
                text = "  ${PhaseUtils.emoji(log.phase)} ${log.phase}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
            })
        }
        card.addView(header)

        // Content
        card.addView(TextView(this).apply {
            text = log.content
            setTextColor(getColor(R.color.mi_text_primary)); textSize = 14f
            setPadding(0, (8*dp).toInt(), 0, 0)
        })

        // Workers
        if (log.workers.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "👷 工人: ${log.workers}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
                setPadding(0, (4*dp).toInt(), 0, 0)
            })
        }

        // Progress bar with fill color
        if (log.progress > 0) {
            val barContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (8*dp).toInt(), 0, 0)
            }
            // Progress track with fill
            val barTrack = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, (8*dp).toInt(), 1f)
                val trackDrawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(getColor(R.color.mi_divider))
                    cornerRadius = 4 * dp
                }
                background = trackDrawable
                clipToOutline = true
                showDividers = LinearLayout.SHOW_DIVIDER_NONE
            }
            // Fill portion
            val barFill = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, (8*dp).toInt(), log.progress.toFloat())
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(getColor(R.color.mi_success))
                    cornerRadius = 4 * dp
                }
            }
            // Spacer
            val spacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, (8*dp).toInt(), (100 - log.progress).toFloat())
            }
            barTrack.addView(barFill)
            barTrack.addView(spacer)
            barContainer.addView(barTrack)
            barContainer.addView(TextView(this).apply {
                text = "${log.progress}%"
                setTextColor(getColor(R.color.mi_success)); textSize = 11f
                setPadding((8*dp).toInt(), 0, 0, 0)
            })
            card.addView(barContainer)
        }

        card.setOnClickListener {
            startActivity(Intent(this, AddDailyLogActivity::class.java).putExtra("log_id", log.id))
        }
        card.setOnLongClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this).setTitle("删除日志").setMessage("确定删除这条施工日志？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch { AppDatabase.getInstance(this@DailyLogListActivity).dailyLogDao().delete(log) }
                    }.setNegativeButton("取消", null)
            )
            true
        }
        container.addView(card)
    }
}
