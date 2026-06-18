package com.construction.diary.cloud.ui.appliance

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Appliance
import com.construction.diary.cloud.util.DialogUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ApplianceListActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appliance_list)

        findViewById<TextView>(R.id.tvTitle).text = "📺 电器大件"
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
            .setOnClickListener {
                startActivity(Intent(this, AddApplianceActivity::class.java))
            }
    }

    override fun onResume() {
        super.onResume()
        val dao = AppDatabase.getInstance(this).applianceDao()
        lifecycleScope.launch {
            dao.getAll().collectLatest { list ->
                container.removeAllViews()
                if (list.isEmpty()) {
                    tvEmpty.visibility = android.view.View.VISIBLE
                } else {
                    tvEmpty.visibility = android.view.View.GONE
                    list.forEach { appliance -> addCard(appliance) }
                }
            }
        }
    }

    private fun addCard(appliance: Appliance) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_bg)
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        // Name + brand (bold)
        val titleText = buildString {
            append(appliance.name)
            if (appliance.brand.isNotBlank()) append(" · ${appliance.brand}")
        }
        val tvTitle = TextView(this).apply {
            text = titleText
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(R.color.mi_text_primary, null))
        }
        card.addView(tvTitle)

        // Category chip + purchase channel
        val rowInfo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 6, 0, 0)
        }
        if (appliance.category.isNotBlank()) {
            val chip = TextView(this).apply {
                text = appliance.category
                textSize = 11f
                setTextColor(resources.getColor(R.color.mi_orange, null))
                setBackgroundResource(R.drawable.bg_input_field)
                setPadding(12, 4, 12, 4)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = 8 }
            }
            rowInfo.addView(chip)
        }
        if (appliance.purchaseChannel.isNotBlank()) {
            val channelTv = TextView(this).apply {
                text = appliance.purchaseChannel
                textSize = 12f
                setTextColor(resources.getColor(R.color.mi_text_secondary, null))
            }
            rowInfo.addView(channelTv)
        }
        card.addView(rowInfo)

        // Price
        if (appliance.price > 0) {
            val tvPrice = TextView(this).apply {
                text = "¥${String.format("%.2f", appliance.price)}"
                textSize = 14f
                setTextColor(resources.getColor(R.color.mi_orange, null))
                setPadding(0, 6, 0, 0)
            }
            card.addView(tvPrice)
        }

        // Warranty info
        if (appliance.warrantyYears > 0) {
            val warrantyText = buildString {
                append("质保 ${appliance.warrantyYears} 年")
                if (appliance.warrantyExpireDate > 0) {
                    append(" · 到期 ${dateFormat.format(Date(appliance.warrantyExpireDate))}")
                }
            }
            val tvWarranty = TextView(this).apply {
                text = warrantyText
                textSize = 13f
                setTextColor(resources.getColor(R.color.mi_text_secondary, null))
                setPadding(0, 4, 0, 0)
            }
            card.addView(tvWarranty)
        }

        // After-sale phone
        if (appliance.afterSalePhone.isNotBlank()) {
            val tvPhone = TextView(this).apply {
                text = "📞 ${appliance.afterSalePhone}"
                textSize = 13f
                setTextColor(resources.getColor(R.color.mi_text_secondary, null))
                setPadding(0, 4, 0, 0)
            }
            card.addView(tvPhone)
        }

        card.setOnClickListener {
            val intent = Intent(this, AddApplianceActivity::class.java)
            intent.putExtra("appliance_id", appliance.id)
            startActivity(intent)
        }

        card.setOnLongClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this).setTitle("删除电器")
                    .setMessage("确定删除「${appliance.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            AppDatabase.getInstance(this@ApplianceListActivity).applianceDao().delete(appliance)
                        }
                    }.setNegativeButton("取消", null)
            )
            true
        }

        container.addView(card)
    }
}
