package com.selfcontrol.domain.model

/**
 * A generic sealed class to represent the result of an operation
 * Used throughout the domain layer for error handling
 */
sealed class Result<out T> {
    
    data class Success<T>(val data: T) : Result<T>()
    
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error occurred"
    ) : Result<Nothing>()
    
    data object Loading : Result<Nothing>()
    
    /**
     * Returns true if this is a Success result
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Returns true if this is an Error result
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Returns true if this is a Loading result
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Returns the data if Success, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Returns the data if Success, or throws the exception if Error
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Cannot get data from Loading state")
    }
    
    /**
     * Execute block if Success
     */
    inline fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) {
            block(data)
        }
        return this
    }
    
    /**
     * Execute block if Error
     */
    inline fun onError(block: (Throwable) -> Unit): Result<T> {
        if (this is Error) {
            block(exception)
        }
        return this
    }
    
    /**
     * Execute block if Loading
     */
    inline fun onLoading(block: () -> Unit): Result<T> {
        if (this is Loading) {
            block()
        }
        return this
    }
    
    /**
     * Map the success value to a new type
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception, message)
        is Loading -> Loading
    }
    
    /**
     * FlatMap for chaining operations
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> Error(exception, message)
        is Loading -> Loading
    }
    
    companion object {
        /**
         * Create a Success result
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Create an Error result
         */
        fun error(exception: Throwable, message: String? = null): Result<Nothing> =
            Error(exception, message ?: exception.message ?: "Unknown error")
        
        /**
         * Create a Loading result
         */
        fun loading(): Result<Nothing> = Loading
    }
}

/**
 * Extension function to convert Kotlin Result to domain Result
 */
fun <T> kotlin.Result<T>.toDomainResult(): Result<T> = fold(
    onSuccess = { Result.Success(it) },
    onFailure = { Result.Error(it) }
)
