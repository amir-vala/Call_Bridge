package com.callbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class BridgeService extends Service {

    private static final String TAG = "CallBridge";
    private static final int PORT = 8765;
    
    // ✅ متغیر static برای وضعیت سرویس
    private static boolean running = false;

    private HttpServer httpServer;
    private PowerManager.WakeLock wakeLock;

    // ✅ متد static برای چک کردن وضعیت
    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "سرویس ساخته شد");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForeground();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CallBridge::WakeLock");
        wakeLock.acquire();

        try {
            // ✅ اصلاح شد: پاس دادن this به عنوان Context
            CallManager callManager = new CallManager(this);
            CallLogReader callLogReader = new CallLogReader(this);
            ContactResolver contactResolver = new ContactResolver(this);

            httpServer = new HttpServer(PORT, callManager, callLogReader, contactResolver);
            httpServer.start();
            
            running = true;  // ✅ تنظیم وضعیت
            Log.i(TAG, "سرور HTTP روی پورت " + PORT + " شروع شد");
        } catch (Exception e) {
            Log.e(TAG, "خطا در شروع سرور: " + e.getMessage());
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;  // ✅ تنظیم وضعیت

        if (httpServer != null) {
            httpServer.stop();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        Log.i(TAG, "سرویس متوقف شد");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAsForeground() {
        String channelId = "call_bridge";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, "پل تماس", NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("سرویس پل تماس در حال اجراست");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }

        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, channelId)
            .setContentTitle("📞 پل تماس فعال است")
            .setContentText("در حال دریافت دستورات از کامپیوتر...")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();

        startForeground(1, notification);
    }
}
