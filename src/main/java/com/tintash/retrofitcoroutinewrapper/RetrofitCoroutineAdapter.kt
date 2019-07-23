package com.tintash.retrofitcoroutinewrapper

import retrofit2.Response
import java.net.HttpURLConnection
import java.net.UnknownHostException

@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
inline fun <reified T, reified E> safeApiCall(
    call: () -> Response<T>,
    errorParser: ErrorParser<E>? = null,
    errorConsumer: ErrorConsumer? = null
): NetworkResult<T, E> {

    val response = try {
        call.invoke()
    } catch (e: Exception) {
        val errorCode =
            if (e is UnknownHostException) HttpURLConnection.HTTP_UNAVAILABLE else ResponseCode.UNKNOWN_ERROR
        return NetworkResult.Error(errorCode, errorConsumer?.consumeException(e) == true, null)
    }

    return if (response.isSuccessful) {
        NetworkResult.createSuccess(response)
    } else {
        NetworkResult.createError(response, errorConsumer?.consumeError(response) == true, errorParser)
    }
}