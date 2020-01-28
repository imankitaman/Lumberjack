package com.lumberjack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import java.lang.Exception
import java.util.ArrayList

/**
 * Created By ankit.aman on Jan,2020
 */
class LumberjackDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASENAME, null, DATABASEVERSION){

    companion object{
        private val DATABASENAME = "device_log_db"
        private val DATABASEVERSION = 1
        private val TABLE_DEVICE_LOG = "device_logs"
        private val TABLE_EXCEPTION_LOG = "exception_logs"
        private val COLUMN_ID = "_id"
        private val COLUMN_LOG = "log"
    }

    private var database: SQLiteDatabase? = null


    override fun onCreate(db: SQLiteDatabase?) {

        val CREATE_DEVICE_LOG_TABLE = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_DEVICE_LOG
                + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_LOG + " TEXT"
                + ");")


        val  CREATE_EXCEPTION_TABLE = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_EXCEPTION_LOG
                + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_LOG + " TEXT"
                + ");")

        try { db?.execSQL(CREATE_DEVICE_LOG_TABLE) } catch (e: Exception){e.message}
        try { db?.execSQL(CREATE_EXCEPTION_TABLE) } catch (e: Exception){e.message}

    }


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        try {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_DEVICE_LOG")
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_EXCEPTION_LOG")
            onCreate(db)
        } catch (e: Exception) { e.message }
    }

    private fun initDB() {
        if (database == null)
            database = this.writableDatabase
    }


    /**
     * Call addDeviceLog to log device related info
     * DB insert operation is performed
     *
     * Note:- Will always check for db is initialized or Not
     */
    fun addDeviceLog(log: String){
        initDB()

        val contentValues = ContentValues()
        contentValues.put(COLUMN_LOG, log)

        try {
            database?.insert(TABLE_DEVICE_LOG, null, contentValues)
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    /**
     * Call addExceptionLog to log exceptions related info
     * DB insert operation is performed
     *
     * Note:- Will always check for db is initialized or Not
     */
    fun addExceptionLog(log: String){
        initDB()

        val contentValues = ContentValues()
        contentValues.put(COLUMN_LOG, log)

        try {
            database?.insert(TABLE_EXCEPTION_LOG, null, contentValues)
        } catch (e: Exception) {
            e.localizedMessage
        }
    }



    fun getDeviceLogList(): ArrayList<String>?{
        return getLogsAsList(TABLE_DEVICE_LOG)
    }


    fun getExceptionsLogList(): ArrayList<String>?{
        return getLogsAsList(TABLE_EXCEPTION_LOG)
    }


    /**
     *  Call getLogsAsList to pullout all the logs with respect to particular table
     *
     *  Note :- check for DB initialization before any operation
     *  @return ArrayList<String> optional can we null
     */
    fun getLogsAsList(tableName: String): ArrayList<String>? {

        initDB()

        var deviceLogList: ArrayList<String>? = null

        val cursor = database?.query(
            tableName,
            arrayOf(COLUMN_ID, COLUMN_LOG),
            null,
            null,
            null,
            null,
            null
        )

        if (cursor == null || cursor.isClosed) {
            return null
        }

        try {
            if (cursor.moveToFirst()) {
                deviceLogList = ArrayList()

                do {
                    if (cursor.isClosed) {
                        break
                    }

                    val deviceLogString = cursor.getString(1)
                    if (!TextUtils.isEmpty(deviceLogString)) {
//                        val deviceLog = DeviceLogModel(deviceLogString)

//                        // Get RowId for DeviceLogModel
//                        val rowId = Integer.valueOf(cursor.getString(0))
//                        deviceLog.setId(rowId)

                        deviceLogList.add(cursor.getString(cursor.getColumnIndex(COLUMN_LOG)))
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.localizedMessage
        } finally {
            cursor.close()
        }

        return deviceLogList
    }

    /**
     * call deleteAllDeviceLogs to flush logs from DB
     *
     * Should call after every Work Manager Job done
     */
    fun deleteAllDeviceLogs() {
        initDB()
        try {
            database?.delete(TABLE_DEVICE_LOG, null, null)
        } catch (e: Exception) {
            e.localizedMessage
        }
    }


    fun deleteAllExceptionLogs() {
        initDB()
        try {
            database?.delete(TABLE_EXCEPTION_LOG, null, null)
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

}