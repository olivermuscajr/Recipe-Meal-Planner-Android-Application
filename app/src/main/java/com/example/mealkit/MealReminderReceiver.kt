package com.example.mealkit

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.media.AudioAttributes

class MealReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mealType = intent.getStringExtra("MEAL_TYPE") ?: "Meal"
        val userId = intent.getStringExtra("USER_ID")

        // Wake the device if it's in sleep mode
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MealKit:MealReminderWakeLock"
        )
        wakeLock.acquire(5000L) // Keep the lock for only 5 seconds

        try {
            // Check if the user matches the logged-in user
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val loggedInUserId = sharedPreferences.getString("loggedInUserId", null)

            if (loggedInUserId != userId) {
                // Ignore notification if user mismatch
                return
            }

            // Create intent to open the app
            val openAppIntent = Intent(context, LoginActivity2::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_from_notification", true)
                putExtra("MEAL_TYPE", mealType)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Notification sound and vibration
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            var vibrationPattern = longArrayOf(0, 500, 500, 500)

            // Notification channel for Android O+
            val channelId = "MEAL_REMINDER_CHANNEL"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(channelId, "Meal Reminder", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    enableVibration(true)
                    vibrationPattern = vibrationPattern
                    setSound(soundUri, audioAttributes)
                }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
            }

            // Build and display the notification
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle("Meal Reminder")
                .setContentText("Time to prepare your $mealType!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri)
                .setVibrate(vibrationPattern)

            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(context, "Notification permission not granted", Toast.LENGTH_SHORT).show()
                return
            }

            val notificationManagerCompat = NotificationManagerCompat.from(context)
            notificationManagerCompat.notify(mealType.hashCode(), builder.build())

        } finally {
            // Release the WakeLock
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}

