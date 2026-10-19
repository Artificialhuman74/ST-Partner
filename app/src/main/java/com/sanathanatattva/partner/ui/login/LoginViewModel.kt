package com.sanathanatattva.partner.ui.login

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.AuthRepository
import com.sanathanatattva.partner.data.GoogleLogin
import com.sanathanatattva.partner.data.GoogleSignIn
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Text that is either ours (translated) or the server's own words. */
sealed interface UiText {
    data class Res(@StringRes val id: Int) : UiText
    data class Raw(val text: String) : UiText
}

fun ApiProblem.toUiText(): UiText = when (this) {
    ApiProblem.Network -> UiText.Res(R.string.error_network)
    is ApiProblem.Server -> message?.let(UiText::Raw) ?: UiText.Res(R.string.error_unknown)
    ApiProblem.Unknown -> UiText.Res(R.string.error_unknown)
}

private val EmailShape = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

fun isPlausibleEmail(email: String): Boolean = EmailShape.matches(email.trim())

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var emailError by mutableStateOf<UiText?>(null)
        private set
    var passwordError by mutableStateOf<UiText?>(null)
        private set
    var formError by mutableStateOf<UiText?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    var googleBusy by mutableStateOf(false)
        private set
    /** Set when Google sign-in worked but that address has no partner account. */
    var googleNoAccount by mutableStateOf<String?>(null)
        private set

    private val notices = Channel<UiText>(Channel.BUFFERED)
    /** One-off messages for the snackbar. */
    val noticeEvents = notices.receiveAsFlow()

    var resetSending by mutableStateOf(false)
        private set
    var resetSentTo by mutableStateOf<String?>(null)
        private set
    var resetError by mutableStateOf<UiText?>(null)
        private set

    private val loggedIn = Channel<User>(Channel.BUFFERED)
    val loggedInEvents = loggedIn.receiveAsFlow()

    fun onEmail(value: String) {
        email = value
        emailError = null
        formError = null
    }

    fun onPassword(value: String) {
        password = value
        passwordError = null
        formError = null
    }

    fun logIn() {
        if (loading) return
        emailError = if (isPlausibleEmail(email)) null else UiText.Res(R.string.login_error_email)
        passwordError = if (password.isNotEmpty()) null else UiText.Res(R.string.login_error_password)
        if (emailError != null || passwordError != null) return
        loading = true
        formError = null
        viewModelScope.launch {
            when (val result = auth.logIn(email, password)) {
                is ApiResult.Success -> loggedIn.send(result.value)
                is ApiResult.Failure -> formError = result.problem.toUiText()
            }
            loading = false
        }
    }

    /** [pickAccount] shows Google's account picker; it needs the Activity, so the screen supplies it. */
    fun logInWithGoogle(pickAccount: suspend () -> GoogleSignIn.Outcome) {
        if (googleBusy || loading) return
        googleBusy = true
        formError = null
        viewModelScope.launch {
            when (val outcome = pickAccount()) {
                is GoogleSignIn.Outcome.Token -> when (val result = auth.logInWithGoogle(outcome.firebaseIdToken)) {
                    is ApiResult.Success -> when (val login = result.value) {
                        is GoogleLogin.Partner -> loggedIn.send(login.user)
                        is GoogleLogin.NoAccount -> googleNoAccount = login.email.orEmpty()
                    }
                    is ApiResult.Failure -> formError = result.problem.toUiText()
                }
                GoogleSignIn.Outcome.Cancelled -> Unit
                GoogleSignIn.Outcome.NoGoogleAccount -> notices.send(UiText.Res(R.string.login_google_no_accounts))
                GoogleSignIn.Outcome.NotConfigured -> notices.send(UiText.Res(R.string.login_google_not_ready))
                GoogleSignIn.Outcome.Failed -> formError = UiText.Res(R.string.login_google_failed)
            }
            googleBusy = false
        }
    }

    /** [keepGoogle]: they chose to join with that Google account, so stay signed in to it. */
    fun dismissGoogleNoAccount(keepGoogle: Boolean) {
        googleNoAccount = null
        if (!keepGoogle) viewModelScope.launch { auth.forgetGoogle() }
    }

    fun sendReset(address: String) {
        if (resetSending) return
        if (!isPlausibleEmail(address)) {
            resetError = UiText.Res(R.string.login_error_email)
            return
        }
        resetSending = true
        resetError = null
        viewModelScope.launch {
            when (val result = auth.sendPasswordReset(address)) {
                is ApiResult.Success -> resetSentTo = address.trim()
                is ApiResult.Failure -> resetError = result.problem.toUiText()
            }
            resetSending = false
        }
    }

    fun clearReset() {
        resetSentTo = null
        resetError = null
    }
}
