package com.example.petbuddy.utils

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.petbuddy.R

fun showNotification(context: Context, title: String, message: String) {
    val builder = NotificationCompat.Builder(context, "appointments_channel")
        .setSmallIcon(R.drawable.ic_launcher_foreground) // zamijeni ako imaš svoju ikonu
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    with(NotificationManagerCompat.from(context)) {
        notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
