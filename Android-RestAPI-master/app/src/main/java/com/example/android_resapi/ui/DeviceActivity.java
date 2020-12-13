package com.example.android_resapi.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android_resapi.R;
import com.example.android_resapi.ui.apicall.GetThingShadow;
import com.example.android_resapi.ui.apicall.UpdateShadow;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceActivity extends AppCompatActivity {
    String urlStr;
    final static String TAG = "AndroidAPITest";
    Timer timer;
    Button startGetBtn;
    Button stopGetBtn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        Intent intent = getIntent();
        urlStr = intent.getStringExtra("thingShadowURL");

        startGetBtn = findViewById(R.id.startGetBtn);
        startGetBtn.setEnabled(true);
        startGetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        new GetThingShadow(DeviceActivity.this, urlStr).execute();
                    }
                },
                        0,2000);

                startGetBtn.setEnabled(false);
                stopGetBtn.setEnabled(true);
            }
        });

        stopGetBtn = findViewById(R.id.stopGetBtn);
        stopGetBtn.setEnabled(false);
        stopGetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timer != null)
                    timer.cancel();
                clearTextView();
                startGetBtn.setEnabled(true);
                stopGetBtn.setEnabled(false);
            }
        });

        Button updateBtn = findViewById(R.id.updateBtn);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText edit_motor = findViewById(R.id.edit_motor);
                EditText edit_waterpump = findViewById(R.id.edit_waterpump);


                JSONObject payload = new JSONObject();

                try {
                    JSONArray jsonArray = new JSONArray();
                    String CDS_input = edit_motor.getText().toString();
                    if (CDS_input != null && !CDS_input.equals("")) {
                        JSONObject tag1 = new JSONObject();
                        tag1.put("tagName", "CDS");
                        tag1.put("tagValue", CDS_input);

                        jsonArray.put(tag1);
                    }

                    String waterpump_input = edit_waterpump.getText().toString();
                    if (waterpump_input != null && !waterpump_input.equals("")) {
                        JSONObject tag2 = new JSONObject();
                        tag2.put("tagName", "waterpump");
                        tag2.put("tagValue", waterpump_input);

                        jsonArray.put(tag2);
                    }



                    if (jsonArray.length() > 0)
                        payload.put("tags", jsonArray);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONEXception");
                }
                Log.i(TAG,"payload="+payload);
                if (payload.length() >0 )
                    new UpdateShadow(DeviceActivity.this,urlStr).execute(payload);
                else
                    Toast.makeText(DeviceActivity.this,"변경할 상태 정보 입력이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void clearTextView() {
        TextView reported_waterpumpTV = findViewById(R.id.reported_waterpump);
        TextView reported_CDSTV = findViewById(R.id.reported_CDS);
        TextView reported_waterlevelTV = findViewById(R.id.reported_waterlevel);
        reported_CDSTV.setText("");
        reported_waterpumpTV.setText("");
        reported_waterlevelTV.setText("");

        TextView desired_waterpumpTV = findViewById(R.id.desired_waterpump);
        TextView desired_CDSTV = findViewById(R.id.desired_CDS);
        TextView desired_waterlevelTV = findViewById(R.id.desired_waterlevel);
        desired_CDSTV.setText("");
        desired_waterpumpTV.setText("");
        desired_waterlevelTV.setText("");
    }

}


