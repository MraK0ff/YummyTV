package su.afk.yummy.tv.core.storage.home

import androidx.room.Entity
import androidx.room.Index

/** Persistent, anime-scoped snapshot of the newest known regular episode from /feed. */
@Entity(
    tableName = "home_new_release_history",
    primaryKeys = ["animeId"],
    indices = [Index(value = ["position"], name = "index_home_new_release_history_position")],
)
data class HomeNewReleaseEntry(
    val animeId: Int,
    val title: String,
    val animeUrl: String,
    val description: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val videoId: Int?,
    val dubTitle: String?,
    val playerTitle: String?,
    val posterSmallUrl: String?,
    val posterMediumUrl: String?,
    val posterBigUrl: String?,
    val posterFullsizeUrl: String?,
    val posterMegaUrl: String?,
    val firstSeenAt: Long,
    val lastDetectedAt: Long,
    /** Zero means that the entry was part of the initial baseline, not a new release. */
    val lastUpdatedAt: Long,
    val position: Int,
)
