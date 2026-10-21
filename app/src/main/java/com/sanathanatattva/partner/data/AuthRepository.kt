package com.sanathanatattva.partner.data

import com.sanathanatattva.partner.data.partner.PartnerRepository
import com.sanathanatattva.partner.data.shop.ShopRepository
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.EmailRequest
import com.sanathanatattva.partner.data.api.GoogleTokenRequest
import com.sanathanatattva.partner.data.api.LoginRequest
import com.sanathanatattva.partner.data.api.PartnerApi
import com.sanathanatattva.partner.data.api.User
import com.sanathanatattva.partner.data.api.apiCall
import com.sanathanatattva.partner.push.PushRegistrar

sealed interface GoogleLogin {
    data class Partner(val user: User) : GoogleLogin
    /** Signed in with Google fine, but no ST partner account uses that address. */
    data class NoAccount(val email: String?) : GoogleLogin
}

class AuthRepository(
    private val api: PartnerApi,
    private val sessions: SessionStore,
    private val google: GoogleSignIn,
    private val push: PushRegistrar,
    private val partner: PartnerRepository,
    private val shop: ShopRepository,
) {
    suspend fun logIn(email: String, password: String): ApiResult<User> =
        when (val result = apiCall { api.login(LoginRequest(email.trim(), password)) }) {
            is ApiResult.Success -> {
                sessions.save(result.value.token, result.value.user)
                push.register()
                ApiResult.Success(result.value.user)
            }
            is ApiResult.Failure -> result
        }

    suspend fun logInWithGoogle(firebaseIdToken: String): ApiResult<GoogleLogin> =
        when (val result = apiCall { api.partnerGoogle(GoogleTokenRequest(firebaseIdToken)) }) {
            is ApiResult.Success -> {
                val body = result.value
                if (body.exists && body.token != null && body.user != null) {
                    sessions.save(body.token, body.user)
                    push.register()
                    ApiResult.Success(GoogleLogin.Partner(body.user))
                } else {
                    // Stay signed in to Google: the person may join with this account next.
                    ApiResult.Success(GoogleLogin.NoAccount(body.email))
                }
            }
            is ApiResult.Failure -> {
                google.signOut()
                result
            }
        }

    /**
     * Re-reads the signed-in user. A 401/403 means the token is dead or the account
     * was suspended, so the session is dropped and the caller should go to Login.
     */
    suspend fun refresh(): ApiResult<User> =
        when (val result = apiCall { api.me() }) {
            is ApiResult.Success -> {
                sessions.updateUser(result.value.user)
                ApiResult.Success(result.value.user)
            }
            is ApiResult.Failure -> {
                val problem = result.problem
                if (problem is ApiProblem.Server && (problem.code == 401 || problem.code == 403)) sessions.clear()
                result
            }
        }

    suspend fun sendPasswordReset(email: String): ApiResult<Unit> =
        when (val result = apiCall { api.forgotPassword(EmailRequest(email.trim())) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }

    /** Google sign-in worked but the person chose not to join with that account. */
    suspend fun forgetGoogle() = google.signOut()

    suspend fun logOut() {
        push.unregister()
        sessions.clear()
        partner.clearCache()
        shop.clear()
        google.signOut()
    }
}
