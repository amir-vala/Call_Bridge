package com.callbridge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int PERM_REQ = 100;

    private TextView statusText;
    private TextView ipText;
    private Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);
            
            statusText = findViewById(R.id.statusText);
            ipText = findViewById(R.id.ipText);
            startBtn = findViewById(R.id.startBtn);

            startBtn.setOnClickListener(v -> {
                try {
                    if (!BridgeService.isRunning()) {
                        checkPermissions();
                    } else {
                        stopBridge();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "خطا در کلیک: " + e.getMessage());
                    Toast.makeText(this, "خطا: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            updateUI();
            Log.i(TAG, "Activity ساخته شد ✅");
            
        } catch (Exception e) {
            Log.e(TAG, "خطا در onCreate: " + e.getMessage());
            Toast.makeText(this, "خطا: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            updateUI();
        } catch (Exception e) {
            Log.e(TAG, "خطا در onResume: " + e.getMessage());
        }
    }

    private void checkPermissions() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= 33) {
            perms = new String[]{
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            perms = new String[]{
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS
            };
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
                Toast.makeText(this, "⚠️ دسترسی‌ها لازم است", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startBridge() {
        try {
            Intent intent = new Intent(this, BridgeService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            updateUI();
            Toast.makeText(this, "✅ سرویس شروع شد", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "خطا در startBridge: " + e.getMessage());
            Toast.makeText(this, "خطا: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopBridge() {
        try {
            Intent intent = new Intent(this, BridgeService.class);
            stopService(intent);
            updateUI();
            Toast.makeText(this, "⏹ سرویس متوقف شد", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "خطا در stopBridge: " + e.getMessage());
        }
    }

    private void updateUI() {
        try {
            if (BridgeService.isRunning()) {
                statusText.setText("🟢 سرویس فعال است");
                statusText.setTextColor(0xFF4CAF50);
                startBtn.setText("⏹  توقف سرویس");
                ipText.setText("آدرس‌های اتصال:\n\n" + getConnectionInfo());
            } else {
                statusText.setText("🔴 سرویس متوقف است");
                statusText.setTextColor(0xFFE53935);
                startBtn.setText("▶️  شروع سرویس");
                ipText.setText("");
            }
        } catch (Exception e) {
            Log.e(TAG, "خطا در updateUI: " + e.getMessage());
        }
    }

    private String getConnectionInfo() {
        try {
            StringBuilder sb = new StringBuilder();
            String wifiIp = getLocalIp();
            
            if (wifiIp != null) {
                sb.append("📶 WiFi:\n");
                sb.append("http://").append(wifiIp).append(":8765\n\n");
            }

            sb.append("🔌 کابل USB:\n");
            sb.append("USB Tethering را فعال کنید\n");
            sb.append("آدرس: http://192.168.42.129:8765\n");

            return sb.toString();
        } catch (Exception e) {
            return "خطا در گرفتن IP";
        }
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
            Log.e(TAG, "خطا در getLocalIp: " + e.getMessage());
        }
        return null;
    }
}
