package com.construction.diary.cloud.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.construction.diary.cloud.data.dao.*
import com.construction.diary.cloud.data.entity.*

@Database(
    entities = [
        Expense::class,
        GiftMoney::class,
        Material::class,
        Worker::class,
        Milestone::class,
        ProjectConfig::class,
        Supplier::class,
        DailyLog::class,
        ChecklistItem::class,
        HouseRoom::class,
        Appliance::class,
        Document::class,
        Maintenance::class,
        StandalonePhoto::class,
        FundingSource::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun giftMoneyDao(): GiftMoneyDao
    abstract fun materialDao(): MaterialDao
    abstract fun workerDao(): WorkerDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun projectConfigDao(): ProjectConfigDao
    abstract fun supplierDao(): SupplierDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun roomDao(): RoomDao
    abstract fun applianceDao(): ApplianceDao
    abstract fun documentDao(): DocumentDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun standalonePhotoDao(): StandalonePhotoDao
    abstract fun fundingSourceDao(): FundingSourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "construction_diary.db"
                ).fallbackToDestructiveMigration()  // 有云同步兜底，schema 变更时清空本地数据后可重新拉取
                .build().also { INSTANCE = it }
            }
        }
    }
}
