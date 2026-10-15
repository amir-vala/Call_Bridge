package com.callbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class BridgeService extends Service {

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "callbridge_channel";

    private HttpServer httpServer;
    private CallManager callManager;
    private CallLogReader callLogReader;
    private ContactResolver contactResolver;

    @Override
    public void onCreate() {
        super.onCreate();
        callManager = new CallManager();
        callLogReader = new CallLogReader();
        contactResolver = new ContactResolver();
        httpServer = new HttpServer(8080, callManager, callLogReader, contactResolver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        try {
            if (!httpServer.isAlive()) {
                httpServer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CallBridge")
                .setContentText("Bridge service is running")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "CallBridge Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
