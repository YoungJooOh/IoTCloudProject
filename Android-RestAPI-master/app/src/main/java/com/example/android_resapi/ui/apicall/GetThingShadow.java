package com.example.android_resapi.ui.apicall;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.example.android_resapi.R;
import com.example.android_resapi.httpconnection.GetRequest;

public class GetThingShadow extends GetRequest {
    final static String TAG = "AndroidAPITest";
    String urlStr;
    public GetThingShadow(Activity activity, String urlStr) {
        super(activity);
        this.urlStr = urlStr;
    }

    @Override
    protected void onPreExecute() {
        try {
            Log.e(TAG, urlStr);
            url = new URL(urlStr);

        } catch (MalformedURLException e) {
            Toast.makeText(activity,"URL is invalid:"+urlStr, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            activity.finish();
        }
    }

    @Override
    protected void onPostExecute(String jsonString) {
        if (jsonString == null)
            return;
        Map<String, String> state = getStateFromJSONString(jsonString);
        TextView reported_ledTV = activity.findViewById(R.id.reported_waterpump);
        TextView reported_tempTV = activity.findViewById(R.id.reported_CDS);
        TextView reported_waterlevelTV = activity.findViewById(R.id.reported_waterlevel);
        reported_tempTV.setText(state.get("reported_CDS"));
        reported_ledTV.setText(state.get("reported_waterpump"));
        reported_waterlevelTV.setText(state.get("reported_water"));

        TextView desired_ledTV = activity.findViewById(R.id.desired_waterpump);
        TextView desired_tempTV = activity.findViewById(R.id.desired_CDS);
        TextView desired_waterlevelTV = activity.findViewById(R.id.desired_waterlevel);
        desired_tempTV.setText(state.get("reported_CDS"));
        desired_ledTV.setText(state.get("reported_waterpump"));
        desired_waterlevelTV.setText(state.get("reported_water"));

    }

    protected Map<String, String> getStateFromJSONString(String jsonString) {
        Map<String, String> output = new HashMap<>();
        try {
            // 처음 double-quote와 마지막 double-quote 제거
            jsonString = jsonString.substring(1,jsonString.length()-1);
            // \\\" 를 \"로 치환
            jsonString = jsonString.replace("\\\"","\"");
            Log.i(TAG, "jsonString="+jsonString);
            JSONObject root = new JSONObject(jsonString);
            JSONObject state = root.getJSONObject("state");
            JSONObject reported = state.getJSONObject("reported");
            String CDSValue = reported.getString("CDS");
            String waterpumpValue = reported.getString("waterpump");
            String waterlevelValue = reported.getString("water");
            String motorValue = reported.getString("motor");
            output.put("reported_CDS", CDSValue);
            output.put("reported_waterpump",waterpumpValue);
            output.put("reported_water",waterlevelValue);
            output.put("reported_motor",motorValue);

            JSONObject desired = state.getJSONObject("desired");
            String desired_CDSValue = desired.getString("CDS");
            String desired_waterpumpValue = desired.getString("waterpump");
            String desired_waterlevelValue = desired.getString("water");
            String desired_motorValue = desired.getString("motor");
            output.put("desired_CDS", desired_CDSValue);
            output.put("desired_waterpump",desired_waterpumpValue);
            output.put("desired_water",desired_waterlevelValue);
            output.put("desired_motor",desired_motorValue);

        } catch (JSONException e) {
            Log.e(TAG, "Exception in processing JSONString.", e);
            e.printStackTrace();
        }
        return output;
    }
}
