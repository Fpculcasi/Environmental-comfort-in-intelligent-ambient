package com.example.francescop.projectnes;

/**
 * Created by francescop on 18/08/2017.
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import android.util.Log;

class ServiceHandler {
    //static InputStream is = null;

    //return a JSON string from Map<String,String>
    private String getEncodedData(Map<String,String> data) {
        StringBuilder sb = new StringBuilder();
        for(String key : data.keySet()) {
            String value = null;
            Log.d("***",data+key);
            try {
                value = URLEncoder.encode(data.get(key),"UTF-8");
            }catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if(sb.length()>0) sb.append("&");

            sb.append(key).append("=").append(value);
        }
        return sb.toString();
    }

    String makeServiceCall(String my_url, Map<String, String> params) {
        String response = null;
        //Encoded String - to encode string by our custom method
        String encodedStr = getEncodedData(params);

        BufferedReader reader;
        HttpURLConnection con;

        try {
            //Converting address String to URL
            String server_url =
                    //*
                    "http://fpculcasi.altervista.org";
                    //*/"http://10.0.2.2";
            URL url = new URL(server_url + "/" + my_url);
            //Opening the connection (Not setting or using CONNECTION_TIMEOUT)
            con = (HttpURLConnection) url.openConnection();

            //Post Method
            con.setRequestMethod("POST");
            //To enable inputting values using POST method
            //(Basically, after this we can write the params to the body of POST method)
            con.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
            //Writing params to outputstreamwriter
            writer.write(encodedStr);
            //Sending the data to the server
            writer.flush();

            //Data Read Procedure - Reading the data comming line by line
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String line;
            while((line = reader.readLine()) != null) { //Read until there is something available
                sb.append(line).append("\n"); //Reading and saving line by line - not all at once
            }
            response = sb.toString(); //Saving complete data received in string, you can do it differently

            Log.i("ServiceHandler",encodedStr);
            //Check to the values received in Logcat
            Log.i("ServiceHandler","The values received in the store part are as follows:");
            Log.i("ServiceHandler", response);

            reader.close();

            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }
}