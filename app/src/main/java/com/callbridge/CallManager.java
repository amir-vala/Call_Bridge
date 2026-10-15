package com.callbridge;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.json.JSONObject;

public class CallManager {

    private static final String TAG = "CallManager";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public CallManager(Context context) {
        this.context = context;
    }

    // ─── برقراری تماس با منطق هوشمند ───

    public String makeCall(String number) {
        try {
            number = normalizeNumber(number);
            final String finalNumber = number;
            
            Log.i(TAG, "🎯 شروع تماس با: " + number);

            // ═══════════════════════════════════════
            // مرحله ۱: تلاش برای تماس مستقیم (سریع)
            // ═══════════════════════════════════════
            
            boolean directCallAttempted = false;
            
            // روش ۱.۱: TelecomManager.placeCall (اندروید ۶+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    TelecomManager telecom = (TelecomManager) 
                        context.getSystemService(Context.TELECOM_SERVICE);
                    if (telecom != null) {
                        Bundle extras = new Bundle();
                        telecom.placeCall(Uri.parse("tel:" + finalNumber), extras);
                        Log.i(TAG, "✅ placeCall موفق - تماس مستقیم برقرار شد");
                        return success("تماس با " + finalNumber + " برقرار شد ✅");
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "⚠️ placeCall بلاک شد (Security): " + e.getMessage());
                    directCallAttempted = true;
                } catch (Exception e) {
                    Log.w(TAG, "⚠️ placeCall شکست خورد: " + e.getMessage());
                    directCallAttempted = true;
                }
            }

