package com.construction.diary.cloud.ui.gift

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
import com.construction.diary.cloud.data.entity.GiftMoney
import com.construction.diary.cloud.util.DateUtils
import com.construction.diary.cloud.util.DialogUtils
import com.construction.diary.cloud.util.MoneyUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GiftListActivity : AppCompatActivity() {

    private lateinit var adapter: GiftAdapter
    private lateinit var tvEmpty: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gift_list)

        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddGiftActivity::class.java))
        }

        adapter = GiftAdapter(
            onClick = { gift ->
                val intent = Intent(this, AddGiftActivity::class.java)
                intent.putExtra("gift_id", gift.id)
                startActivity(intent)
            },
            onLongClick = { gift ->
                DialogUtils.showRoundedCompat(
                    AlertDialog.Builder(this)
                        .setTitle("删除确认")
                        .setMessage("确定删除「${gift.giverName}」的礼金记录？")
                        .setPositiveButton("删除") { _, _ ->
                            lifecycleScope.launch {
                                AppDatabase.getInstance(this@GiftListActivity)
                                    .giftMoneyDao().delete(gift)
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
            AppDatabase.getInstance(this@GiftListActivity)
                .giftMoneyDao().getAll().collectLatest { list ->
                    adapter.submitList(list)
                    tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
        }
    }

    class GiftAdapter(
        private val onClick: (GiftMoney) -> Unit,
        private val onLongClick: (GiftMoney) -> Unit
    ) : RecyclerView.Adapter<GiftAdapter.VH>() {

        private var items = listOf<GiftMoney>()
        fun submitList(list: List<GiftMoney>) { items = list; notifyDataSetChanged() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gift, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvGiverName.text = item.giverName
            holder.tvAmount.text = "+${MoneyUtils.format(item.amount)}"
            holder.tvRelationship.text = item.relationship
            holder.tvRelationship.visibility = if (item.relationship.isNotEmpty()) View.VISIBLE else View.GONE
            holder.tvOccasion.text = item.occasion
            holder.tvOccasion.visibility = if (item.occasion.isNotEmpty()) View.VISIBLE else View.GONE
            holder.tvDate.text = DateUtils.formatDate(item.date)
            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener { onLongClick(item); true }
        }

        override fun getItemCount() = items.size

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvGiverName: TextView = view.findViewById(R.id.tvGiverName)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
            val tvRelationship: TextView = view.findViewById(R.id.tvRelationship)
            val tvOccasion: TextView = view.findViewById(R.id.tvOccasion)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
        }
    }
}
