package com.lumberjack


/**
 * Created By ankit.aman on Jan,2020
 */
data class ResponseClass<T>(val response:T,val errorMessage : String, val status: Int)