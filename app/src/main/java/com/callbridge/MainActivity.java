package com.callbridge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class MainActivity extends Activity {

    private static final int PERM_REQ = 100;
    private static final int OVERLAY_REQ = 101;

    private TextView statusText;
    private TextView ipText;
    private Button startBtn;
    private boolean serviceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        ipText = findViewById(R.id.ipText);
        startBtn = findViewById(R.id.startBtn);

        startBtn.setOnClickListener(v -> {
            if (!serviceRunning) {
                checkPermissions();
            } else {
                stopBridge();
            }
        });

        // بررسی وضعیت سرویس
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    // ─── بررسی و درخواست پرمیشن‌ها ───

    private void checkPermissions() {
        String[] perms = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        };

        // Android 13+ نیاز به پرمیشن نوتیفیکیشن دارد
        if (Build.VERSION.SDK_INT >= 33) {
            perms = java.util.Arrays.copyOf(perms, perms.length + 1);
            perms[perms.length - 1] = Manifest.permission.POST_NOTIFICATIONS;
        }

        boolean allGranted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startBridge();
        } else {
            ActivityCompat.requestPermissions(this, perms, PERM_REQ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQ) {
            boolean all = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    all = false;
                    break;
                }
            }
            if (all) {
                startBridge();
            } else {
                Toast.makeText(this, "⚠️ برای کارکرد صحیح، همه دسترسی‌ها لازم است", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ─── شروع و توقف سرویس ───

    private void startBridge() {
        Intent intent = new Intent(this, BridgeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        serviceRunning = true;
        updateUI();
        Toast.makeText(this, "✅ سرویس شروع شد", Toast.LENGTH_SHORT).show();
    }

    private void stopBridge() {
        Intent intent = new Intent(this, BridgeService.class);
        stopService(intent);
        serviceRunning = false;
        updateUI();
        Toast.makeText(this, "⏹ سرویس متوقف شد", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        if (BridgeService.isRunning()) {
            serviceRunning = true;
            statusText.setText("🟢 سرویس فعال است");
            statusText.setTextColor(0xFF4CAF50);
            startBtn.setText("⏹  توقف سرویس");
            startBtn.setBackgroundColor(0xFFE53935);
            ipText.setText("آدرس‌های اتصال:\n\n" + getConnectionInfo());
        } else {
            serviceRunning = false;
            statusText.setText("🔴 سرویس متوقف است");
            statusText.setTextColor(0xFFE53935);
            startBtn.setText("▶️  شروع سرویس");
            startBtn.setBackgroundColor(0xFF1976D2);
            ipText.setText("");
        }
    }

    // ─── اطلاعات اتصال ───

    private String getConnectionInfo() {
        StringBuilder sb = new StringBuilder();

        // IP WiFi
        String wifiIp = getLocalIp();
        if (wifiIp != null) {
            sb.append("📶 WiFi:\n");
            sb.append("http://").append(wifiIp).append(":8765\n\n");
        }

        // IP USB Tethering
        sb.append("🔌 کابل USB:\n");
        sb.append("۱. کابل را وصل کنید\n");
        sb.append("۲. تنظیمات ← شبکه ← نقطه اتصال ← اتصال با کابل ✅\n");
        sb.append("۳. آدرس: http://192.168.42.129:8765\n");

        return sb.toString();
    }

    private String getLocalIp() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (intf.getName().startsWith("wlan")) {
                    for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}