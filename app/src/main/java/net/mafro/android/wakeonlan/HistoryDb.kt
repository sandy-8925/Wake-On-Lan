package net.mafro.android.wakeonlan

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity
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
    var lastUsedDate : Int = 0

    @ColumnInfo(name = "used_count")
    var usedCount : Int = 1

    @ColumnInfo(name = "is_starred")
    var isStarred : Int = 0
}

@Dao
interface HistoryDao

@Database(entities = [HistoryIt::class], version = 2)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao() : HistoryDao
}