package com.example.collegeelectionsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;

public class GenerateTokenActivity extends AppCompatActivity {

    private EditText etTokenCount;
    private Button btnGenerate;
    private TextView tvGeneratedTokens;
    private FirebaseFirestore db;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TOKEN_LENGTH = 8; // 8-character token

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_token);

        etTokenCount = findViewById(R.id.etTokenCount);
        btnGenerate = findViewById(R.id.btnGenerateTokens);
        tvGeneratedTokens = findViewById(R.id.tvGeneratedTokens);

        db = FirebaseFirestore.getInstance();

        btnGenerate.setOnClickListener(v -> generateTokens());
    }

    private void generateTokens() {
        String countStr = etTokenCount.getText().toString().trim();
        if (countStr.isEmpty()) {
            Toast.makeText(this, "Enter number of tokens", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = Integer.parseInt(countStr);
        StringBuilder allTokens = new StringBuilder();

        for (int i = 0; i < count; i++) {
            String token = generateRandomToken();

            HashMap<String, Object> tokenData = new HashMap<>();
            tokenData.put("token", token);
            tokenData.put("used", false);
            tokenData.put("createdAt", Timestamp.now());

            db.collection("tokens").add(tokenData)
                    .addOnSuccessListener(doc -> {
                        allTokens.append(token).append("\n");
                        tvGeneratedTokens.setText(allTokens.toString());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }

        Toast.makeText(this, "Tokens generated successfully", Toast.LENGTH_SHORT).show();
    }

    private String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return token.toString().toUpperCase(Locale.ROOT);
    }
}
