package com.construction.diary.cloud.ui.worker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Worker
import com.construction.diary.cloud.util.DialogUtils
import com.construction.diary.cloud.util.MoneyUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkerListActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_list)
        findViewById<TextView>(R.id.tvTitle).text = "👷 工人管理"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddWorkerActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            AppDatabase.getInstance(this@WorkerListActivity).workerDao().getAll().collectLatest { workers ->
                container.removeAllViews()
                tvEmpty.visibility = if (workers.isEmpty()) View.VISIBLE else View.GONE
                workers.forEach { addRow(it) }
            }
        }
    }

    private fun addRow(w: Worker) {
        val dp = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.card_bg)
            setPadding((20*dp).toInt(), (16*dp).toInt(), (20*dp).toInt(), (16*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8*dp).toInt() }
        }
        val tv1 = TextView(this).apply {
            text = "${w.name}  ${w.role}"
            setTextColor(getColor(R.color.mi_text_primary)); textSize = 15f
        }
        val tv2 = TextView(this).apply {
            text = "📱 ${w.phone.ifEmpty { "未记录" }}  💰 日薪: ${MoneyUtils.format(w.dailyWage)}"
            setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
            setPadding(0, (4*dp).toInt(), 0, 0)
        }
        row.addView(tv1); row.addView(tv2)
        row.setOnClickListener {
            startActivity(Intent(this, AddWorkerActivity::class.java).putExtra("worker_id", w.id))
        }
        row.setOnLongClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this).setTitle("删除工人").setMessage("确定删除「${w.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch { AppDatabase.getInstance(this@WorkerListActivity).workerDao().delete(w) }
                    }.setNegativeButton("取消", null)
            ); true
        }
        container.addView(row)
    }
}
