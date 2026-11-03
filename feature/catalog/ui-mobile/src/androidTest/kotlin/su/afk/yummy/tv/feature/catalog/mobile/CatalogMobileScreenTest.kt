package su.afk.yummy.tv.feature.catalog.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.feature.catalog.SeasonalCatalogState
import su.afk.yummy.tv.feature.home.presentation.R

class CatalogMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun seasonalTitleIsShownWhenFeedIsLoaded() {
        composeRule.setContent {
            ScreenPreviewTheme {
                CatalogMobileScreen(
                    state = SeasonalCatalogState.State(
                        feed = HomeFeed(heroItems = emptyList(), sections = emptyList()),
                        isLoading = false,
                    ),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.home_season_title)).assertIsDisplayed()
    }

    @Test
    fun retryActionIsShownWhenFeedFailsToLoad() {
        composeRule.setContent {
            ScreenPreviewTheme {
                CatalogMobileScreen(
                    state = SeasonalCatalogState.State(isLoading = false),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.home_load_error)).assertIsDisplayed()
    }
}
