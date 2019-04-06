package net.mafro.android.wakeonlan

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery

@Entity(tableName = HistoryProvider.HISTORY_TABLE_NAME)
class HistoryIt {
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id : Int = 0

    @ColumnInfo(name = "title")
    var title : String = ""

    @ColumnInfo(name = "mac")
    lateinit var mac : String

    @ColumnInfo(name = "ip")
    lateinit var ip : String

    @ColumnInfo(name = "port")
    var port : Int = MagicPacket.PORT

    @ColumnInfo(name = "created")
    var createdDate : Int = 0

    @ColumnInfo(name = "last_used")
    var lastUsedDate : Long = 0

    @ColumnInfo(name = "used_count")
    var usedCount : Int = 1

    @ColumnInfo(name = "is_starred")
    var starred : Int = 0
}

@Dao
interface HistoryDao {
    @Query("select * from ${HistoryProvider.HISTORY_TABLE_NAME}")
    fun getHistoryList() : LiveData<List<HistoryIt>>

    @RawQuery
    fun doQuery(query : SupportSQLiteQuery) : Cursor

    @Query("select * from ${HistoryProvider.HISTORY_TABLE_NAME} where ${History.Items._ID} = :id")
    fun historyItem(id : Long) : HistoryIt

    @Update
    fun updateItem(historyItem : HistoryIt)

    @Delete
    fun deleteItem(historyItem: HistoryIt)

    @Query("select count(*) from ${HistoryProvider.HISTORY_TABLE_NAME} where ${History.Items.MAC}=:mac and ${History.Items.IP}=:ip and ${History.Items.PORT}=:port")
    fun getNumRows(mac : String, ip : String, port : Int) : Int

    @Insert
    fun addNewItem(item : HistoryIt)
}

@Database(entities = [HistoryIt::class], version = HistoryProvider.DATABASE_VERSION)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao() : HistoryDao
}

internal class Migration_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE history ADD COLUMN ${History.Items.USED_COUNT} INTEGER DEFAULT 1;")
        db.execSQL("ALTER TABLE history ADD COLUMN ${History.Items.IS_STARRED} INTEGER DEFAULT 0;")
    }
}

internal class Migration_2_3 : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
    }
}