package com.example.collegeelectionsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

public class StudentLoginActivity extends AppCompatActivity {

    private static final String TAG = "StudentLogin";
    private static final int REQ_POST_NOTIFICATIONS = 1001;
    private static final String PREFS = "votify_prefs";
    private static final String KEY_FCM_SUBSCRIBED = "fcm_subscribed";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        mAuth = FirebaseAuth.getInstance();
        TextView tvForgot = findViewById(R.id.tvForgotPassword);
        tvForgot.setOnClickListener(v -> showPasswordResetDialog(etEmail));

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView btnBack = findViewById(R.id.btnBack);
        TextView anonymous = findViewById(R.id.btnAnonymousLogin);
        TextView admin = findViewById(R.id.btnAdminLogin);

        anonymous.setOnClickListener(v -> {
            Intent intent = new Intent(StudentLoginActivity.this, AnonymousLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        admin.setOnClickListener(v -> {
            Intent intent = new Intent(StudentLoginActivity.this, AdminLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(StudentLoginActivity.this, StudentRegisterStep1Activity.class));
            finish();
        });

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void showPasswordResetDialog(EditText prefillEmailField) {
        // create an EditText for input
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter your email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        // prefill from existing email field if available
        String current = prefillEmailField.getText().toString().trim();
        if (!current.isEmpty()) input.setText(current);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reset password")
                .setMessage("Enter your email to receive a password reset link.")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    sendPasswordReset(email);
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void sendPasswordReset(String email) {
        if (email.isEmpty()) {
            android.widget.Toast.makeText(this, "Please enter your email", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (!email.endsWith("@learner.manipal.edu")) {
            android.widget.Toast.makeText(this, "Email must end with @learner.manipal.edu", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.widget.Toast.makeText(this, "Reset link sent to " + email, android.widget.Toast.LENGTH_LONG).show();
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        android.widget.Toast.makeText(this, "Error: " + err, android.widget.Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void loginUser() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.endsWith("@learner.manipal.edu")) {
            Toast.makeText(this, "Email must end with @learner.manipal.edu", Toast.LENGTH_SHORT).show();
            return;
        }

        // disable button to avoid double clicks
        btnLogin.setEnabled(false);

        // sign in
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            btnLogin.setEnabled(true);
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                    // Request notifications permission on Android 13+
                    checkNotificationPermissionThenSubscribe();

                    // Navigate to dashboard and clear backstack
                    Intent i = new Intent(StudentLoginActivity.this, DashboardActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(this, "Login succeeded but user is null.", Toast.LENGTH_LONG).show();
                }
            } else {
                String msg = "Error: " + Objects.requireNonNull(task.getException()).getMessage();
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "signIn failed", task.getException());
            }
        });
    }

    private void checkNotificationPermissionThenSubscribe() {
        // If target SDK >= 33 (Android 13) we need runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
                return; // subscription will occur in onRequestPermissionsResult if granted
            }
        }
        subscribeToTopicsIfNeeded();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                subscribeToTopicsIfNeeded();
            } else {
                // permission denied: still try to subscribe (FCM may still deliver), but no guarantees for notifications on Android 13+
                Log.w(TAG, "POST_NOTIFICATIONS permission denied by user");
                subscribeToTopicsIfNeeded();
            }
        }
    }

    private void subscribeToTopicsIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean already = prefs.getBoolean(KEY_FCM_SUBSCRIBED, false);
        if (already) {
            Log.d(TAG, "Already subscribed to topics (skipping).");
            return;
        }

        // Subscribe to announcements
        FirebaseMessaging.getInstance().subscribeToTopic("announcements")
                .addOnCompleteListener((Task<Void> t) -> {
                    if (t.isSuccessful()) {
                        Log.d(TAG, "Subscribed to announcements");
                    } else {
                        Log.e(TAG, "Failed subscribe announcements", t.getException());
                    }
                    // we don't early-return; try subscribing to both topics always
                });

        // Subscribe to results
        FirebaseMessaging.getInstance().subscribeToTopic("results")
                .addOnCompleteListener((Task<Void> t) -> {
                    if (t.isSuccessful()) {
                        Log.d(TAG, "Subscribed to results");
                    } else {
                        Log.e(TAG, "Failed subscribe results", t.getException());
                    }

                    // Mark subscribed (even if one failed — reattempts can be implemented later)
                    prefs.edit().putBoolean(KEY_FCM_SUBSCRIBED, true).apply();
                });
    }
}
