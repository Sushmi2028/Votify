package com.example.collegeelectionsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

public class AnonymousLoginActivity extends AppCompatActivity {

    // Config: set to true if you want to mark token used immediately on login.
    // Recommended: leave false and mark the token as used when the votes are committed atomically.
    private static final boolean MARK_TOKEN_USED_ON_LOGIN = false;

    // SharedPreferences keys
    private static final String PREFS = "votify_prefs";
    private static final String KEY_ANON_TOKEN_DOC_ID = "anon_token_doc_id";   // store token document id
    private static final String KEY_ANON_TOKEN_VALUE = "anon_token_value";    // store token string value

    private EditText etToken;
    private Button btnLogin;
    private TextView tvStudent, tvAdmin;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anonymous_login);

        etToken = findViewById(R.id.etToken);
        btnLogin = findViewById(R.id.btnAnonymousLogin);
        tvStudent = findViewById(R.id.btnloginActivity);
        tvAdmin = findViewById(R.id.btnAdminLogin);

        db = FirebaseFirestore.getInstance();

        // navigation links
        tvStudent.setOnClickListener(v -> {
            startActivity(new Intent(AnonymousLoginActivity.this, StudentLoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });

        tvAdmin.setOnClickListener(v -> {
            startActivity(new Intent(AnonymousLoginActivity.this, AdminLoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });

        btnLogin.setOnClickListener(v -> validateTokenAndLogin());
    }

    private void validateTokenAndLogin() {
        String tokenInput = etToken.getText().toString().trim();
        if (tokenInput.isEmpty()) {
            Toast.makeText(this, "Please enter a token", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query tokens collection for a document where field "token" equals the input
        Query q = db.collection("tokens").whereEqualTo("token", tokenInput).limit(1);

        q.get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                Toast.makeText(this, "Invalid token", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use the first (and expected only) matched document
            QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);

            Boolean used = doc.getBoolean("used");
            if (used != null && used) {
                Toast.makeText(this, "Token already used", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save token doc id and value to SharedPreferences for later use by VotingActivity
            String tokenDocId = doc.getId();
            String tokenValue = doc.getString("token"); // should be same as tokenInput
            if (tokenValue == null) tokenValue = tokenInput;

            SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_ANON_TOKEN_DOC_ID, tokenDocId)
                    .putString(KEY_ANON_TOKEN_VALUE, tokenValue)
                    .apply();

            // Subscribe to announcement/result topics for notifications
            FirebaseMessaging.getInstance().subscribeToTopic("announcements");
            FirebaseMessaging.getInstance().subscribeToTopic("results");

            // Optionally mark token used now (toggle via MARK_TOKEN_USED_ON_LOGIN)
            if (MARK_TOKEN_USED_ON_LOGIN) {
                db.collection("tokens").document(tokenDocId)
                        .update("used", true, "lastUsedAt", Timestamp.now())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Anonymous login successful!", Toast.LENGTH_SHORT).show();
                            goToAnonymousDashboard();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Login succeeded but failed to update token: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            // still proceed to dashboard because token is saved locally
                            goToAnonymousDashboard();
                        });
            } else {
                // Do not mark used here — preferred to mark when votes are committed atomically.
                // But still update lastSeen or lastAttempt for auditing if desired (non-destructive).
                db.collection("tokens").document(tokenDocId)
                        .update("lastSeenAt", Timestamp.now())
                        .addOnSuccessListener(aVoid -> {
                            // ignore
                        })
                        .addOnFailureListener(e -> {
                            // ignore
                        });

                Toast.makeText(this, "Anonymous login successful!", Toast.LENGTH_SHORT).show();
                goToAnonymousDashboard();
            }

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error checking token: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void goToAnonymousDashboard() {
        Intent i = new Intent(this, AnonymousVotingActivity.class);
        // clear backstack so pressing back doesn't return to login
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
