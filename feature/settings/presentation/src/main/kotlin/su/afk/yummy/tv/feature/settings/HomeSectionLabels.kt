package su.afk.yummy.tv.feature.settings

import su.afk.yummy.tv.feature.settings.presentation.R

/** Localized label resource for each section registered in [HomeSectionKind]. */
fun HomeSectionKind.labelRes(): Int = when (this) {
    HomeSectionKind.SEASON_ANIME -> R.string.settings_home_section_season_anime
    HomeSectionKind.CONTINUE_WATCHING -> R.string.settings_home_section_continue_watching
    HomeSectionKind.MY_NEW_EPISODES -> R.string.settings_home_section_my_new_episodes
    HomeSectionKind.NEW_RELEASES -> R.string.settings_home_section_new_releases
    HomeSectionKind.NEW_TITLES -> R.string.settings_home_section_new_titles
    HomeSectionKind.RECOMMENDATIONS -> R.string.settings_home_section_recommendations
    HomeSectionKind.ANNOUNCEMENTS -> R.string.settings_home_section_announcements
    HomeSectionKind.COLLECTIONS -> R.string.settings_home_section_collections
    HomeSectionKind.BLOGGER_VIDEOS -> R.string.settings_home_section_blogger_videos
    HomeSectionKind.MORE -> R.string.settings_home_section_more
}
