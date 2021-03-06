package net.mafro.android.wakeonlan

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomDatabase
import androidx.room.Update
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
    var createdDate : Long = 0

    @ColumnInfo(name = History.Items.LAST_USED_DATE)
    var lastUsedDate : Long = 0

    @ColumnInfo(name = History.Items.USED_COUNT)
    var usedCount : Int = 0

    @ColumnInfo(name = History.Items.IS_STARRED)
    var starred : Int = 0
}

@Dao
interface HistoryDao {
    @RawQuery(observedEntities = [HistoryIt::class])
    fun histItemList(query : SupportSQLiteQuery) : LiveData<List<HistoryIt>>

    @RawQuery
    fun doQuery(query : SupportSQLiteQuery) : Cursor

    @Query("select * from ${HistoryProvider.HISTORY_TABLE_NAME} where ${History.Items._ID} = :id")
    fun historyItem(id : Int) : HistoryIt

    @Query("select * from ${HistoryProvider.HISTORY_TABLE_NAME} where ${History.Items._ID} = :id")
    fun getHistoryItem(id : Int) : HistoryIt?

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

internal class MigrationFrom1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE history ADD COLUMN ${History.Items.USED_COUNT} INTEGER DEFAULT 1;")
        db.execSQL("ALTER TABLE history ADD COLUMN ${History.Items.IS_STARRED} INTEGER DEFAULT 0;")
    }
}

internal class MigrationFrom2To3 : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
    }
}