package net.mafro.android.wakeonlan

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery

@Entity(tableName = HistoryProvider.HISTORY_TABLE_NAME)
class HistoryIt {
    //TODO: Test update scenario from old version of app to new version
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = History.Items._ID)
    var id : Int = 0

    @ColumnInfo(name = History.Items.TITLE)
    var title : String = ""

    @ColumnInfo(name = History.Items.MAC)
    lateinit var mac : String

    @ColumnInfo(name = History.Items.IP)
    lateinit var ip : String

    @ColumnInfo(name = History.Items.PORT)
    var port : Int = MagicPacket.PORT

    @ColumnInfo(name = History.Items.CREATED_DATE)
    var createdDate : Int = 0

    @ColumnInfo(name = History.Items.LAST_USED_DATE)
    var lastUsedDate : Long = 0

    @ColumnInfo(name = History.Items.USED_COUNT)
    var usedCount : Int = 1

    @ColumnInfo(name = History.Items.IS_STARRED)
    var starred : Int = 0
}

@Dao
interface HistoryDao {
    @Query("select * from ${HistoryProvider.HISTORY_TABLE_NAME} order by :sortModeColname")
    fun getHistoryList(sortModeColname : String) : LiveData<List<HistoryIt>>

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