package com.lumberjack

import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import androidx.work.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequest
import com.google.gson.GsonBuilder
import java.util.concurrent.ExecutionException


/**
 * Created By ankit.aman on Jan,2020
 */
class Lumberjack private constructor() {

    // Predefined logger format
    private var mLogFormat: AppLoggerFormat? = null
    // Application Context for Workmanager and DB opeation
    private var context: Context? = null
    private var executorService: ExecutorService? = null
    private var lumberjackDatabaseHelper: LumberjackDatabaseHelper? = null
    private var URL: String = "server url"// TODO()


    companion object {
        private val TAG = "Lumberjack"
        private const val MAX_LOG_LENGTH = 4000//4*1024 approx
        private const val MAX_TAG_LENGTH = 23
        private val LOG_SYNC_TIME_IN_HR = 2L
        private val EXCEPTION_LOG_TAG = "exception"
        val LOGGER_API_URL = "logger_url"
        val LOG_LIST = "log_list"

        /* Singleton Instance */
        private val INSTANCE: Lumberjack by lazy { Lumberjack() }

        @Synchronized
        fun getInstance(): Lumberjack {
            return INSTANCE
        }
    }


    /**
     * Call this method to init Lumberjack.
     * By default, 2hrs is a Sync time to upload logs automatically.
     *
     * @param context The current context.
     * @see .init
     */
    fun init(@NonNull context: Context) {
        init(context, LOG_SYNC_TIME_IN_HR, AppLoggerFormat(context))
    }

    /**
     * Call this method to init Lumberjack.
     * By default, 2hrs is a Sync time to upload logs automatically.
     *
     * @param context   The current context.
     * @param logFormat [LogFormat] to set custom log message format.
     * @see .init
     */
    fun init(@NonNull context: Context?, @NonNull logFormat: AppLoggerFormat) {
        init(context, LOG_SYNC_TIME_IN_HR, logFormat)
    }

    /**
     * Call this method to init Lumberjack.
     * By default, 2hrs is a Sync time to upload logs automatically.
     *
     * @param context             The current context.
     * @param logSyncTimeInHr Sync time of logs in hr.
     * @see .init
     */
    fun init(@NonNull context: Context?, logSyncTimeInHr: Long) {
        init(context, logSyncTimeInHr, AppLoggerFormat(context))
    }


    /**
     * Call this method to init Lumberjack.
     * By default, 2hrs is a Sync time logSyncTimeInHr upload logs automatically.. You can change the sync time
     * period of logs by defining expiryTimeInSeconds.
     *
     * @param context             The current context.
     * @param logSyncTimeInHr Expiry time for logs in seconds.
     * @param logFormat           [LogFormat] to set custom log message format.
     * @see .init
     */
    fun init(
        @NonNull context: Context?, logSyncTimeInHr: Long
        , logFormat: AppLoggerFormat?) {

        if (context == null) {
            Log.e(TAG, "Lumberjack isn't initialized: Context couldn't be null")
            return
        }

        this.context = context.applicationContext

        synchronized(Lumberjack::class.java) {
            if (logFormat != null) {
                mLogFormat = logFormat
            }

            if (lumberjackDatabaseHelper == null) {
                lumberjackDatabaseHelper = LumberjackDatabaseHelper(context)
            }
        }
    }

    /**
     * isInitialize Method will check for Fb instance and logger model instance
     */
    private fun isInitialize(): Boolean {
        if (lumberjackDatabaseHelper == null || mLogFormat == null) {
            init(context, logFormat = AppLoggerFormat(context))
            return false
        }
        return true
    }


    /**
     * call log will print for logs in format if MAX_LOG_LENGTH
     * reached will split into next log line and manager to print
     *
     */
    fun log(logType: Int, tag: String?, message: String?) {
        if (message == null || tag == null) return

        if (BuildConfig.DEBUG) {
            val finalTagName =
                if (tag.length > MAX_TAG_LENGTH) tag.substring(MAX_TAG_LENGTH) else tag
            if (MAX_LOG_LENGTH > message.length) {
                Log.println(logType, finalTagName, message)
            } else {
                // Split by line, then ensure each line can fit into Log's maximum length.
                var i = 0
                val length = message.length
                while (i < length) {
                    var newline = message.indexOf('\n', i)
                    newline = if (newline != -1) newline else length
                    do {
                        val end = newline.coerceAtMost(i + MAX_LOG_LENGTH)
                        val part = message.substring(i, end)
                        Log.println(logType, finalTagName, part)
                        i = end
                    } while (i < newline)
                    i++
                }
            }
        }

//        writeToDeviceLogDB(
//            getFormattedLog(logType, tag, message)
//        )
    }


    fun logException(tag: String, message: String, tr: Throwable?) {
        if (BuildConfig.DEBUG) {
            Log.e(
                tag, "EXCEPTION: " + getMethodName() + ", " + message + '\n' +
                        Log.getStackTraceString(tr)
            )
        }

        writeToExceptionLogDB(
            getFormattedLog(
                Log.ERROR, tag, "EXCEPTION: " + getMethodName() + ", "
                        + message
            )
        )
    }


    /**
     * will pull out the method name from stacktrace
     */
    private fun getMethodName(): String {
        val stacktrace = Thread.currentThread().stackTrace
        val e = stacktrace[4]//coz 0th will be getStackTrace so 1st
        return e.methodName
    }


