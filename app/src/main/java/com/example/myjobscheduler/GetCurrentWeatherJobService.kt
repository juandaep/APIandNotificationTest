package com.example.myjobscheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.lang.Exception
import java.text.DecimalFormat

class GetCurrentWeatherJobService : JobService() {

    companion object {
        private val TAG = GetCurrentWeatherJobService::class.java.simpleName

        // Nama kota yg ingin ditampilkan
        internal const val CITY = "Jakarta"

        // API Key
        internal const val APP_ID = ""
    }


    override fun onStartJob(params: JobParameters): Boolean {
        Log.d(TAG, "onStartJob()")
        getCurrentWeather(params)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.d(TAG, "onStopJob()")
        return true
    }

    private fun getCurrentWeather(job: JobParameters) {
        Log.d(TAG, "getCurrentWeather: Mulai...")
        val client = AsyncHttpClient()
        val url = "http://api.openweathermap.org/data/2.5/weather?q=$CITY&appid=$APP_ID"
        Log.d(TAG, "getCurrentWeather: $url")
        client.get(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<Header>,
                responseBody: ByteArray
            ) {
                val result = String(responseBody)
                Log.d(TAG, result)
                try {
                    val responseObject = JSONObject(result)

                    /*
                    Angka 0 pada getJSONObject menunjukkan index ke-0
                    Jika data yang ingin kita ambil ada lebih dari satu maka gunakanlah looping
                    */
                    val currentWeather =
                        responseObject.getJSONArray("weather").getJSONObject(0).getString("main")
                    val description = responseObject.getJSONArray("weather").getJSONObject(0)
                        .getString("description")
                    val tempKelvin = responseObject.getJSONObject("main").getDouble("temp")

                    val tempInCelsius = tempKelvin - 273
                    val temperature = DecimalFormat("##.##").format(tempInCelsius)

                    val title = "Current Weather"
                    val message = "$currentWeather, $description with $temperature celsius"
                    val notifId = 100

                    showNotification(applicationContext, title, message, notifId)

                    Log.d(TAG, "onSuccess: Selesai...")
                    // ketika proses selesai, maka perlu dipanggil jobFinished dengan parameter false;
                    jobFinished(job, false)

                } catch (e: Exception) {
                    Log.d(TAG, "onSuccess: Gagal...")
                    // ketika terjadi error, maka jobFinished diset dengan parameter true. Yang artinya job perlu di reschedule
                    jobFinished(job, true)
                    e.printStackTrace()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<Header>,
                responseBody: ByteArray,
                error: Throwable?
            ) {
                Log.d(TAG, "onFailure: Gagal...")
                // ketika proses gagal, maka jobFinished diset dengan parameter true. Yang artinya job perlu di reschedule
                jobFinished(job, true)
            }
        })
    }

    private fun showNotification(context: Context, title: String, message: String, notifId: Int) {
        val CHANNEL_ID = "Channel_1"
        val CHANNER_NAME ="Job scheduler channel"

        val notificationManagerCompat = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_replay_30)
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, android.R.color.black))
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setSound(alarmSound)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNER_NAME,
            NotificationManager.IMPORTANCE_DEFAULT)

            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)

            builder.setChannelId(CHANNEL_ID)
            notificationManagerCompat.createNotificationChannel(channel)
        }

        val notification = builder.build()
        notificationManagerCompat.notify(notifId, notification)
    }
}