package com.example.francescop.projectnes;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    Dialog loadingDialog;
    FloatingActionButton fab;
    String mEmail;
    Button searchButton, logoutButton;
    TextView tempMeasured, lumMeasured;
    TextView tempTV, lumTV, roomTV, prevOpinions;
    String room, measuredTemp, measuredLum;
    SeekBar tempSB, lumSB;
    ScrollView mainView;
    CoordinatorLayout layout;

    Timer timer;
    TimerTask task;
    final String temp[] = {"(-3) Very Cold",
                "(-2) Cold",
                "(-1) Chill",
                "(0) Good",
                "(+1) Warm",
                "(+2) Hot",
                "(+3) Very Hot"},
         lum[] = {"(-3) Gloomy",
                 "(-2) Dark",
                 "(-1) Few light",
                 "(0) Good",
                 "(+1) Intense light",
                 "(+2) Bright light",
                 "(+3) Dazzling light"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mEmail = getIntent().getStringExtra("username");

        mainView = (ScrollView) findViewById(R.id.scrollView);
        layout = (CoordinatorLayout)findViewById(R.id.rootlayout);

        prevOpinions = (TextView) findViewById(R.id.prevTV);

        searchButton = (Button) findViewById(R.id.buttonSearch);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes

                    startActivityForResult(intent, 0);

                } catch (Exception e) {

                    Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
                    startActivity(marketIntent);

                }
            }
        });

        logoutButton = (Button) findViewById(R.id.buttonLeave);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CheckRoomTask("OUT").execute();
            }
        });
        logoutButton.setEnabled(false);

        roomTV = (TextView) findViewById(R.id.textViewRoomVal);

        tempMeasured = (TextView) findViewById(R.id.textViewTempVal);
        lumMeasured = (TextView) findViewById(R.id.textViewLumVal);

        tempTV = (TextView) findViewById(R.id.textViewTempPar);
        tempTV.setText(temp[3]);
        tempSB = (SeekBar) findViewById(R.id.seekBarTemp);
        tempSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 3;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                tempTV.setText(temp[progress]);
            }
        });

        lumTV = (TextView) findViewById(R.id.textViewLumPar);
        lumTV.setText(lum[3]);
        lumSB = (SeekBar) findViewById(R.id.seekBarLum);
        lumSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 3;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                lumTV.setText(lum[progress]);
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SubmitOpinionTask(measuredTemp, tempSB.getProgress()-3,
                        measuredLum, lumSB.getProgress()-3).execute();
            }
        });
        fab.setVisibility(View.INVISIBLE);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(loadingDialog!=null && loadingDialog.isShowing()) loadingDialog.dismiss();
        if(timer!=null){
            timer.cancel();
            timer.purge();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                room = contents;

                tempTV.setText(temp[3]);
                lumTV.setText(lum[3]);
                tempSB.setProgress(3);
                lumSB.setProgress(3);
                tempMeasured.setText("");
                lumMeasured.setText("");

                new CheckRoomTask("IN").execute();
            }
            if(resultCode == RESULT_CANCELED){
                //handle cancel
                Snackbar.make(layout, "QR code result canceled", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }
    }

    private class CheckRoomTask extends AsyncTask<Void, Void, String> {
        String access;

        CheckRoomTask(String access) {
            this.access = access;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingDialog = ProgressDialog.show(MainActivity.this,
                    "Logging in", "Wait for the server...");
        }

        @Override
        protected String doInBackground(Void... args) {
            Map<String,String> nameValuePairs = new HashMap<>();
            nameValuePairs.put("username", mEmail);
            nameValuePairs.put("room", room);
            nameValuePairs.put("check", access);

            ServiceHandler jsonParser = new ServiceHandler();
            String result = jsonParser.makeServiceCall("checkRoom.php", nameValuePairs);
            Log.d("***CheckRoomTask","result: "+result);

            return result;
        }

        @Override
        protected void onPostExecute(String result){
            if(loadingDialog.isShowing()) loadingDialog.dismiss();

            if(result!=null && result.contains("success")){
                if(access.equals("IN")) {
                    roomTV.setText(room);
                    logoutButton.setEnabled(true);
                    searchButton.setEnabled(false);
                    mainView.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.VISIBLE);
                    final Handler handler = new Handler();
                    timer = new Timer();
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                public void run() {
                                    new UploadAsyncTask().execute();
                                }
                            });
                        }
                    };
                    timer.schedule(task, 0, 20*1000);

                } else if (access.equals("OUT")){
                    logoutButton.setEnabled(false);
                    searchButton.setEnabled(true);
                    mainView.setVisibility(View.INVISIBLE);
                    fab.setVisibility(View.INVISIBLE);
                    timer.cancel();
                    timer.purge();
                }
            } else
                Snackbar.make(layout, "Room was not found", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
        }
    }

    private class SubmitOpinionTask extends AsyncTask<Void, Void, String>{
        String temp, lum;
        int temp_val, lum_val;

        SubmitOpinionTask(String temp, int temp_val, String lum, int lum_val) {
            this.temp = temp;
            this.temp_val = temp_val;
            this.lum = lum;
            this.lum_val = lum_val;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingDialog = ProgressDialog.show(MainActivity.this, "Submit",
                    "Sending your opinion to the server");
        }
        @Override
        protected String doInBackground(Void... voids) {
            Map<String,String> params = new HashMap<>();
            params.put("user", mEmail);
            params.put("room", room);
            params.put("temp", temp);
            params.put("tempVal", "" + temp_val);
            params.put("lum", lum);
            params.put("lumVal", "" + lum_val);

            ServiceHandler jsonParser = new ServiceHandler();
            String response = jsonParser.makeServiceCall("sendOpinion.php", params);

            Log.i("***SubmitOpinion", "Response> " + response);
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if (loadingDialog.isShowing()) loadingDialog.dismiss();
            if(response==null)
                Snackbar.make(layout, "Server unreachable " +!response.equals("success") + " " +response, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
        }
    }

    private class UploadAsyncTask extends AsyncTask<Void, Void, StringBuilder> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingDialog = ProgressDialog.show(MainActivity.this,
                    "Retrieve opinions", "Queering the server for the last half an hour opinions...");
        }

        @Override
        protected StringBuilder doInBackground(Void... args) {
            Map<String,String> nameValuePairs = new HashMap<>();
            StringBuilder sb = new StringBuilder();

            nameValuePairs.put("room", room);

            ServiceHandler jsonParser = new ServiceHandler();
            String result = jsonParser.makeServiceCall("getMeasures.php", nameValuePairs);
            if (result != null) {
                try {
                    JSONObject jsonObj = new JSONObject(result);
                    JSONArray jsonEvents = jsonObj.getJSONArray("measures");
                    JSONObject measureObj = (JSONObject) jsonEvents.get(0);
                    measuredTemp = measureObj.getString("temp");
                    measuredLum = measureObj.getString("lum");

                } catch (JSONException e) { e.printStackTrace(); }
            } else Log.e("***JSON Data","Didn't receive any data from server!");

            result = jsonParser.makeServiceCall("getOpinions.php", nameValuePairs);
            Log.d("***UploadAsyncTask","result: "+result);

            if (result != null) {
                try {
                    JSONObject jsonObj = new JSONObject(result);
                    JSONArray jsonEvents = jsonObj.getJSONArray("opinions");

                    for (int i = 0; i < jsonEvents.length();i++) {
                        JSONObject opinionsObj = (JSONObject) jsonEvents.get(i);

                        sb.append(opinionsObj.getString("date")).append("\n");
                        sb.append("Temperature " + opinionsObj.getString("temp") + "(" + opinionsObj.getString("temp_vote") + ")").append("\n");
                        sb.append("Luminance " + opinionsObj.getString("lum") + "(" + opinionsObj.getString("lum_vote") + ")").append("\n").append("\n");
                    }

                } catch (JSONException e) { e.printStackTrace(); }
            } else Log.e("***JSON Data","Didn't receive any data from server!");

            return sb;
        }

        @Override
        protected void onPostExecute(StringBuilder result){
            if(loadingDialog.isShowing()) loadingDialog.dismiss();

            tempMeasured.setText(measuredTemp);
            lumMeasured.setText(measuredLum);
            if(result!=null) prevOpinions.setText(result.toString());
        }
    }
}
