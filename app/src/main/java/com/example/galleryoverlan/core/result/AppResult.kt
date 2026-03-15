package com.example.galleryoverlan.core.result

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error"
    ) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }

    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
}

inline fun <T> runCatchingApp(block: () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: Exception) {
        AppResult.Error(e)
    }
