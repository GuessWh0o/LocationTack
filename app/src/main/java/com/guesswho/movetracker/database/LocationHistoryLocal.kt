package com.guesswho.movetracker.database


import androidx.annotation.Nullable
import androidx.room.*

@Entity
data class LocationHistory(
    @ColumnInfo(name = "lat")
    @Nullable
    var lat: Double = 0.0,

    @ColumnInfo(name = "long")
    @Nullable
    var longitude: Double = 0.0,

    @ColumnInfo(name = "speed")
    @Nullable
    var speed: Float? = 0F,

    @ColumnInfo(name = "accuracy")
    @Nullable
    var accuracy: Float? = 0F,

    @ColumnInfo(name = "timeStamp")
    @Nullable
    var time: Long? = 0,

    @ColumnInfo(name = "session_id")
    @Nullable
    var session: String? = ""
) {
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @ColumnInfo(name = "isUpdated")
    @Nullable
    var isUpdated: Boolean? = false
}

@Dao
interface LocationHistoryDoa : BaseDao<LocationHistory> {

    @Query("SELECT * FROM locationhistory")
    fun getAll(): List<LocationHistory>

    @Query(
        "SELECT * FROM locationhistory where isUpdated=:status "
    )
    fun getPendingSyncedLocation(status: Boolean = false): List<LocationHistory>

    @Query("DELETE FROM locationhistory")
    fun deleteAll()

    @Query("select * from locationhistory where session_id=:sessionId")
    fun getLocationForSession(sessionId: String): List<LocationHistory>


}


