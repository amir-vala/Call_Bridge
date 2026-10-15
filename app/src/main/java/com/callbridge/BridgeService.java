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
    private static boolean running = false;

    private HttpServer httpServer;
    private PowerManager.WakeLock wakeLock;

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
        // ✅ شروع foreground با try-catch
        try {
            startAsForeground();
        } catch (Exception e) {
            Log.e(TAG, "خطا در startAsForeground: " + e.getMessage());
        }

        // ✅ جلوگیری از خواب
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CallBridge::WakeLock");
            wakeLock.acquire();
        } catch (Exception e) {
            Log.e(TAG, "خطا در wakeLock: " + e.getMessage());
        }

        // ✅ شروع سرور HTTP
        try {
            CallManager callManager = new CallManager(this);
            CallLogReader callLogReader = new CallLogReader(this);
            ContactResolver contactResolver = new ContactResolver(this);

            httpServer = new HttpServer(PORT, callManager, callLogReader, contactResolver);
            httpServer.start();

            running = true;
            Log.i(TAG, "سرور HTTP روی پورت " + PORT + " شروع شد");
        } catch (Exception e) {
            Log.e(TAG, "خطا در شروع سرور: " + e.getMessage());
            running = false;
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;

        try {
            if (httpServer != null) {
                httpServer.stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطا در توقف httpServer: " + e.getMessage());
        }

        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطا در release wakeLock: " + e.getMessage());
        }
        
        Log.i(TAG, "سرویس متوقف شد");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAsForeground() {
        String channelId = "call_bridge_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    channelId, 
                    "پل تماس", 
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("سرویس پل تماس در حال اجراست");
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) {
                    nm.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطا در ساخت channel: " + e.getMessage());
            }
        }

        try {
            Intent notifIntent = new Intent(this, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, channelId);
            } else {
                builder = new Notification.Builder(this);
            }

            Notification notification = builder
                .setContentTitle("📞 پل تماس فعال است")
                .setContentText("در حال دریافت دستورات از کامپیوتر...")
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

            // ✅ startForeground با try-catch
            startForeground(1, notification);
            
        } catch (Exception e) {
            Log.e(TAG, "خطا در startForeground: " + e.getMessage());
            // اگر foreground نشد، حداقل سرویس در پس‌زمینه کار می‌کند
        }
    }
}
