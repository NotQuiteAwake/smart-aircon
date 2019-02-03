package com.ota.jimmychen.ota;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Networking {
    MediaType JSON = MediaType.parse("application/json;charset=utf-8");

    public void post_request(final String urlStr, final JSONObject json) {
        OkHttpClient httpClient = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(JSON, String.valueOf(json));
        try {
            Request request = new Request.Builder()
                    .url(urlStr + "/post_tasks")
                    .post(requestBody)
                    .build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    //Toast.makeText(MainActivity.this, "POST failed", Toast.LENGTH_SHORT);
                    //              showToast("POST Failed");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String repStr = response.body().string();
                    if (repStr != null) {
                        try {
                            JSONObject repJSON = new JSONObject(repStr);
                            int status = repJSON.getInt("status");
                            if (status == 1) {
                                //Toast.makeText(MainActivity.this, "Submit Successful(1)", Toast.LENGTH_SHORT).show()
                                // showToast("successful.");
                            } else if (status == -1) {
                                //Toast.makeText(MainActivity.this, "Submit Failed(-1)", Toast.LENGTH_SHORT).show();
                                // showToast("failed.");
                            }
                        } catch (JSONException e) { /*And what can I possibly say?*/ }
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            //Toast.makeText(MainActivity.this, "Illegal Address.", Toast.LENGTH_SHORT).show();
            // showToast("illegal address");
        }
    }

    public String get_data(final String urlStr) {
        HttpURLConnection conn = null;
        InputStream is = null;
        String resultData = "";
        try {
            URL url = new URL(urlStr + "/data_fetch");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                is = conn.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader bufferReader = new BufferedReader(isr);
                String inputLine = "";
                while ((inputLine = bufferReader.readLine()) != null) {
                    resultData += inputLine + "\n";
                }
                Log.i("get_data:", "resultData: " + resultData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultData;
    }

    public List<String> json_to_array(JSONArray jArray) {
        List<String> list = new ArrayList<String>();
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); i++) {
                try { list.add(jArray.getString(i)); } catch (JSONException e) { e.printStackTrace(); }
            }
        }
        return list;
    }

    public List<Double> str_to_double(List<String> strl) {
        List<Double> doul = new ArrayList<>();
        for (int i = 0; i < strl.size(); i++) {
            doul.add(Double.valueOf(strl.get(i)));
        }
        return doul;
    }
}