    /**
     * Call writeToDeviceLogDB method for normal logging
     *
     * @param message type String nullable
     *  Note: Creating Fixed Single Thread Execution Since it is a DB operation
     */
    private fun writeToDeviceLogDB(message: String?) {
        try {
            if (executorService == null)
                executorService = Executors.newSingleThreadExecutor()

            val runnable = Runnable {
                try {
                    if (!isInitialize() || message == null || message.isEmpty())
                        return@Runnable
                    lumberjackDatabaseHelper?.addDeviceLog(message)
                } catch (ex: Exception) {
                    ex.localizedMessage
                }
            }
            executorService?.submit(runnable)
        } catch (e: OutOfMemoryError) {
            e.localizedMessage
        } catch (e: Exception) {
            e.localizedMessage
        }
    }



    /**
     * Call writeToExceptionLogDB method for exception logging
     *
     * @param message type String nullable
     * Note: Creating Fixed Single Thread Execution Since it is a DB operation
     */
    private fun writeToExceptionLogDB(message: String?) {
        try {
            if (executorService == null)
                executorService = Executors.newSingleThreadExecutor()

            val runnable = Runnable {
                try {
                    if (!isInitialize() || message == null || message.isEmpty())
                        return@Runnable
                    lumberjackDatabaseHelper?.addExceptionLog(message)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            executorService?.submit(runnable)

        } catch (e: OutOfMemoryError) {
            e.localizedMessage
        } catch (e: Exception) {
            e.localizedMessage
        }
    }


    /**
     * Call getFormattedLog method to get formatted logs
     *
     * @param logLevel type {@link #Log}
     * @param tag type String as class or message tag
     * @param message type optional String
     * @return String type response
     *
     * Ex:- "[$logLevelName/$tag]: | $appMetadata | $userMetaData | $message "
     */
    private fun getFormattedLog(logLevel: Int, tag: String, message: String?): String? {
        return if (isInitialize()) {
            message?.apply {
                mLogFormat?.formatLogMessage(logLevel, tag, this)
            }
        } else null
    }


    /**
     * Call syncLogsIfAny on every App launch
     * - Will schedule Syncing logs after 2Hr {@link #LOG_SYNC_TIME_IN_HR }By default defined hr
     *   also replace if any existing scheduled
     * - TAG name LOGGER_API_URL
     *
     * Blocker if WorkManager is Scheduled / Running or DeviceLogList is empty
     */
    private fun syncLogsIfAny() {

        if(isWorkerRunning(LOGGER_API_URL)) return

        val deviceLogs = lumberjackDatabaseHelper?.getDeviceLogList()

        if(deviceLogs.isNullOrEmpty()) return

        /**
         * Convert logs as string
         * to be passsed as input Data for Work
         * assign as LOGGER_API_URL
         */
        val logsInString = convertToJson(deviceLogs)
        val data = Data.Builder()
        data.putString(LOGGER_API_URL,URL)
        logsInString?.apply {
            data.putString(LOG_LIST, this)
        }


        val workRequest: OneTimeWorkRequest = OneTimeWorkRequest
            .Builder(LumberjackPeriodicWorker::class.java)
            .setConstraints(getWorkerConstraints())
            .setInputData(data.build())
            .setInitialDelay(LOG_SYNC_TIME_IN_HR,TimeUnit.HOURS)
            .addTag(LOGGER_API_URL)
            .build()

        context?.apply {
            WorkManager.getInstance(this).enqueueUniqueWork(LOGGER_API_URL, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }

    private fun syncExceptionsIfAny(){

        val exceptionLogs = lumberjackDatabaseHelper?.getExceptionsLogList()

        if(exceptionLogs.isNullOrEmpty()) return

        val logsInString = convertToJson(exceptionLogs)
        val data = Data.Builder()
        data.putString(LOGGER_API_URL,URL)
        logsInString?.apply {
            data.putString(LOG_LIST, this)
        }

        val oneTimeWork = OneTimeWorkRequest.Builder(LumberjackPeriodicWorker::class.java)
            .setConstraints(getWorkerConstraints())
            .setInputData(data.build())
            .setInitialDelay(LOG_SYNC_TIME_IN_HR,TimeUnit.HOURS)
            .addTag(EXCEPTION_LOG_TAG)
            .build()

        context?.apply {
            WorkManager.getInstance(this).enqueue(oneTimeWork)
        }

//        WorkManager.getInstance(context!!).getWorkInfosByTag(EXCEPTION_LOG_TAG)
//            .addListener()
    }


    /**
     *  ## HACK ### to check for is Work Manager is already scheduled or not
     *
     *  Call isWorkerRunning to check for of Worker State is in RUNNING or ENQUEUED
     *  will return true
     *
     *  @return Boolean
    **/
    fun isWorkerRunning(tag: String): Boolean {
        if(context==null) return false
        var status: List<WorkInfo>?
        try {
            status = WorkManager.getInstance(context!!).getWorkInfosByTag(tag).get()
            for (workStatus in status!!) {
                if (workStatus.state === WorkInfo.State.RUNNING || workStatus.state === WorkInfo.State.ENQUEUED) {
                    return true
                }
            }
            return false
        } catch (e: InterruptedException) {
            e.localizedMessage
        } catch (e: ExecutionException) {
            e.localizedMessage
        }

        return false
    }

    /**
     *  getWorkerConstraints contains WorkManager related Constraints
     *  - Check for Battery
     *  - Check for DeviceIdle
     *  - Check for Network Connected
     *
     *  @return Constraints
     */
    private fun getWorkerConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    /**
     * convertToJson Convert Array of String To String format
     */
    private fun convertToJson(data: ArrayList<String>): String?{
        val gson = GsonBuilder().serializeNulls().create()
       return gson.toJson(data)
    }


}