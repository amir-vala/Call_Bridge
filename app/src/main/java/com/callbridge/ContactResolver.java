package com.callbridge;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class ContactResolver {

    private static final String TAG = "ContactResolver";
    private final Context context;

    public ContactResolver(Context context) {
        this.context = context;
    }

    // ─── پیدا کردن نام از روی شماره ───

    public String findNameByNumber(String number) {
        try {
            if (number == null || number.isEmpty()) {
                return "{\"name\":\"\"}";
            }

            // نرمال‌سازی
            String clean = number.replaceAll("[^0-9]", "");

            // جستجو در مخاطبین
            Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            );

            Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null, null, null
            );

            String name = "";
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0);
                }
                cursor.close();
            }

            // اگر پیدا نشد، با ۱۰ رقم آخر جستجو کن
            if (name.isEmpty() && clean.length() >= 10) {
                String last10 = clean.substring(clean.length() - 10);
                uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(last10)
                );
                cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null
                );
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0);
                    }
                    cursor.close();
                }
            }

            JSONObject obj = new JSONObject();
            obj.put("name", name != null ? name : "");
            obj.put("number", number);
            return obj.toString();

        } catch (Exception e) {
            Log.e(TAG, "خطا: " + e.getMessage());
            return "{\"name\":\"\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ─── خواندن همه مخاطبین ───

    public String getAllContacts() {
        try {
            JSONArray arr = new JSONArray();

            Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null) {
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                while (cursor.moveToNext()) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", nameIdx >= 0 ? cursor.getString(nameIdx) : "");
                    obj.put("number", numIdx >= 0 ? cursor.getString(numIdx) : "");
                    arr.put(obj);
                }
                cursor.close();
            }

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("contacts", arr);
            result.put("count", arr.length());
            return result.toString();

        } catch (Exception e) {
            return "{\"ok\":false,\"contacts\":[],\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}