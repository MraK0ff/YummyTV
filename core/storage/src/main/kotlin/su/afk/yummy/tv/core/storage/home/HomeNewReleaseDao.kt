package su.afk.yummy.tv.core.storage.home

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class HomeNewReleaseDao {
    @Query("SELECT * FROM home_new_release_history ORDER BY position")
    abstract suspend fun getAll(): List<HomeNewReleaseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entries: List<HomeNewReleaseEntry>)
}
