package com.construction.diary.cloud.ui.document

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
import com.construction.diary.cloud.data.entity.Document
import com.construction.diary.cloud.util.DialogUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DocumentListActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_list)
        findViewById<TextView>(R.id.tvTitle).text = "📋 证件资料"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddDocumentActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            AppDatabase.getInstance(this@DocumentListActivity).documentDao().getAll().collectLatest { list ->
                container.removeAllViews()
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                list.forEach { addRow(it) }
            }
        }
    }

    private fun addRow(d: Document) {
        val dp = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.card_bg)
            setPadding((20 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt(), (16 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * dp).toInt() }
        }

        // Header row: name + category chip
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = d.name; textSize = 16f
            setTextColor(getColor(R.color.mi_text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (d.category.isNotEmpty()) {
            header.addView(TextView(this).apply {
                text = d.category; textSize = 11f
                setTextColor(getColor(R.color.mi_orange))
                background = getDrawable(R.drawable.bg_chip)
                setPadding((10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt())
            })
        }
        card.addView(header)

        // Storage location (prominent)
        if (d.storageLocation.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "📍 存放：${d.storageLocation}"
                setTextColor(getColor(R.color.mi_text_primary)); textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, (8 * dp).toInt(), 0, 0)
            })
        }

        // Related party
        if (d.relatedParty.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "🏢 ${d.relatedParty}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 13f
                setPadding(0, (4 * dp).toInt(), 0, 0)
            })
        }

        // Issue / Expire dates
        val dates = mutableListOf<String>()
        if (d.issueDate > 0) dates.add("签发：${dateFormat.format(Date(d.issueDate))}")
        if (d.expireDate > 0) dates.add("到期：${dateFormat.format(Date(d.expireDate))}")
        if (dates.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "📅 ${dates.joinToString("  |  ")}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
                setPadding(0, (4 * dp).toInt(), 0, 0)
            })
        }

        if (d.note.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "📝 ${d.note}"
                setTextColor(getColor(R.color.mi_text_hint)); textSize = 12f
                setPadding(0, (4 * dp).toInt(), 0, 0)
            })
        }

        card.setOnClickListener {
            startActivity(Intent(this, AddDocumentActivity::class.java).putExtra("document_id", d.id))
        }
        card.setOnLongClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this).setTitle("删除证件").setMessage("确定删除「${d.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch { AppDatabase.getInstance(this@DocumentListActivity).documentDao().delete(d) }
                    }.setNegativeButton("取消", null)
            )
            true
        }
        container.addView(card)
    }
}
