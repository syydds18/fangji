package com.construction.diary.cloud.ui.material

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Material
import com.construction.diary.cloud.util.DialogUtils
import com.construction.diary.cloud.util.MoneyUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog

class MaterialListActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tvBudget: TextView
    private lateinit var tvActual: TextView
    private var currentFilter = -1 // -1=all, 0=pending, 1=bought, 2=received

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_material_list)

        findViewById<TextView>(R.id.tvTitle).text = "📋 材料清单"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvBudget = findViewById(R.id.tvBudgetTotal)
        tvActual = findViewById(R.id.tvActualTotal)

        // Filter chips
        setupFilterChips()

        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddMaterialActivity::class.java))
        }
    }

    private fun setupFilterChips() {
        val chips = listOf(
            R.id.chipAll to -1,
            R.id.chipPending to 0,
            R.id.chipBought to 1,
            R.id.chipReceived to 2
        )
        chips.forEach { (id, filter) ->
            findViewById<View>(id).setOnClickListener {
                currentFilter = filter
                updateChipStates()
                loadMaterials()
            }
        }
    }

    private fun updateChipStates() {
        val chips = mapOf(
            R.id.chipAll to -1, R.id.chipPending to 0,
            R.id.chipBought to 1, R.id.chipReceived to 2
        )
        chips.forEach { (id, filter) ->
            val chip = findViewById<TextView>(id)
            if (filter == currentFilter) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(getColor(R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip)
                chip.setTextColor(getColor(R.color.mi_text_primary))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateChipStates()
        loadMaterials()
        loadSummary()
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MaterialListActivity)
            val budget = db.materialDao().getTotalBudget() ?: 0.0
            val actual = db.materialDao().getTotalActual() ?: 0.0
            tvBudget.text = "预算: ${MoneyUtils.format(budget)}"
            tvActual.text = "实际: ${MoneyUtils.format(actual)}"
        }
    }

    private fun loadMaterials() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MaterialListActivity)
            val flow = if (currentFilter == -1) db.materialDao().getAll()
            else db.materialDao().getByStatus(currentFilter)
            flow.collectLatest { materials ->
                container.removeAllViews()
                tvEmpty.visibility = if (materials.isEmpty()) View.VISIBLE else View.GONE
                materials.forEach { addMaterialRow(it) }
            }
        }
    }

    private fun addMaterialRow(m: Material) {
        val dp = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.card_bg)
            setPadding((20*dp).toInt(), (16*dp).toInt(), (20*dp).toInt(), (16*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8*dp).toInt() }
        }

        val statusText = when(m.status) { 0->"🟡 待购"; 1->"🔵 已购"; 2->"🟢 已到货"; else->"" }
        val statusColor = when(m.status) { 0->getColor(R.color.mi_warning); 1->getColor(R.color.mi_blue); 2->getColor(R.color.mi_success); else->getColor(R.color.mi_text_secondary) }

        val tvName = TextView(this).apply {
            text = "${m.name}  ${m.spec}"
            setTextColor(getColor(R.color.mi_text_primary))
            textSize = 15f
        }
        val tvInfo = TextView(this).apply {
            text = "$statusText  数量: ${m.quantity}  预算: ${MoneyUtils.format(m.budgetAmount)}  实际: ${MoneyUtils.format(m.actualAmount)}"
            setTextColor(statusColor)
            textSize = 12f
            setPadding(0, (4*dp).toInt(), 0, 0)
        }

        row.addView(tvName)
        row.addView(tvInfo)

        row.setOnClickListener {
            val intent = Intent(this, AddMaterialActivity::class.java)
            intent.putExtra("material_id", m.id)
            startActivity(intent)
        }
        row.setOnLongClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this)
                    .setTitle("删除材料")
                    .setMessage("确定删除「${m.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            AppDatabase.getInstance(this@MaterialListActivity).materialDao().delete(m)
                        }
                    }
                    .setNegativeButton("取消", null)
            )
            true
        }
        container.addView(row)
    }
}
