package com.callbridge;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class CallLogReader {

    private static final String TAG = "CallLogReader";
    private final Context context;

    public CallLogReader(Context context) {
        this.context = context;
    }

    // ─── خواندن لاگ تماس‌ها ───

    public String getCallLog(int limit) {
        try {
            JSONArray arr = new JSONArray();

            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC LIMIT " + limit
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    JSONObject obj = new JSONObject();

                    obj.put("number", getStr(cursor, CallLog.Calls.NUMBER));
                    obj.put("name", getStr(cursor, CallLog.Calls.CACHED_NAME));
                    obj.put("date", getLong(cursor, CallLog.Calls.DATE));
                    obj.put("duration", getLong(cursor, CallLog.Calls.DURATION));

                    int type = getInt(cursor, CallLog.Calls.TYPE);
                    switch (type) {
                        case CallLog.Calls.INCOMING_TYPE:
                            obj.put("type", "incoming");
                            obj.put("dir", "in");
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            obj.put("type", "outgoing");
                            obj.put("dir", "out");
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            obj.put("type", "missed");
                            obj.put("dir", "missed");
                            break;
                        case CallLog.Calls.REJECTED_TYPE:
                            obj.put("type", "rejected");
                            obj.put("dir", "rejected");
                            break;
                        default:
                            obj.put("type", "unknown");
                            obj.put("dir", "unknown");
                    }

                    arr.put(obj);
                }
                cursor.close();
            }

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("logs", arr);
            return result.toString();

        } catch (SecurityException e) {
            return "{\"ok\":false,\"error\":\"دسترسی به لاگ تماس داده نشده\",\"logs\":[]}";
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + e.getMessage() + "\",\"logs\":[]}";
        }
    }

    // ─── آخرین تماس ───

    public JSONObject getLastCall() {
        try {
            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC LIMIT 1"
            );

            if (cursor != null && cursor.moveToFirst()) {
                JSONObject obj = new JSONObject();
                obj.put("number", getStr(cursor, CallLog.Calls.NUMBER));
                obj.put("name", getStr(cursor, CallLog.Calls.CACHED_NAME));
                obj.put("date", getLong(cursor, CallLog.Calls.DATE));
                obj.put("type", getInt(cursor, CallLog.Calls.TYPE));
                cursor.close();
                return obj;
            }

            if (cursor != null) cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "خطا در خواندن آخرین تماس: " + e.getMessage());
        }
        return null;
    }

    // ─── ابزارهای کمکی ───

    private String getStr(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 && !c.isNull(idx) ? c.getString(idx) : "";
    }

    private long getLong(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getLong(idx) : 0;
    }

    private int getInt(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getInt(idx) : 0;
    }
}