            // روش ۱.۲: Intent.ACTION_CALL (اگر placeCall کار نکرد)
            if (!directCallAttempted) {
                try {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + finalNumber));
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                    context.startActivity(callIntent);
                    Log.i(TAG, "✅ ACTION_CALL موفق - تماس مستقیم برقرار شد");
                    return success("تماس با " + finalNumber + " برقرار شد ✅");
                } catch (SecurityException e) {
                    Log.w(TAG, "⚠️ ACTION_CALL بلاک شد (Security): " + e.getMessage());
                    directCallAttempted = true;
                } catch (Exception e) {
                    Log.w(TAG, "⚠️ ACTION_CALL شکست خورد: " + e.getMessage());
                    directCallAttempted = true;
                }
            }

            // ═══════════════════════════════════════
            // مرحله ۲: ترفند DIAL + Keyevent (نجات‌دهنده!)
            // ═══════════════════════════════════════
            
            Log.i(TAG, "🔄 تماس مستقیم کار نکرد، استفاده از ترفند...");
            
            try {
                // گام ۲.۱: باز کردن شماره‌گیر با شماره از پیش پر شده
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + finalNumber));
                dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                dialIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                context.startActivity(dialIntent);
                Log.i(TAG, "✅ شماره‌گیر باز شد");
                
                // گام ۲.۲: بعد از ۸۰۰ میلی‌ثانیه، دکمه سبز تماس را فشار بده
                handler.postDelayed(() -> {
                    try {
                        // KEYCODE_CALL = 5 (دکمه سبز تماس)
                        Runtime.getRuntime().exec(new String[]{"input", "keyevent", "5"});
                        Log.i(TAG, "✅ KEYCODE_CALL ارسال شد - تماس برقرار شد");
                    } catch (Exception e) {
                        Log.e(TAG, "❌ خطا در ارسال keyevent: " + e.getMessage());
                    }
                }, 800);
                
                return success("تماس با " + finalNumber + " برقرار شد (با تأخیر ۱ ثانیه) ✅");
                
            } catch (Exception e) {
                Log.e(TAG, "❌ همه روش‌ها شکست خوردند: " + e.getMessage());
                return error("خطا در برقراری تماس: " + e.getMessage());
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ خطای کلی: " + e.getMessage());
            return error("خطا: " + e.getMessage());
        }
    }

    // ─── فقط شماره‌گیری (بدون تماس خودکار) ───

    public String dialNumber(String number) {
        try {
            number = normalizeNumber(number);
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return success("شماره " + number + " آماده شد");
        } catch (Exception e) {
            return error("خطا: " + e.getMessage());
        }
    }

    // ─── قطع تماس ───

    public String hangupCall() {
        try {
            // روش ۱: TelecomManager (سریع‌ترین)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                TelecomManager telecom = (TelecomManager) 
                    context.getSystemService(Context.TELECOM_SERVICE);
                if (telecom != null) {
                    try {
                        telecom.endCall();
                        Log.i(TAG, "✅ endCall موفق");
                        return success("تماس قطع شد 🔴");
                    } catch (Exception e) {
                        Log.w(TAG, "⚠️ endCall کار نکرد: " + e.getMessage());
                    }
                }
            }
            
            // روش ۲: Keyevent (KEYCODE_ENDCALL = 6)
            Runtime.getRuntime().exec(new String[]{"input", "keyevent", "6"});
            Log.i(TAG, "✅ KEYCODE_ENDCALL ارسال شد");
            return success("تماس قطع شد 🔴");
            
        } catch (Exception e) {
            return error("خطا: " + e.getMessage());
        }
    }

    // ─── پاسخ به تماس ورودی ───

    public String answerCall() {
        try {
            // روش ۱: TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TelecomManager telecom = (TelecomManager) 
                    context.getSystemService(Context.TELECOM_SERVICE);
                if (telecom != null) {
                    try {
                        telecom.acceptRingingCall();
                        Log.i(TAG, "✅ acceptRingingCall موفق");
                        return success("تماس پاسخ داده شد 🟢");
                    } catch (Exception e) {
                        Log.w(TAG, "⚠️ acceptRingingCall کار نکرد: " + e.getMessage());
                    }
                }
            }
            
            // روش ۲: Keyevent (KEYCODE_CALL = 5)
            Runtime.getRuntime().exec(new String[]{"input", "keyevent", "5"});
            Log.i(TAG, "✅ KEYCODE_CALL ارسال شد");
            return success("تماس پاسخ داده شد 🟢");
            
        } catch (Exception e) {
            return error("خطا: " + e.getMessage());
        }
    }

    // ─── وضعیت تماس ───

    public String getCallState() {
        try {
            TelephonyManager tm = (TelephonyManager) 
                context.getSystemService(Context.TELEPHONY_SERVICE);
            int state = tm.getCallState();

            String stateStr;
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING: stateStr = "ringing"; break;
                case TelephonyManager.CALL_STATE_OFFHOOK: stateStr = "active"; break;
                default: stateStr = "idle";
            }

            String number = "", name = "";
            if (!"idle".equals(stateStr)) {
                CallLogReader reader = new CallLogReader(context);
                try {
                    JSONObject lastLog = reader.getLastCall();
                    if (lastLog != null) {
                        number = lastLog.optString("number", "");
                        name = lastLog.optString("name", "");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "خطا در خواندن لاگ: " + e.getMessage());
                }
            }

            JSONObject obj = new JSONObject();
            obj.put("state", stateStr);
            obj.put("number", number);
            obj.put("name", name);
            return obj.toString();

        } catch (Exception e) {
            return "{\"state\":\"unknown\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ─── اطلاعات گوشی ───

    public String getPhoneInfo() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("model", Build.MODEL);
            obj.put("manufacturer", Build.MANUFACTURER);
            obj.put("android_version", Build.VERSION.RELEASE);
            obj.put("sdk", Build.VERSION.SDK_INT);
            return obj.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ─── ابزارها ───

    private String normalizeNumber(String num) {
        num = num.replaceAll("[^0-9+]", "");
        if (num.startsWith("+98")) num = "0" + num.substring(3);
        else if (num.startsWith("98") && num.length() == 12) num = "0" + num.substring(2);
        return num;
    }

    private String success(String msg) {
        return "{\"ok\":true,\"msg\":\"" + msg + "\"}";
    }

    private String error(String msg) {
        return "{\"ok\":false,\"msg\":\"" + msg + "\"}";
    }
}
