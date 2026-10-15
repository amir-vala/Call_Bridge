package com.callbridge;

import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class HttpServer extends NanoHTTPD {

    private static final String TAG = "HttpServer";
    private final CallManager callManager;
    private final CallLogReader callLogReader;
    private final ContactResolver contactResolver;

    public HttpServer(int port, CallManager cm, CallLogReader clr, ContactResolver cr) {
        super(port);
        this.callManager = cm;
        this.callLogReader = clr;
        this.contactResolver = cr;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        String response;

        try {
            Log.d(TAG, method + " " + uri);

            // ─── GET ───
            if (Method.GET.equals(method)) {
                switch (uri) {
                    case "/":
                        response = json("ok", true, "msg", "پل تماس فعال است ✅");
                        break;

                    case "/api/state":
                        response = callManager.getCallState();
                        break;

                    case "/api/call_log":
                        Map<String, String> params = decodeParams(session);
                        int limit = 20;
                        if (params.containsKey("limit")) {
                            try { limit = Integer.parseInt(params.get("limit")); } catch (Exception e) {}
                        }
                        response = callLogReader.getCallLog(limit);
                        break;

                    case "/api/contacts":
                        response = contactResolver.getAllContacts();
                        break;

                    case "/api/contact_name":
                        Map<String, String> p2 = decodeParams(session);
                        String num = p2.getOrDefault("number", "");
                        response = contactResolver.findNameByNumber(num);
                        break;

                    case "/api/phone_info":
                        response = callManager.getPhoneInfo();
                        break;

                    default:
                        response = json("error", "مسیر نامعتبر: " + uri);
                }
            }

            // ─── POST ───
            else if (Method.POST.equals(method)) {
                // خواندن body
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String body = files.getOrDefault("postData", "{}");
                JSONObject data;
                try {
                    data = new JSONObject(body);
                } catch (Exception e) {
                    data = new JSONObject();
                }

                switch (uri) {
                    case "/api/call": {
                        String number = data.optString("number", "");
                        if (number.isEmpty()) {
                            response = json("ok", false, "msg", "شماره وارد نشده");
                        } else {
                            response = callManager.makeCall(number);
                        }
                        break;
                    }

                    case "/api/hangup":
                        response = callManager.hangupCall();
                        break;

                    case "/api/answer":
                        response = callManager.answerCall();
                        break;

                    case "/api/dial": {
                        // فقط شماره‌گیری بدون تماس خودکار
                        String number = data.optString("number", "");
                        response = callManager.dialNumber(number);
                        break;
                    }

                    default:
                        response = json("error", "مسیر نامعتبر: " + uri);
                }
            }

            // ─── OPTIONS (CORS) ───
            else if (Method.OPTIONS.equals(method)) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{}");
            }

            else {
                response = json("error", "متد پشتیبانی نمی‌شود");
            }

        } catch (Exception e) {
            Log.e(TAG, "خطا: " + e.getMessage());
            response = json("error", e.getMessage());
        }

        Response r = newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", response);
        r.addHeader("Access-Control-Allow-Origin", "*");
        r.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        r.addHeader("Access-Control-Allow-Headers", "Content-Type");
        return r;
    }

    private Map<String, String> decodeParams(IHTTPSession session) {
        Map<String, String> result = new HashMap<>();
        Map<String, String> query = session.getParms();
        if (query != null) result.putAll(query);
        return result;
    }

    private String json(Object... kv) {
        try {
            JSONObject obj = new JSONObject();
            for (int i = 0; i < kv.length - 1; i += 2) {
                obj.put(String.valueOf(kv[i]), kv[i + 1]);
            }
            return obj.toString();
        } catch (Exception e) {
            return "{\"error\":\"json error\"}";
        }
    }
}