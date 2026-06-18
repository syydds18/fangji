package com.construction.diary.cloud.ui.stats

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.dao.BuyerSum
import com.construction.diary.cloud.data.dao.PhaseSum
import com.construction.diary.cloud.util.MoneyUtils
import com.construction.diary.cloud.util.PhaseUtils
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {
    private lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        findViewById<TextView>(R.id.tvTitle).text = "📊 统计报表"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        contentContainer = findViewById(R.id.contentContainer)
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@StatisticsActivity)
            val phaseSums = db.expenseDao().getPhaseSums()
            val topBuyers = db.expenseDao().getTopBuyers()
            val totalExpense = db.expenseDao().getTotalAmount() ?: 0.0
            val totalGift = db.giftMoneyDao().getTotalAmount() ?: 0.0
            val budget = db.projectConfigDao().getValue("budget")?.toDoubleOrNull() ?: 0.0

            contentContainer.removeAllViews()
            val dp = resources.displayMetrics.density

            // === Overview Card ===
            val overviewCard = createCard()
            addSectionTitle(overviewCard, "💰 资金概览")
            val budgetUsed = if (budget > 0) (totalExpense / budget * 100).toInt() else 0
            addStatRow(overviewCard, "启动资金", MoneyUtils.format(budget), getColor(R.color.mi_blue))
            addStatRow(overviewCard, "总支出", MoneyUtils.format(totalExpense), getColor(R.color.mi_danger))
            addStatRow(overviewCard, "礼金收入", MoneyUtils.format(totalGift), getColor(R.color.mi_success))
            addStatRow(overviewCard, "结余", MoneyUtils.format(budget + totalGift - totalExpense),
                if (budget + totalGift - totalExpense >= 0) getColor(R.color.mi_success) else getColor(R.color.mi_danger))
            if (budget > 0) {
                addStatRow(overviewCard, "预算使用", "${budgetUsed}%", getColor(R.color.mi_warning))
            }
            contentContainer.addView(overviewCard)

            // === Phase Pie Chart ===
            if (phaseSums.isNotEmpty()) {
                val pieCard = createCard()
                addSectionTitle(pieCard, "🏗️ 各阶段花销分布")
                addPieChart(pieCard, phaseSums, totalExpense)
                contentContainer.addView(pieCard)

                // Phase detail list
                val detailCard = createCard()
                addSectionTitle(detailCard, "📋 各阶段明细")
                phaseSums.forEachIndexed { index, ps ->
                    val pct = if (totalExpense > 0) (ps.total / totalExpense * 100).toInt() else 0
                    val emoji = PhaseUtils.emoji(ps.phase)
                    addStatRow(detailCard, "$emoji ${ps.phase}", "${MoneyUtils.format(ps.total)} ($pct%)",
                        getPhaseColor(index))
                }
                contentContainer.addView(detailCard)
            }

            // === Top Buyers ===
            if (topBuyers.isNotEmpty()) {
                val buyerCard = createCard()
                addSectionTitle(buyerCard, "🛒 花销排行（谁买的最多）")
                topBuyers.forEachIndexed { index, bs ->
                    val rank = when(index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index+1}." }
                    addStatRow(buyerCard, "$rank ${bs.buyer}", MoneyUtils.format(bs.total), getPhaseColor(index))
                }
                contentContainer.addView(buyerCard)
            }
        }
    }

    private fun createCard(): LinearLayout {
        val dp = resources.displayMetrics.density
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.card_bg)
            setPadding((20*dp).toInt(), (16*dp).toInt(), (20*dp).toInt(), (16*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12*dp).toInt() }
        }
    }

    private fun addSectionTitle(card: LinearLayout, title: String) {
        val dp = resources.displayMetrics.density
        card.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(getColor(R.color.mi_text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12*dp).toInt() }
        })
    }

    private fun addStatRow(card: LinearLayout, label: String, value: String, valueColor: Int) {
        val dp = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (40*dp).toInt()
            )
        }
        row.addView(TextView(this).apply {
            text = label; textSize = 14f
            setTextColor(getColor(R.color.mi_text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = value; textSize = 14f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(valueColor)
        })
        card.addView(row)
    }

    private fun addPieChart(card: LinearLayout, phaseSums: List<PhaseSum>, total: Double) {
        val dp = resources.displayMetrics.density
        val chartSize = (200 * dp).toInt()

        val chartView = object : View(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasureSpec(widthMeasureSpec, chartSize)
            }

            private fun setMeasureSpec(measureSpec: Int, desired: Int) {
                val specMode = MeasureSpec.getMode(measureSpec)
                val specSize = MeasureSpec.getSize(measureSpec)
                val size = when (specMode) {
                    MeasureSpec.EXACTLY -> specSize
                    MeasureSpec.AT_MOST -> desired.coerceAtMost(specSize)
                    else -> desired
                }
                setMeasuredDimension(size, size)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val radius = cx * 0.7f
                val innerRadius = radius * 0.5f
                val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                var startAngle = -90f
                phaseSums.forEachIndexed { index, ps ->
                    val sweepAngle = (ps.total / total * 360).toFloat()
                    paint.color = getPhaseColor(index)
                    canvas.drawArc(rect, startAngle, sweepAngle, true, paint)
                    startAngle += sweepAngle
                }

                // Inner circle (donut)
                paint.color = getColor(R.color.mi_card)
                canvas.drawCircle(cx, cy, innerRadius, paint)

                // Center text
                paint.color = getColor(R.color.mi_text_primary)
                paint.textSize = 14f * resources.displayMetrics.density
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(MoneyUtils.format(total), cx, cy + 6 * dp, paint)
            }
        }
        chartView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, chartSize
        ).apply { bottomMargin = (8*dp).toInt() }
        card.addView(chartView)
    }

    private fun getPhaseColor(index: Int): Int {
        val colors = intArrayOf(
            getColor(R.color.mi_orange), getColor(R.color.mi_blue),
            getColor(R.color.mi_success), getColor(R.color.mi_danger),
            getColor(R.color.mi_purple), getColor(R.color.mi_warning),
            getColor(R.color.mi_blue_gray)
        )
        return colors[index % colors.size]
    }
}
