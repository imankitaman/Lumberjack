package com.lumberjack

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import org.json.JSONException
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder



class LumberjackPeriodicWorker(context: Context, params: WorkerParameters) : Worker(context,params){

    override fun doWork(): Result {

        val logsInString = inputData.getString(Lumberjack.LOG_LIST)
        val url = inputData.getString(Lumberjack.LOGGER_API_URL)

        if(!logsInString .isNullOrBlank() && !url.isNullOrBlank()) {
            val result = logExceptionToServer(url,logsInString)
            //Todo- check if work completed or not based in work manager request ID and add timestamp check before deleting logs
            if(result.equals("200")) {

            }
            return result
        }
        return Result.failure()

    }

    /**
     * This method is used to upload/ log error to server
     */
    private fun logExceptionToServer(url:String?,logs: String?)
            : Result{
        val sb = StringBuilder()
        val httpUrl = url
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(httpUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.doOutput = true
            urlConnection.requestMethod = "POST"
            urlConnection.useCaches = false
            urlConnection.connectTimeout = 10000
            urlConnection.readTimeout = 10000
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.connect()
            val out =  DataOutputStream(urlConnection.outputStream)
            out.writeBytes(URLEncoder.encode(logs,"UTF-8"))
            Log.d("WorkManager" , urlConnection.toString())
            out.flush ()
            out.close ()
            val result = urlConnection.responseCode
            if (result == HttpURLConnection.HTTP_OK) {
                val br = BufferedReader(
                    InputStreamReader(
                        urlConnection.inputStream, "utf-8"
                    )
                )
                var line = br.readLine()
                while (line != null) {
                    sb.append(line)
                    line = br.readLine()
                }
                br.close()
                return Result.success()
                Log.d("WorkManager" ,  sb.toString())

            } else {
                Log.d("WorkManager" ,urlConnection.responseMessage)
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        } finally {
            urlConnection?.disconnect()
        }
        return Result.failure()
    }
}