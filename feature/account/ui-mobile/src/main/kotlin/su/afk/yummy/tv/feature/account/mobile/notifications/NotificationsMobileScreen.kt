package su.afk.yummy.tv.feature.account.mobile.notifications

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileNotificationsTab
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
internal fun NotificationsMobileScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val openNotificationFailed = AccountUiError.OPEN_NOTIFICATION_FAILED.accountErrorMessage()

    LaunchedEffect(Unit) {
        onEvent(AccountState.Event.ScreenShown)
    }
    LaunchedEffect(state.hubError) {
        if (state.hubError == AccountUiError.OPEN_NOTIFICATION_FAILED && openNotificationFailed != null) {
            Toast.makeText(context, openNotificationFailed, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(effect) {
        effect.collect { item ->
            if (item == AccountState.Effect.HideKeyboard) {
                // No keyboard is shown on this screen; consume the shared account effect.
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MobileTopBar(
            title = stringResource(R.string.account_tab_notifications),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (state.hubError != null && state.hubError != AccountUiError.OPEN_NOTIFICATION_FAILED) {
                state.hubError.accountErrorMessage()?.let { message ->
                    MobileMessage(
                        title = message,
                        description = state.hubErrorDetails,
                        actionLabel = stringResource(su.afk.yummy.tv.core.designsystem.R.string.common_retry),
                        onAction = { onEvent(AccountState.Event.RefreshHubSelected) },
                        fillMaxSize = false,
                    )
                }
            }
            AccountMobileNotificationsTab(state = state, onEvent = onEvent)
        }
    }
}
