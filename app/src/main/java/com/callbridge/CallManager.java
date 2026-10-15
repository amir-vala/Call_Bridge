package com.callbridge;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.json.JSONObject;

public class CallManager {

    private static final String TAG = "CallManager";
    private final Context context;

    public CallManager(Context context) {
        this.context = context;
    }

    // ─── برقراری تماس مستقیم ───

    public String makeCall(String number) {
        try {
            // نرمال‌سازی شماره
            number = normalizeNumber(number);

            // تماس مستقیم با ACTION_CALL (بدون تأیید کاربر)
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            Log.i(TAG, "تماس مستقیم با: " + number);
            return success("تماس با " + number + " برقرار شد ✅");

        } catch (SecurityException e) {
            Log.e(TAG, "خطای دسترسی: " + e.getMessage());
            return error("دسترسی تماس داده نشده. در تنظیمات گوشی اجازه دهید.");

        } catch (Exception e) {
            Log.e(TAG, "خطا: " + e.getMessage());
            // فال‌بک: شماره‌گیری بدون تماس خودکار
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + number));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return success("شماره " + number + " آماده شد (تماس خودکار محدود شده)");
            } catch (Exception e2) {
                return error("خطا در برقراری تماس: " + e2.getMessage());
            }
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
            TelecomManager telecom = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                telecom.endCall();
                Log.i(TAG, "تماس قطع شد");
                return success("تماس قطع شد 🔴");
            } else {
                // فال‌بک برای اندرویدهای قدیمی‌تر
                Runtime.getRuntime().exec(new String[]{"input", "keyevent", "6"});
                return success("تماس قطع شد 🔴");
            }

        } catch (SecurityException e) {
            return error("دسترسی قطع تماس داده نشده");
        } catch (Exception e) {
            return error("خطا در قطع تماس: " + e.getMessage());
        }
    }

    // ─── پاسخ به تماس ورودی ───

    public String answerCall() {
        try {
            TelecomManager telecom = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= 26) {
                telecom.acceptRingingCall();
                Log.i(TAG, "تماس پاسخ داده شد");
                return success("تماس پاسخ داده شد 🟢");
            } else {
                Runtime.getRuntime().exec(new String[]{"input", "keyevent", "5"});
                return success("تماس پاسخ داده شد 🟢");
            }

        } catch (SecurityException e) {
            return error("دسترسی پاسخ تماس داده نشده");
        } catch (Exception e) {
            return error("خطا در پاسخ تماس: " + e.getMessage());
        }
    }

    // ─── وضعیت تماس ───

    public String getCallState() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int state = tm.getCallState();

            String stateStr;
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    stateStr = "ringing";
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    stateStr = "active";
                    break;
                default:
                    stateStr = "idle";
            }

            // گرفتن شماره تماس‌گیرنده (اگر موجود باشد)
            String number = "";
            String name = "";

            // برای تماس ورودی، آخرین لاگ را بخوان
            if (!"idle".equals(stateStr)) {
                CallLogReader reader = new CallLogReader(context);
                try {
                    org.json.JSONObject lastLog = reader.getLastCall();
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

        } catch (SecurityException e) {
            return "{\"state\":\"unknown\",\"error\":\"دسترسی لازم نیست\"}";
        } catch (Exception e) {
            return "{\"state\":\"unknown\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ─── اطلاعات گوشی ───

    public String getPhoneInfo() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("model", android.os.Build.MODEL);
            obj.put("manufacturer", android.os.Build.MANUFACTURER);
            obj.put("android_version", android.os.Build.VERSION.RELEASE);
            obj.put("sdk", android.os.Build.VERSION.SDK_INT);
            return obj.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ─── ابزارهای کمکی ───

    private String normalizeNumber(String num) {
        num = num.replaceAll("[^0-9+]", "");
        if (num.startsWith("+98")) {
            num = "0" + num.substring(3);
        } else if (num.startsWith("98") && num.length() == 12) {
            num = "0" + num.substring(2);
        }
        return num;
    }

    private String success(String msg) {
        return "{\"ok\":true,\"msg\":\"" + msg + "\"}";
    }

    private String error(String msg) {
        return "{\"ok\":false,\"msg\":\"" + msg + "\"}";
    }
}