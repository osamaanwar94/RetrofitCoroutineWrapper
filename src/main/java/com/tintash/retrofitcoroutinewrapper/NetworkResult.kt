package com.tintash.retrofitcoroutinewrapper

import androidx.annotation.IntDef
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import retrofit2.Response

/**
 * Response Code
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    ResponseCode.ONGOING,
    ResponseCode.SUCCESS,
    ResponseCode.SUCCESS_NO_CONTENT,
    ResponseCode.UNAUTHORIZED,
    ResponseCode.UNPROCESSABLE_ENTITY,
    ResponseCode.SERVER_ERROR,
    ResponseCode.INVALID_EMAIL,
    ResponseCode.FORCE_UPDATE,
    ResponseCode.NETWORK_ERROR,
    ResponseCode.UNKNOWN_ERROR
)
annotation class ResponseCode {
    companion object {
        const val ONGOING = 0
        const val SUCCESS = 200
        const val SUCCESS_NO_CONTENT = 204
        const val UNAUTHORIZED = 401
        const val UNPROCESSABLE_ENTITY = 422
        const val SERVER_ERROR = 500
        const val INVALID_EMAIL = 404
        const val FORCE_UPDATE = 426
        const val NETWORK_ERROR = 503
        const val UNKNOWN_ERROR = 512
    }
}

/**
 *
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
sealed class NetworkResult<out T> {
    data class Success<out T : Any>(
        val code: Int,
        val data: T?
    ) : NetworkResult<T>() {
        constructor(response: Response<T>) : this(response.code(), response.body())
    }

    data class Error(
        val code: Int,
        val isErrorConsumed: Boolean,
        val parsedErrors: JsonArray?
    ) : NetworkResult<Nothing>() {
        constructor(response: Response<*>, isErrorConsumed: Boolean) : this(
            response.code(),
            isErrorConsumed,
            response.parseErrors()
        )
    }
}

inline fun <reified T> NetworkResult<T>.createResponseModel(): ResponseWrapper<T> {
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
data class ResponseWrapper<T>(
    val responseCode: Int,
    val success: Boolean,
    val status: Int,
    val data: T?,
    val errors: JsonArray?
) {
    companion object
}

inline fun <reified T> ResponseWrapper.Companion.createOnGoing(): ResponseWrapper<T> {
    return ResponseWrapper(ResponseCode.ONGOING, false, RequestStatus.ONGOING, data = null, errors = null)
}

inline fun <reified T> ResponseWrapper.Companion.createSuccess(code: Int, data: T?): ResponseWrapper<T> {
    return ResponseWrapper(code, true, RequestStatus.SUCCESS, data = data, errors = null)
}

inline fun <reified T> ResponseWrapper.Companion.createError(code: Int, errors: JsonArray?): ResponseWrapper<T> {
    return ResponseWrapper(code, false, RequestStatus.FAILED, data = null, errors = errors)
}

private fun Response<*>.parseErrors() = try {
    val errorString = errorBody()?.string()
    val error = Gson().fromJson(errorString, JsonObject::class.java)
    error?.get("errors")?.asJsonArray
} catch (e: JsonSyntaxException) {
    JsonArray().also {
        it.add("Sorry. Unable to access server. Try Again")
    }
}