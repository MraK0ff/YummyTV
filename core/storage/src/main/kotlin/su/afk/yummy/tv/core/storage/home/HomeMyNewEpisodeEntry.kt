package su.afk.yummy.tv.core.storage.home

import androidx.room.Entity
import androidx.room.Index

/** Persistent snapshot of the newest known aired episode for a library anime. */
@Entity(
    tableName = "home_my_new_episode_history",
    primaryKeys = ["animeId"],
    indices = [Index(value = ["position"], name = "index_home_my_new_episode_history_position")],
)
data class HomeMyNewEpisodeEntry(
    val animeId: Int,
    val title: String,
    val description: String,
    val episodeNumber: Int,
    val airedAtSeconds: Long,
    val posterSmallUrl: String?,
    val posterMediumUrl: String?,
    val posterBigUrl: String?,
    val posterFullsizeUrl: String?,
    val posterMegaUrl: String?,
    val firstSeenAt: Long,
    /** Zero marks the initial baseline; positive values mark real episode advances. */
    val lastUpdatedAt: Long,
    val position: Int,
)
