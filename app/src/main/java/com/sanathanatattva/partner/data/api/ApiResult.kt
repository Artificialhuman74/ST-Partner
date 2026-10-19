package com.sanathanatattva.partner.data.api

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val problem: ApiProblem) : ApiResult<Nothing>
}

sealed interface ApiProblem {
    /** No connection, DNS failure, timeout. */
    data object Network : ApiProblem

    /**
     * The server answered with an error; [message] is its `error` text when it sent one,
     * [fields] maps form fields to their messages (rider signup validation).
     */
    data class Server(val code: Int, val message: String?, val fields: Map<String, String> = emptyMap()) : ApiProblem

    data object Unknown : ApiProblem
}

internal val apiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val body = parseErrorBody(e.response()?.errorBody()?.string())
    ApiResult.Failure(ApiProblem.Server(e.code(), body?.error?.takeIf { it.isNotBlank() }, body?.fields.orEmpty()))
} catch (e: IOException) {
    ApiResult.Failure(ApiProblem.Network)
} catch (e: Exception) {
    ApiResult.Failure(ApiProblem.Unknown)
}

internal fun parseErrorBody(body: String?): ErrorBody? {
    if (body.isNullOrBlank()) return null
    return runCatching { apiJson.decodeFromString(ErrorBody.serializer(), body) }.getOrNull()
}

internal fun parseErrorMessage(body: String?): String? = parseErrorBody(body)?.error?.takeIf { it.isNotBlank() }
