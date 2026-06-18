package com.construction.diary.cloud
import com.construction.diary.cloud.R
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.construction.diary.cloud.data.AppDatabase
import com.construction.diary.cloud.ui.dailylog.DailyLogListActivity
import com.construction.diary.cloud.ui.expense.ExpenseListActivity
import com.construction.diary.cloud.ui.stats.StatisticsActivity
import com.construction.diary.cloud.ui.supplier.SupplierListActivity
import com.construction.diary.cloud.ui.appliance.ApplianceListActivity
import com.construction.diary.cloud.ui.document.DocumentListActivity
import com.construction.diary.cloud.ui.maintenance.MaintenanceListActivity
import com.construction.diary.cloud.ui.floorplan.FloorPlanActivity
import com.construction.diary.cloud.ui.gift.GiftListActivity
import com.construction.diary.cloud.ui.funding.FundingSourceListActivity
import com.construction.diary.cloud.ui.material.MaterialListActivity
import com.construction.diary.cloud.ui.milestone.MilestoneListActivity
import com.construction.diary.cloud.ui.photo.PhotoGalleryActivity
import com.construction.diary.cloud.ui.settings.SettingsActivity
import com.construction.diary.cloud.ui.worker.WorkerListActivity
import com.construction.diary.cloud.util.MoneyUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvProjectAddress: TextView
    private lateinit var tvProjectDate: TextView
    private lateinit var tvBudget: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvGift: TextView
    private lateinit var tvBalance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvProjectAddress = findViewById(R.id.tvProjectAddress)
        tvProjectDate = findViewById(R.id.tvProjectDate)
        tvBudget = findViewById(R.id.tvBudget)
        tvExpense = findViewById(R.id.tvExpense)
        tvGift = findViewById(R.id.tvGift)
        tvBalance = findViewById(R.id.tvBalance)

        // Click listeners
        findViewById<android.view.View>(R.id.cardProjectInfo).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionExpense).setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionGift).setOnClickListener {
            startActivity(Intent(this, GiftListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionFundingSource).setOnClickListener {
            startActivity(Intent(this, FundingSourceListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionMaterial).setOnClickListener {
            startActivity(Intent(this, MaterialListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionWorker).setOnClickListener {
            startActivity(Intent(this, WorkerListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionMilestone).setOnClickListener {
            startActivity(Intent(this, MilestoneListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionPhoto).setOnClickListener {
            startActivity(Intent(this, PhotoGalleryActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionFloorPlan).setOnClickListener {
            startActivity(Intent(this, FloorPlanActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionStats).setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionDailyLog).setOnClickListener {
            startActivity(Intent(this, DailyLogListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionSupplier).setOnClickListener {
            startActivity(Intent(this, SupplierListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionAppliance).setOnClickListener {
            startActivity(Intent(this, ApplianceListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionDocument).setOnClickListener {
            startActivity(Intent(this, DocumentListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.actionMaintenance).setOnClickListener {
            startActivity(Intent(this, MaintenanceListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)

            // Project info
            val address = db.projectConfigDao().getValue("address")
            val startDate = db.projectConfigDao().getValue("start_date")
            val budgetStr = db.projectConfigDao().getValue("budget")
            val budget = budgetStr?.toDoubleOrNull() ?: 0.0

            if (!address.isNullOrEmpty()) {
                tvProjectAddress.text = address
                tvProjectDate.text = if (!startDate.isNullOrEmpty()) startDate else "建房中"
            } else {
                tvProjectAddress.text = "点击设置建房信息"
                tvProjectDate.text = "设置 → 建房信息"
            }

            // Financial stats
            val totalExpense = db.expenseDao().getTotalAmount() ?: 0.0
            val totalGift = db.giftMoneyDao().getTotalAmount() ?: 0.0
            val balance = budget + totalGift - totalExpense

            tvBudget.text = MoneyUtils.format(budget)
            tvExpense.text = MoneyUtils.format(totalExpense)
            tvGift.text = MoneyUtils.format(totalGift)
            tvBalance.text = MoneyUtils.format(balance)
        }
    }
}
