package com.lumberjack

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.lang.StringBuilder


/**
 * Created By ankit.aman on Jan,2020
 */

class AppLoggerFormat{

    private var deviceUUID: String? = null

    constructor(context: Context?){
       context?.apply {
           deviceUUID = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
       }
    }

    /**
     * Implement this method to override the default log message format.
     *
     * @param logLevel The level of logcat logging that Parse should do.
     * @param message  Log message that need to be customized.
     * @return Formatted Log Message that will store in database.
     */
    fun formatLogMessage(logLevel: Int, tag: String, message: String, userMetaData: String?=null): String {
        val logLevelName = getLogLevelName(logLevel)
        val appMetaData = getAppMetaData()
        return getFormattedLogMessage(
            logLevelName,
            tag,
            message,
            appMetaData,
            userMetaData
        )
    }


    fun formatLogMessage(tag: String, message: String, userMetaData: String?): String {
      return this.formatLogMessage(Log.INFO,tag,message,userMetaData = userMetaData)
    }

    fun getFormattedLogMessage(
        logLevelName: String, tag: String, message: String, appMetadata: String?, userMetaData: String?
    ): String {
        return " [$logLevelName/$tag]: | $appMetadata | $userMetaData | $message "
    }


    fun getAppMetaData(): String {
        val osVersion = "Android-" + Build.VERSION.RELEASE
        val sb = StringBuilder()
            .append("osVersion: $osVersion | ")
            .append("timeStamp" )

        if (deviceUUID == null) {
            sb.append("DeviceUUID: $deviceUUID | ")
        }

        return  sb.toString()
    }


    private  fun getLogLevelName(messageLogLevel: Int): String {
        return when (messageLogLevel) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "NONE"
        }
    }

}

