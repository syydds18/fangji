package com.construction.diary.cloud.ui.supplier

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.PhotoViewActivity
import com.construction.diary.cloud.R
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.data.entity.Supplier
import com.construction.diary.cloud.util.DialogUtils
import com.construction.diary.cloud.util.ImageUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SupplierListActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier_list)
        findViewById<TextView>(R.id.tvTitle).text = "🏪 供应商管理"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddSupplierActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            AppDatabase.getInstance(this@SupplierListActivity).supplierDao().getAll().collectLatest { list ->
                container.removeAllViews()
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                list.forEach { addRow(it) }
            }
        }
    }

    private fun addRow(s: Supplier) {
        val dp = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.card_bg)
            setPadding((20*dp).toInt(), (16*dp).toInt(), (20*dp).toInt(), (16*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8*dp).toInt() }
        }

        // Header row: name + category tag
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = s.name; textSize = 16f
            setTextColor(getColor(R.color.mi_text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (s.category.isNotEmpty()) {
            header.addView(TextView(this).apply {
                text = s.category; textSize = 11f
                setTextColor(getColor(R.color.mi_orange))
                background = getDrawable(R.drawable.bg_chip)
                setPadding((10*dp).toInt(), (4*dp).toInt(), (10*dp).toInt(), (4*dp).toInt())
            })
        }
        card.addView(header)

        // Contact info
        if (s.contact.isNotEmpty() || s.phone.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "👤 ${s.contact.ifEmpty { "—" }}  📱 ${s.phone.ifEmpty { "—" }}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 13f
                setPadding(0, (6*dp).toInt(), 0, 0)
            })
        }
        if (s.address.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "📍 ${s.address}"
                setTextColor(getColor(R.color.mi_text_secondary)); textSize = 12f
                setPadding(0, (4*dp).toInt(), 0, 0)
            })
        }

        // QR Code thumbnail
        if (s.qrCodePath.isNotEmpty()) {
            val file = ImageUtils.getImageFile(this, s.qrCodePath)
            if (file.exists()) {
                val qrRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, (8*dp).toInt(), 0, 0)
                }
                val ivQr = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams((60*dp).toInt(), (60*dp).toInt())
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                }
                qrRow.addView(ivQr)
                qrRow.addView(TextView(this).apply {
                    text = "  💳 点击查看收款码大图"
                    setTextColor(getColor(R.color.mi_blue)); textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                qrRow.setOnClickListener {
                    startActivity(Intent(this@SupplierListActivity, PhotoViewActivity::class.java)
                        .putExtra("image_file", s.qrCodePath))
                }
                card.addView(qrRow)
            }
        }

        if (s.note.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "📝 ${s.note}"
                setTextColor(getColor(R.color.mi_text_hint)); textSize = 12f
                setPadding(0, (4*dp).toInt(), 0, 0)
            })
        }

        // Call button
        if (s.phone.isNotEmpty()) {
            val callBtn = TextView(this).apply {
                text = "📞 拨打电话"; textSize = 13f
                setTextColor(getColor(R.color.mi_success))
                setPadding(0, (8*dp).toInt(), 0, 0)
            }
            callBtn.setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${s.phone}")))
            }
            card.addView(callBtn)
        }

        card.setOnClickListener {
            startActivity(Intent(this, AddSupplierActivity::class.java).putExtra("supplier_id", s.id))
        }
        card.setOnLongClickListener {
            DialogUtils.showRoundedCompat(
                AlertDialog.Builder(this).setTitle("删除供应商").setMessage("确定删除「${s.name}」？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch { AppDatabase.getInstance(this@SupplierListActivity).supplierDao().delete(s) }
                    }.setNegativeButton("取消", null)
            )
            true
        }
        container.addView(card)
    }
}
