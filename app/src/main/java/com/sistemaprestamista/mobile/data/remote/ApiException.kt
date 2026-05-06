package com.sistemaprestamista.mobile.data.remote

class ApiException(
    message: String,
    val statusCode: Int? = null,
) : Exception(message)
