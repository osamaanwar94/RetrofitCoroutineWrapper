package com.tintash.retrofitcoroutinewrapper

import androidx.annotation.IntDef
import retrofit2.Response

/**
 * Network Result Layer
 *
 */

/**
 * Response Code
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    ResponseCode.ONGOING,
    ResponseCode.UNKNOWN_ERROR
)
annotation class ResponseCode {
    companion object {
        const val ONGOING = 0
        const val UNKNOWN_ERROR = -1
    }
}

/**
 * Request Status
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    RequestStatus.ONGOING,
    RequestStatus.SUCCESS,
    RequestStatus.FAILED
)
annotation class RequestStatus {
    companion object {
        const val ONGOING = 0
        const val SUCCESS = 1
        const val FAILED = 2
    }
}

/**
 *
 * @param T Payload type
 */
sealed class NetworkResult<out T, out E> {
    data class Success<out T>(
        val code: Int,
        val data: T?
    ) : NetworkResult<T, Nothing>()

    data class Error<out E>(
        val code: Int,
        val isErrorConsumed: Boolean,
        val parsedErrors: List<E>?
    ) : NetworkResult<Nothing, E>()

    companion object
}

/**
 *
 * @param E
 */
interface ErrorParser<E> {
    fun parseErrors(response: Response<*>): List<E>?
}

/**
 *
 */
interface ErrorConsumer {
    fun consumeError(response: Response<*>): Boolean

    fun consumeException(e: Exception): Boolean
}

/**
 *
 * @receiver NetworkResult.Companion
 * @param response Response<*>
 * @param isErrorConsumed Boolean
 * @param errorParser ErrorParser<E>?
 * @return NetworkResult.Error<E>
 */
inline fun <reified E> NetworkResult.Companion.createError(
    response: Response<*>,
    isErrorConsumed: Boolean,
    errorParser: ErrorParser<E>?
): NetworkResult.Error<E> {
    return NetworkResult.Error(response.code(), isErrorConsumed, errorParser?.parseErrors(response))
}

/**
 *
 * @receiver NetworkResult.Companion
 * @param response Response<T>
 * @return NetworkResult.Success<T>
 */
inline fun <reified T> NetworkResult.Companion.createSuccess(
    response: Response<T>
): NetworkResult.Success<T> {
    return NetworkResult.Success(response.code(), response.body())
}

/**
 *
 * @receiver NetworkResult<T, E>
 * @return ResponseWrapper<T, E>
 */
inline fun <reified T, E> NetworkResult<T, E>.createResponseModel(): ResponseWrapper<T, E> {
    return when (this) {
        is NetworkResult.Success -> {
            ResponseWrapper.createSuccess(code, data)
        }
        is NetworkResult.Error -> {
            ResponseWrapper.createError(code, parsedErrors)
        }
    }
}

/**
 * @param T Payload type
 * @property responseCode Int ResponseCode of the Api
 * @property success Boolean Was Api successful
 * @property status Int {Ongoing, Success, Failure}
 * @property data T? payload data
 * @property errors JsonArray? errors
 * @constructor
 */
data class ResponseWrapper<T, E>(
    val responseCode: Int,
    val success: Boolean,
    val status: Int,
    val data: T?,
    val errors: List<E>?
) {
    companion object
}

/**
 *
 * @receiver ResponseWrapper.Companion
 * @return ResponseWrapper<T, E>
 */
inline fun <reified T, E> ResponseWrapper.Companion.createOnGoing(): ResponseWrapper<T, E> {
    return ResponseWrapper(ResponseCode.ONGOING, false, RequestStatus.ONGOING, data = null, errors = null)
}

/**
 *
 * @receiver ResponseWrapper.Companion
 * @param code Int
 * @param data T?
 * @return ResponseWrapper<T, E>
 */
inline fun <reified T, E> ResponseWrapper.Companion.createSuccess(code: Int, data: T?): ResponseWrapper<T, E> {
    return ResponseWrapper(code, true, RequestStatus.SUCCESS, data = data, errors = null)
}

/**
 *
 * @receiver ResponseWrapper.Companion
 * @param code Int
 * @param errors List<E>?
 * @return ResponseWrapper<T, E>
 */
inline fun <reified T, E> ResponseWrapper.Companion.createError(code: Int, errors: List<E>?): ResponseWrapper<T, E> {
    return ResponseWrapper(code, false, RequestStatus.FAILED, data = null, errors = errors)
}