package com.example.objectrecognition;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewDebug;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;
public class HttpGetTask extends AsyncTask <Void, Void, String> {
    private MainActivity mParentActivity;
    private String mImagePath;
    private String[] speciesName;
    private String mRelatedImageUri;
    ProgressDialog mProgressDialog;
    private static final String PROJECT = "all"; // try specific floras: "weurope", "canada"…
    private static final String API_KEY = "2b10AB4TYvEjgK5Cyugy21je";
    private static final String TARGETURL = "https://my-api.plantnet.org/v2/identify/"
            + PROJECT
            + "?api-key=" + API_KEY
            + "&" + "include-related-images=true";
    public HttpGetTask(MainActivity parentActivity, String imagePath) {
        this.mParentActivity = parentActivity;
        this.mImagePath = imagePath;
        this.speciesName = new String[2];
    }
    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog(mParentActivity);
        mProgressDialog.setMessage("Identifying...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }
    @Override
    protected String doInBackground(Void... voids) {
        return exec_get();
    }

    @Override
    protected void onPostExecute(String statusCodeString) {
        mProgressDialog.dismiss();
        this.mParentActivity.showIdentifyResult(statusCodeString, mRelatedImageUri, speciesName);
    }

    private String exec_get() {
        String result;
        String relatedImageUri;
        OkHttpClient client = new OkHttpClient();
        File file = new File(this.mImagePath);
        Log.d("objectSample", file.getName());

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("organs", "auto")
                .addFormDataPart("images", file.getName(),
                        RequestBody.create(MediaType.parse("image/jpeg"), file))
                .build();
        Request request = new Request.Builder()
                .url(TARGETURL)
                .post(requestBody)
                .build();
        try {
            Response response = client.newCall(request).execute();
            int statusCode = response.code();
            result = String.valueOf(statusCode);
            switch (statusCode){
                case 400: speciesName[0] = "Bad Request"; break;
                case 401: speciesName[0] = "Unauthorized"; break;
                case 404: speciesName[0] = "Species not found"; break;
                case 413: speciesName[0] = "Payload too large"; break;
                case 414: speciesName[0] = "URI too long"; break;
                case 415: speciesName[0] = "Unsupported Media Type"; break;
                case 429: speciesName[0] = "Too many requests"; break;
                case 500: speciesName[0] = "Internal server error"; break;
                default: break;
            }
            if (statusCode == 200) {
                String responseBody = response.body().string();
                String keyToExtract = "scientificNameWithoutAuthor";
                JSONObject responseJSON = new JSONObject(responseBody);
                JSONArray resultJSON = responseJSON.optJSONArray("results");
                if (resultJSON == null) {
                    result = String.valueOf(201); // status code 201: got response but no results
                    speciesName[0] =  "Species Not Found";
                } else {
                    speciesName[0] = resultJSON.getJSONObject(0)
                            .getJSONObject("species")
                            .optString("scientificNameWithoutAuthor", "unknown");
                    speciesName[1] = resultJSON.getJSONObject(0)
                            .getJSONObject("species")
                            .optJSONArray("commonNames").toString();
                    JSONArray relatedImages = resultJSON.getJSONObject(0)
                                    .optJSONArray("images");
                    if (relatedImages!=null) {
                        mRelatedImageUri = relatedImages.optJSONObject(0)
                                .getJSONObject("url")
                                .optString("m","");
                    } else {
                        mRelatedImageUri = "";
                    }
                    Log.d("objectSample", speciesName[1]);
                }
            }
            return result;
        } catch (IOException e) {
            mProgressDialog.dismiss();
            throw new RuntimeException(e);
        } catch (JSONException e) {
            mProgressDialog.dismiss();
            throw new RuntimeException(e);
        }

        /*
        HttpURLConnection http=null;
        InputStream inputStream=null;
        OutputStream output=null;
        String result="";
        String boundary = Long.toHexString(System.currentTimeMillis()); // 随机分隔线
        String crlf = "\r\n"; // 回车换行符
        String twoHyphens = "--"; // 分隔线前缀

        try{
            URL url = new URL(TARGETURL);
            http = (HttpURLConnection) url.openConnection();
            http.setUseCaches(false);
            http.setDoOutput(true);
            http.setDoInput(true);
            http.setRequestMethod("POST");
            http.setRequestProperty("Connection", "Keep-Alive");
            http.setRequestProperty("Cache-Control", "no-cache");
            http.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            //http.connect();
            http.setConnectTimeout(20*1000);//设置连接主机超时（单位：毫秒）
            http.setReadTimeout(20*1000);
            output = http.getOutputStream();

            // organs
            output.write((twoHyphens + boundary + crlf).getBytes());
            output.write(("Content-Disposition: form-data; name=\"organs\"" + crlf).getBytes());
            output.write(("Content-Type: text/plain; charset=UTF-8"+crlf).getBytes());
            output.write(crlf.getBytes());
            output.write("auto".getBytes());
            output.write(crlf.getBytes());
            // image file
            output.write((twoHyphens+boundary+crlf).getBytes());
            output.write(("Content-Disposition: form-data; name=\"images\"; filename=\"" + file.getName() +"\""+ crlf).getBytes());
            output.write(("Content-Type: image/jpeg" + crlf).getBytes());
            output.write(crlf.getBytes());
            InputStream inputImage = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputImage.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                Log.d("objectSample", String.valueOf(bytesRead));
            }
            inputImage.close();
            output.write(crlf.getBytes());


            output.flush();


            String status = http.getResponseMessage();
            Log.d("objectSample", status);
            inputStream = http.getErrorStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result+=line;
            }
            Log.d("objectSample", result);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.d("objectSample", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                if (http!=null){
                    http.disconnect();
                }
                if (inputStream != null){
                    inputStream.close();
                }
                output.close();
                JSONObject jsStr = new JSONObject(result);
                return jsStr.optString("scientificNameWithoutAuthor", "unknown");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        */
    }
}
