package com.construction.diary.cloud.ui.expense

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Expense
import com.construction.diary.cloud.util.DateUtils
import com.construction.diary.cloud.util.DialogUtils
import com.construction.diary.cloud.util.MoneyUtils
import com.construction.diary.cloud.util.PhaseUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chipContainer: android.widget.LinearLayout
    private var currentPhase: String? = null
    private lateinit var adapter: ExpenseAdapter
    private lateinit var tvEmpty: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        recyclerView = findViewById(R.id.recyclerView)
        chipContainer = findViewById(R.id.chipContainer)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        adapter = ExpenseAdapter(
            onClick = { expense ->
                val intent = Intent(this, AddExpenseActivity::class.java)
                intent.putExtra("expense_id", expense.id)
                startActivity(intent)
            },
            onLongClick = { expense ->
                DialogUtils.showRoundedCompat(
                    AlertDialog.Builder(this)
                        .setTitle("删除确认")
                        .setMessage("确定删除「${expense.itemName}」？")
                        .setPositiveButton("删除") { _, _ ->
                            lifecycleScope.launch {
                                AppDatabase.getInstance(this@ExpenseListActivity)
                                    .expenseDao().delete(expense)
                            }
                        }
                        .setNegativeButton("取消", null)
                )
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupChips()
        loadData()
    }

    private fun setupChips() {
        val allPhases = listOf("全部") + PhaseUtils.PHASES
        chipContainer.removeAllViews()
        allPhases.forEach { phase ->
            val chip = TextView(this).apply {
                text = phase
                textSize = 13f
                setPadding(dp(16), dp(8), dp(16), dp(8))
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = dp(8)
                layoutParams = lp
                setChipStyle(phase == "全部")
                setOnClickListener {
                    currentPhase = if (phase == "全部") null else phase
                    updateChipSelection()
                    loadData()
                }
            }
            chipContainer.addView(chip)
        }
    }

    private fun updateChipSelection() {
        for (i in 0 until chipContainer.childCount) {
            val chip = chipContainer.getChildAt(i) as TextView
            val phaseText = chip.text.toString()
            val isSelected = (currentPhase == null && phaseText == "全部") ||
                    (currentPhase == phaseText)
            chip.setChipStyle(isSelected)
        }
    }

    private fun TextView.setChipStyle(selected: Boolean) {
        setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
        setTextColor(
            if (selected) resources.getColor(R.color.white, theme)
            else resources.getColor(R.color.mi_text_primary, theme)
        )
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@ExpenseListActivity)
            val flow = if (currentPhase != null) {
                db.expenseDao().getByPhase(currentPhase!!)
            } else {
                db.expenseDao().getAll()
            }
            flow.collectLatest { list ->
                adapter.submitList(list)
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    class ExpenseAdapter(
        private val onClick: (Expense) -> Unit,
        private val onLongClick: (Expense) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.VH>() {

        private var items = listOf<Expense>()

        fun submitList(list: List<Expense>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expense, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvItemName.text = item.itemName
            holder.tvAmount.text = MoneyUtils.format(item.amount)
            holder.tvBrand.text = if (item.brand.isNotEmpty()) item.brand else ""
            holder.tvBrand.visibility = if (item.brand.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            holder.tvBuyer.text = if (item.buyer.isNotEmpty()) "👤 ${item.buyer}" else ""
            holder.tvBuyer.visibility = if (item.buyer.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            holder.tvDate.text = DateUtils.formatDate(item.date)
            holder.tvPhase.text = if (item.phase.isNotEmpty()) "${PhaseUtils.emoji(item.phase)} ${item.phase}" else ""
            holder.tvPhase.visibility = if (item.phase.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener { onLongClick(item); true }
        }

        override fun getItemCount() = items.size

        class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val tvItemName: TextView = view.findViewById(R.id.tvItemName)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
            val tvBrand: TextView = view.findViewById(R.id.tvBrand)
            val tvBuyer: TextView = view.findViewById(R.id.tvBuyer)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvPhase: TextView = view.findViewById(R.id.tvPhase)
        }
    }
}
