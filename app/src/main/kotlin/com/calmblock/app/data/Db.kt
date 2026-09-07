package com.calmblock.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mode: String,
    val builtIn: Boolean = false,
    val confirmedAt: Long? = null,
    val timedEndAt: Long? = null,
    val days: Int = 0,
    val startMinute: Int = 21 * 60,
    val endMinute: Int = 7 * 60,
    val freezeMinutes: Int = 0,
    val enabled: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "items", primaryKeys = ["blockId", "kind", "value"])
data class ItemEntity(
    val blockId: Long,
    val kind: String,
    val value: String,
    val label: String,
    val addedAt: Long,
) {
    companion object {
        const val APP = "app"
        const val DOMAIN = "domain"
    }
}

@Entity(tableName = "blocked_events")
data class BlockedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val label: String,
    val kind: String,
    val at: Long,
)

@Entity(tableName = "usage_days", primaryKeys = ["date", "pkg"])
data class UsageDayEntity(
    val date: String,
    val pkg: String,
    val label: String,
    val minutes: Int,
)

@Dao
interface BlockDao {
    @Query("SELECT * FROM blocks ORDER BY builtIn DESC, createdAt ASC")
    fun blocks(): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks ORDER BY builtIn DESC, createdAt ASC")
    suspend fun blocksNow(): List<BlockEntity>

    @Query("SELECT * FROM blocks WHERE id = :id")
    fun block(id: Long): Flow<BlockEntity?>

    @Query("SELECT * FROM blocks WHERE id = :id")
    suspend fun blockNow(id: Long): BlockEntity?

    @Insert
    suspend fun insert(block: BlockEntity): Long

    @Update
    suspend fun update(block: BlockEntity)

    @Query("DELETE FROM blocks WHERE id = :id AND builtIn = 0 AND confirmedAt IS NULL")
    suspend fun deleteUnconfirmed(id: Long)

    @Query("SELECT * FROM items WHERE blockId = :blockId ORDER BY kind, label")
    fun items(blockId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items")
    suspend fun allItemsNow(): List<ItemEntity>

    @Query("SELECT * FROM items")
    fun allItems(): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE blockId = :blockId AND kind = :kind AND value = :value")
    suspend fun deleteItem(blockId: Long, kind: String, value: String)

    @Query("DELETE FROM items WHERE blockId = :blockId")
    suspend fun deleteItemsOf(blockId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(rows: List<UsageDayEntity>)

    @Query("SELECT * FROM usage_days WHERE date >= :from AND date <= :to ORDER BY date, minutes DESC")
    fun usageBetween(from: String, to: String): Flow<List<UsageDayEntity>>

    @Query("SELECT * FROM usage_days ORDER BY date")
    suspend fun allUsageNow(): List<UsageDayEntity>

    @Insert
    suspend fun insertEvent(event: BlockedEventEntity)

    @Query("SELECT * FROM blocked_events WHERE date >= :from AND date <= :to ORDER BY at")
    fun eventsBetween(from: String, to: String): Flow<List<BlockedEventEntity>>
}

@Database(entities = [BlockEntity::class, ItemEntity::class, UsageDayEntity::class, BlockedEventEntity::class], version = 3, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): BlockDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "calmblock.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }

        /** Freeze window went from whole hours to minutes. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE blocks_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, mode TEXT NOT NULL, " +
                        "builtIn INTEGER NOT NULL, confirmedAt INTEGER, timedEndAt INTEGER, days INTEGER NOT NULL, startMinute INTEGER NOT NULL, " +
                        "endMinute INTEGER NOT NULL, freezeMinutes INTEGER NOT NULL, createdAt INTEGER NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO blocks_new SELECT id, name, mode, builtIn, confirmedAt, timedEndAt, days, startMinute, endMinute, freezeHours * 60, createdAt FROM blocks",
                )
                db.execSQL("DROP TABLE blocks")
                db.execSQL("ALTER TABLE blocks_new RENAME TO blocks")
            }
        }

        /** Schedules must be turned on explicitly; existing ones stay on. Blocked attempts get a table. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocks ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "CREATE TABLE blocked_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, " +
                        "label TEXT NOT NULL, kind TEXT NOT NULL, at INTEGER NOT NULL)",
                )
            }
        }
    }
}
