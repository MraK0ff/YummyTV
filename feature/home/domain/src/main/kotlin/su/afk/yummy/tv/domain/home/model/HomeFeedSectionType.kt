package su.afk.yummy.tv.domain.home.model

enum class HomeFeedSectionType {
    /** Hero carousel populated from the current anime season. */
    SEASON_ANIME,
    /** Continue-watching row populated from local and remote progress. */
    CONTINUE_WATCHING,
    /** Недавно вышедшие серии тайтлов из списков пользователя; собирается локально из расписания. */
    MY_NEW_EPISODES,
    /** Свежие серии (`/feed` new_videos). */
    NEW_RELEASES,
    /** Новые тайтлы (`/feed` new). */
    NEW_TITLES,
    RECOMMENDATIONS,
    /** Анонсы, которые скоро выйдут (`/feed` announcements). */
    ANNOUNCEMENTS,
    COLLECTIONS,
    BLOGGER_VIDEOS,
    MORE,
}
