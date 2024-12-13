package com.example.my_blog_app;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    TextView textView;
    //static String site_url = "http://10.0.2.2:8000"; // local
    static String site_url = "https://jerryzoo.pythonanywhere.com/"; // pythonanywhere
    JSONObject post_json;
    String imageUrl = null;
    CloadImage taskDownload;
    private TextView totalPriceTextView;
    private Button calculateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        totalPriceTextView = findViewById(R.id.totalPriceTextView);
        calculateButton = findViewById(R.id.calculateButton);

        calculateButton.setOnClickListener(v -> showCalculateDialog());
    }

    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "장바구니 불러오기", Toast.LENGTH_LONG).show();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Pair<Bitmap, String>>> {
        @Override
        protected List<Pair<Bitmap, String>> doInBackground(String... urls) {
            List<Pair<Bitmap, String>> dataList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                String token = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);
                    Log.d("CloadImage", "aryJson: " + aryJson);
                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);
                        imageUrl = post_json.getString("image");
                        String text = post_json.getString("text");

                        if (!imageUrl.equals("")) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                            dataList.add(new Pair<>(imageBitmap, text));
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return dataList;
        }

        @Override
        protected void onPostExecute(List<Pair<Bitmap, String>> dataList) {
            if (dataList.isEmpty()) {
                textView.setText("장바구니가 비었습니다.");
            } else {
                textView.setText("");
                List<Bitmap> images = new ArrayList<>();
                List<String> texts = new ArrayList<>();
                for (Pair<Bitmap, String> data : dataList) {
                    images.add(data.first);
                    texts.add(data.second);
                    updateTotalPrice(data.second);
                }
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(images, texts);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private void updateTotalPrice(String texts) {
        int totalPrice = totalPriceTextView.getText().toString().replace("총 금액: ", "").replace("원", "").trim().isEmpty() ? 0 : Integer.parseInt(totalPriceTextView.getText().toString().replace("총 금액: ", "").replace("원", "").trim());

        String priceString = texts.split(" - ")[1].replace("원", "").trim(); // 금액 부분 추출
        totalPrice += Integer.parseInt(priceString); // 금액을 정수로 변환하여 합산

        totalPriceTextView.setText("총 금액: " + totalPrice + "원"); // 총 금액 표시
    }

    private void showCalculateDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_calculate, null);

        TextView tvTotalPrice = dialogView.findViewById(R.id.tvTotalPrice);
        tvTotalPrice.setText(totalPriceTextView.getText().toString());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("총 금액 계산")
                .setView(dialogView)
                .create();

        Button btnCalculate = dialogView.findViewById(R.id.btnCalculate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // 계산 버튼 동작 설정
        btnCalculate.setOnClickListener(v -> {
            resetDatabase(); // 데이터베이스 초기화 호출
            totalPriceTextView.setText("총 금액: 0원"); // 총 금액 초기화
            dialog.dismiss();
        });

        // 취소 버튼 동작 설정
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void resetDatabase() {
        new ResetDatabaseTask(this).execute();
    }

    private static class ResetDatabaseTask extends AsyncTask<Void, Void, String> {
        private final WeakReference<MainActivity> activityReference;

        ResetDatabaseTask(MainActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(site_url + "/reset_model/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return "계산 완료";
                } else {
                    return "계산 실패";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Exception occurred.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
            activity.clearRecyclerView(); // RecyclerView 비우기
        }
    }

    // RecyclerView 비우기 메서드 추가
    private void clearRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(null); // 어댑터를 null로 설정하여 데이터 비우기
        textView.setText("장바구니가 비었습니다."); // 장바구니 비어있음을 표시
    }
}