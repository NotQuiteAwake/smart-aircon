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

// TODO: replace built-in networking with okhttp3
// TODO: replace org.json.* with com.alibaba.fastjson.*
public class Networking {
    MediaType JSON = MediaType.parse("application/json;charset=utf-8");
    private static int PORT_NUMBER = 8080;
    private static String IP_ADDRESS = "";
    private static final String CLASS_TAG = "Networking";

    Networking(String ip_address, int port_number) { IP_ADDRESS = ip_address; PORT_NUMBER = port_number; }

    Networking(int port_number) { PORT_NUMBER = port_number; }

    public void setIp(String ip_address) {
        IP_ADDRESS = ip_address;
    }

    public String getIp() { return IP_ADDRESS; }

    public boolean checkIP(String ip) {
        ip = "http://" + ip;
        HttpURLConnection conn = null;
        InputStream is = null;
        String resultData = "";
        try {
            URL url = new URL(ip + ":" + PORT_NUMBER);
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
                Log.i(CLASS_TAG, "resultData: " + resultData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            JSONObject json = new JSONObject(resultData);
            if (json.getString("description").equals("smart-air-conditioner")) return true;
        } catch (JSONException e) { return false; }
        return false;
    }

    private void postRequest(final String urlstr, final JSONObject json) {
        OkHttpClient httpClient = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(JSON, String.valueOf(json));
        final String urlStr = urlstr + ":" + PORT_NUMBER;
        try {
            Request request = new Request.Builder()
                    .url(urlStr + "/post_tasks")
                    .post(requestBody)
                    .build();
            Response response = httpClient.newCall(request).execute();
            // TODO: throws exception regarding the response.
        } catch (IllegalArgumentException e) { e.printStackTrace();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private String getData(final String urlstr) {
        HttpURLConnection conn = null;
        InputStream is = null;
        String resultData = "";
        final String urlStr = urlstr + ":" + PORT_NUMBER;
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
                Log.i("getData:", "resultData: " + resultData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultData;
    }

    public int setExpTime(double exp_time) {
        JSONObject req = new JSONObject();

        try {
            req.put("cmd", "setExpTime");
            req.put("exp_time", exp_time);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        postRequest(IP_ADDRESS, req);
        String res = getData(IP_ADDRESS);
        int status = -1;

        try {
            JSONObject json_res = new JSONObject(res);
            status = json_res.getInt("status");
        } catch (JSONException e) { e.printStackTrace(); }

        return status;
    }

    public List<Double> getExpTemp(String person_id) {
        JSONObject req = new JSONObject();
        List<Double> exp_temp = new ArrayList<>();
        try {
            req.put("cmd", "get_exp_temp");
            Log.i(CLASS_TAG, person_id);
            req.put("person_id", person_id);
            Log.i(CLASS_TAG, req.toString());
            postRequest(IP_ADDRESS, req);
            JSONObject json = new JSONObject(getData(IP_ADDRESS));
            exp_temp = strToDouble(jsonToArray(json.getJSONArray("exp_temp")));
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(CLASS_TAG, "getExpTemp() JSON error");
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(CLASS_TAG, "getExpTemp() NullPointerException");
        }
        return exp_temp;
    }

    public void setExpTemp(String person_id, int exp_time, double exp_temp) {
        try {
            JSONObject req = new JSONObject();
            req.put("cmd", "set_exp_temp");
            req.put("person_id", person_id);
            req.put("exp_time", exp_time);
            req.put("exp_temp", exp_temp);
            postRequest(IP_ADDRESS, req);
            getData(IP_ADDRESS);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    // @return the JSON-formatted stat String.
    public String getStatJson() {
        JSONObject req = new JSONObject();
        try {
            req.put("cmd", "get_stat");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        postRequest(IP_ADDRESS, req);
        return getData(IP_ADDRESS);
    }

    // @return a formatted String with lines of stats.
    public String getStat() {
        String res = getStatJson();
        String output = "";
        try {
            JSONObject json_rep = new JSONObject(res);
            int status = json_rep.getInt("status");
            if (status == 1) {
                List<String> temp = jsonToArray(json_rep.getJSONArray("temp"));
                List<String> exp = jsonToArray(json_rep.getJSONArray("exp_temp"));
                int time = json_rep.getInt("time");
                int p_time = json_rep.getInt("p_time");
                output = "";
                output = output + "Current time:\n" + time + "\n";
                output = output + "Temperature records:\n" + String.join(",", temp) + "\n";
                output = output + "Expected Temperature:\n" + String.join(",", exp) + "\n";
                output = output + "Next predicted turn-on time:\n" + p_time + "\n";
            } else if (status == -1) {
                output = "Fetch failed: " + status;
            }
        } catch (JSONException e){ e.printStackTrace(); /*I guess that I forgive you*/ }
        return output;
    }

    public boolean getInitState() {
        boolean isInitialized = false;
        try {
            JSONObject req = new JSONObject();
            req.put("cmd", "check_init_state");
            postRequest(IP_ADDRESS, req);
            JSONObject response = new JSONObject(getData(IP_ADDRESS));
            if (response.getInt("status") == -1) { Log.e(CLASS_TAG, "update_init_state failed to fetch state."); }
            else {
                int init_state_int = response.getInt("init_state");
                if (init_state_int > 0) { isInitialized = true; }
                else isInitialized = false;
            }
        } catch (JSONException e) { e.printStackTrace(); }
        return isInitialized;
    }

    // TODO: open a specialized API for get_temp
    public List<Double> getTemp() {
        JSONObject req = new JSONObject();
        List<Double> res = new ArrayList<>();
        try {
            req.put("cmd", "get_stat");
            postRequest(IP_ADDRESS, req);
            List<String> temp = new ArrayList<>();
            JSONObject data = new JSONObject(getData(IP_ADDRESS));
            res = strToDouble(jsonToArray(data.getJSONArray("temp")));
        } catch (JSONException e) { e.printStackTrace(); }
        return res;
    }

    public int getNextTime() {
        int next_time = 0;
        try {
            JSONObject json = new JSONObject(getStatJson());
            next_time = json.getInt("p_time");
        }
        catch (JSONException e) { e.printStackTrace(); }
        return next_time;
    }

    public List<String> getMemberList() {
        JSONObject req = new JSONObject();
        try { req.put("cmd", "get_member_list"); }
        catch (JSONException e) { e.printStackTrace(); }
        postRequest(IP_ADDRESS, req);
        String res = getData(IP_ADDRESS);
        // TODO: variable name unification
        List<String> member_list = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(res);
            member_list = jsonToArray(json.getJSONArray("member_list"));
        } catch (JSONException e) { e.printStackTrace(); }
        Log.i(CLASS_TAG, "member_list: " + member_list);
        return member_list;
    }

    public void setPriority(String person_id, int priority) {
        JSONObject req = new JSONObject();
        try {
            req.put("cmd", "set_user_priority");
            req.put("person_id", person_id);
            req.put("priority", priority);
            postRequest(IP_ADDRESS, req);
            String result = getData(IP_ADDRESS);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public String getUserJson(String person_id) {
        String res = "";
        try {
            JSONObject req = new JSONObject();
            req.put("cmd", "get_user");
            req.put("person_id", person_id);
            postRequest(IP_ADDRESS, req);
            res = getData(IP_ADDRESS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return res;
    }

    public void setPresence(String person_id, boolean isPresent) {
        try {
            JSONObject req = new JSONObject();
            req.put("cmd", "set_user_presence");
            req.put("person_id", person_id);
            req.put("presence", isPresent ? 1 : 0);
            postRequest(IP_ADDRESS, req);
            String result = getData(IP_ADDRESS);
        } catch (JSONException e) { e.printStackTrace(); }
    }

    public void setState(String person_id, String state_id) {
        JSONObject req = new JSONObject();
        try {
            req.put("cmd", "set_user_state");
            req.put("person_id", person_id);
            req.put("state_id", state_id);
            postRequest(IP_ADDRESS, req);
            String result = getData(IP_ADDRESS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setName(String person_id, String name) {
        JSONObject req = new JSONObject();
        try {
            req.put("cmd", "set_name");
            req.put("person_id", person_id);
            req.put("name", name);
            postRequest(IP_ADDRESS, req);
            String result = getData(IP_ADDRESS);
        } catch(JSONException e) { e.printStackTrace(); }
    }

    public List<String> getStateList() {
        List<String> list = new ArrayList<>();
        JSONObject req = new JSONObject();
        try {
            req.put("cmd", "get_state_list");
            postRequest(IP_ADDRESS, req);
            list = jsonToArray(new JSONObject(getData(IP_ADDRESS)).getJSONArray("state_list"));
        } catch (JSONException e) { e.printStackTrace(); }
        return list;
    }

    private List<String> jsonToArray(JSONArray jArray) {
        List<String> list = new ArrayList<String>();
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); i++) {
                try { list.add(jArray.getString(i)); } catch (JSONException e) { e.printStackTrace(); }
            }
        }
        return list;
    }

    private List<Double> strToDouble(List<String> strl) {
        List<Double> doul = new ArrayList<>();
        for (int i = 0; i < strl.size(); i++) {
            doul.add(Double.valueOf(strl.get(i)));
        }
        return doul;
    }
}
