package mobi.nftblockchain.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    private val TAG = "AlarmReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                "USER",
                "User-specific notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Make sound"
            }

            notificationManager.createNotificationChannel(channel)
        }

        val activity = Intent(context, MainActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val notification = NotificationCompat.Builder(context, "USER")
            .setContentTitle("Time to Spin!")
            .setContentText("Are you ready? You can again receive up to 500 NFT for FREE now.")
            .setSmallIcon(R.drawable.logo)
            .setColor(ContextCompat.getColor(context, R.color.push))
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    activity,
                    PendingIntent.FLAG_IMMUTABLE
                )
            ).build()

        NotificationManagerCompat.from(context).notify(1, notification)
    }
}