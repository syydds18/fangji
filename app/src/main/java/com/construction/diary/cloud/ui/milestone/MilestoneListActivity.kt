package com.construction.diary.cloud.ui.milestone

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Milestone
import com.construction.diary.cloud.util.DateUtils
import com.construction.diary.cloud.util.DialogUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MilestoneListActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_milestone_list)
        findViewById<TextView>(R.id.tvTitle).text = "📅 建房大事记"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.fabAdd).setOnClickListener { showAddDialog() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            AppDatabase.getInstance(this@MilestoneListActivity).milestoneDao().getAll().collectLatest { list ->
                container.removeAllViews()
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                list.forEach { addRow(it) }
            }
        }
    }

    private fun showAddDialog(editId: Long = 0, existing: Milestone? = null) {
        val dp = resources.displayMetrics.density
        var selectedDate = existing?.date ?: System.currentTimeMillis()
        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24*dp).toInt(), (8*dp).toInt(), (24*dp).toInt(), 0)
        }
        val etTitle = EditText(this).apply {
            hint = "事件标题（如：上梁、封顶）"
            background = getDrawable(R.drawable.bg_input_field)
            setPadding((12*dp).toInt(), (10*dp).toInt(), (12*dp).toInt(), (10*dp).toInt())
            textSize = 15f; setTextColor(getColor(R.color.mi_text_primary))
            existing?.let { setText(it.title) }
        }
        val tvDate = TextView(this).apply {
            text = "📅 ${DateUtils.formatDate(selectedDate)}"
            background = getDrawable(R.drawable.bg_input_field)
            setPadding((12*dp).toInt(), (12*dp).toInt(), (12*dp).toInt(), (12*dp).toInt())
            textSize = 15f; setTextColor(getColor(R.color.mi_orange))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12*dp).toInt() }
            setOnClickListener {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                android.app.DatePickerDialog(
                    this@MilestoneListActivity,
                    { _, year, month, day ->
                        val picked = java.util.Calendar.getInstance().apply {
                            set(year, month, day)
                        }.timeInMillis
                        selectedDate = picked
                        text = "📅 ${DateUtils.formatDate(picked)}"
                    },
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH),
                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        val etDesc = EditText(this).apply {
            hint = "详细描述"
            background = getDrawable(R.drawable.bg_input_field)
            setPadding((12*dp).toInt(), (10*dp).toInt(), (12*dp).toInt(), (10*dp).toInt())
            textSize = 15f; setTextColor(getColor(R.color.mi_text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (80*dp).toInt()
            ).apply { topMargin = (12*dp).toInt() }
            gravity = android.view.Gravity.TOP
            existing?.let { setText(it.description) }
        }
        lay.addView(etTitle); lay.addView(tvDate); lay.addView(etDesc)

        DialogUtils.showRoundedCompat(
            AlertDialog.Builder(this)
                .setTitle(if (editId > 0) "编辑大事记" else "添加大事记")
                .setView(lay)
                .setPositiveButton("保存") { _, _ ->
                    val title = etTitle.text.toString().trim()
                    if (title.isEmpty()) return@setPositiveButton
                    lifecycleScope.launch {
                        val db = AppDatabase.getInstance(this@MilestoneListActivity)
                        val m = Milestone(id = editId, title = title, date = selectedDate, description = etDesc.text.toString().trim())
                        if (editId > 0) db.milestoneDao().update(m) else db.milestoneDao().insert(m)
                    }
                }
                .setNegativeButton("取消", null)
        )
    }

    private fun addRow(m: Milestone) {
        val dp = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = getDrawable(R.drawable.card_bg)
            setPadding((20*dp).toInt(), (16*dp).toInt(), (20*dp).toInt(), (16*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8*dp).toInt() }
        }
        // Date column
        val dateCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams((60*dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val tvDate = TextView(this).apply {
            text = DateUtils.formatDate(m.date)
            setTextColor(getColor(R.color.mi_orange)); textSize = 12f
        }
        dateCol.addView(tvDate)

        // Content column
        val contentCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding((12*dp).toInt(), 0, 0, 0)
        }
        val tvTitle = TextView(this).apply {
            text = "📌 ${m.title}"
            setTextColor(getColor(R.color.mi_text_primary)); textSize = 15f
        }
        contentCol.addView(tvTitle)
        if (m.description.isNotEmpty()) {
            val tvDesc = TextView(this).apply {
                text = m.description
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 13f
                setPadding(0, (4*dp).toInt(), 0, 0)
            }
            contentCol.addView(tvDesc)
        }

        row.addView(dateCol); row.addView(contentCol)
        row.setOnClickListener { showAddDialog(m.id, m) }
        row.setOnLongClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this).setTitle("删除大事记").setMessage("确定删除「${m.title}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch { AppDatabase.getInstance(this@MilestoneListActivity).milestoneDao().delete(m) }
                    }.setNegativeButton("取消", null)
            ); true
        }
        container.addView(row)
    }
}
