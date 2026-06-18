package com.construction.diary.cloud.ui.funding

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
import com.construction.diary.cloud.data.entity.FundingSource
import com.construction.diary.cloud.util.DateUtils
import com.construction.diary.cloud.util.DialogUtils
import com.construction.diary.cloud.util.MoneyUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FundingSourceListActivity : AppCompatActivity() {

    private lateinit var adapter: FundingSourceAdapter
    private lateinit var tvEmpty: android.widget.TextView
    private lateinit var tvTotalAmount: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_funding_source_list)

        tvEmpty = findViewById(R.id.tvEmpty)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddFundingSourceActivity::class.java))
        }

        adapter = FundingSourceAdapter(
            onClick = { item ->
                val intent = Intent(this, AddFundingSourceActivity::class.java)
                intent.putExtra("funding_id", item.id)
                startActivity(intent)
            },
            onLongClick = { item ->
                DialogUtils.showRoundedCompat(
                    AlertDialog.Builder(this)
                        .setTitle("删除确认")
                        .setMessage("确定删除「${item.contributorName}」的出资记录？")
                        .setPositiveButton("删除") { _, _ ->
                            lifecycleScope.launch {
                                AppDatabase.getInstance(this@FundingSourceListActivity)
                                    .fundingSourceDao().delete(item)
                            }
                        }
                        .setNegativeButton("取消", null)
                )
            }
        )
        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        lifecycleScope.launch {
            AppDatabase.getInstance(this@FundingSourceListActivity)
                .fundingSourceDao().getAll().collectLatest { list ->
                    adapter.submitList(list)
                    tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
        }

        lifecycleScope.launch {
            val total = AppDatabase.getInstance(this@FundingSourceListActivity)
                .fundingSourceDao().getTotalAmount() ?: 0.0
            tvTotalAmount.text = MoneyUtils.format(total)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val total = AppDatabase.getInstance(this@FundingSourceListActivity)
                .fundingSourceDao().getTotalAmount() ?: 0.0
            tvTotalAmount.text = MoneyUtils.format(total)
        }
    }

    class FundingSourceAdapter(
        private val onClick: (FundingSource) -> Unit,
        private val onLongClick: (FundingSource) -> Unit
    ) : RecyclerView.Adapter<FundingSourceAdapter.VH>() {

        private var items = listOf<FundingSource>()
        fun submitList(list: List<FundingSource>) { items = list; notifyDataSetChanged() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_funding_source, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvContributorName.text = item.contributorName
            holder.tvAmount.text = "+${MoneyUtils.format(item.amount)}"
            holder.tvRelationship.text = item.relationship
            holder.tvRelationship.visibility = if (item.relationship.isNotEmpty()) View.VISIBLE else View.GONE
            holder.tvNote.text = item.note
            holder.tvNote.visibility = if (item.note.isNotEmpty()) View.VISIBLE else View.GONE
            holder.tvDate.text = DateUtils.formatDate(item.date)
            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener { onLongClick(item); true }
        }

        override fun getItemCount() = items.size

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvContributorName: TextView = view.findViewById(R.id.tvContributorName)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
            val tvRelationship: TextView = view.findViewById(R.id.tvRelationship)
            val tvNote: TextView = view.findViewById(R.id.tvNote)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
        }
    }
}
