package su.afk.yummy.tv.feature.account.mobile.account

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoginCard
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileNotificationsTab
import su.afk.yummy.tv.feature.account.presentation.R

@Composable
fun AccountNotificationsMobileScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val autofillManager = LocalAutofillManager.current
    val captchaHint = stringResource(R.string.account_captcha_required_toast)
    val openNotificationFailed = AccountUiError.OPEN_NOTIFICATION_FAILED.accountErrorMessage()

    LaunchedEffect(Unit) {
        onEvent(AccountState.Event.ScreenShown)
    }
    LaunchedEffect(effect) {
        effect.collect { effectItem ->
            when (effectItem) {
                AccountState.Effect.HideKeyboard -> {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
                AccountState.Effect.ShowCaptchaHint ->
                    Toast.makeText(context, captchaHint, Toast.LENGTH_SHORT).show()
                AccountState.Effect.DiscardAutofill -> autofillManager?.cancel()
            }
        }
    }
    LaunchedEffect(state.hubError) {
        if (state.hubError == AccountUiError.OPEN_NOTIFICATION_FAILED && openNotificationFailed != null) {
            Toast.makeText(context, openNotificationFailed, Toast.LENGTH_SHORT).show()
        }
    }

    BaseScreen(
        isScroll = false,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MobileTopBar(
                title = stringResource(R.string.account_tab_notifications),
                onBack = { onEvent(AccountState.Event.BackSelected) },
            )
            when {
                !state.isSessionResolved -> {
                    AccountMobileLoadingIndicator(modifier = Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .mobileContentMaxWidth()
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp,
                            bottom = MobileBottomBarDefaults.contentBottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!state.isSignedIn) {
                            item(key = "sign_in") {
                                AccountMobileLoginCard(
                                    state = state,
                                    onEvent = onEvent,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            if (state.hubError != null &&
                                state.hubError != AccountUiError.OPEN_NOTIFICATION_FAILED
                            ) {
                                item(key = "hub_error") {
                                    state.hubError.accountErrorMessage()?.let { error ->
                                        MobileMessage(
                                            title = error,
                                            description = state.hubErrorDetails,
                                            icon = Icons.Filled.Warning,
                                            actionLabel = stringResource(CoreR.string.common_retry),
                                            onAction = { onEvent(AccountState.Event.RefreshHubSelected) },
                                            fillMaxSize = false,
                                        )
                                    }
                                }
                            }
                            item(key = "notifications") {
                                AccountMobileNotificationsTab(
                                    state = state,
                                    onEvent = onEvent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
