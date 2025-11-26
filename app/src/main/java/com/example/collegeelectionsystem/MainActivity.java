package com.example.collegeelectionsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * MainActivity (startup router).
 * Redirects users according to role:
 * - admin -> AdminDashboardActivity
 * - student/auth -> DashboardActivity
 * - otherwise -> StudentLoginActivity
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityRouter";
    private static final String PREFS = "votify_prefs";
    private static final String KEY_ANON_TOKEN_DOC_ID = "anon_token_doc_id";
    // optionally you may have saved token value too; key name must match earlier code
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // no need to setContentView if this is a pure router; but if layout exists keep it
        // setContentView(R.layout.activity_main);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser current = mAuth.getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (current != null) {
            // Signed-in user -> determine role from Firestore users collection
            String uid = current.getUid();
            // read the users/<uid> doc
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            String role = doc.getString("role");
                            if (role != null && role.equalsIgnoreCase("admin")) {
                                goToAdminDashboard();
                            } else {
                                goToStudentDashboard();
                            }
                        } else {
                            // No user doc found -> default to student dashboard OR send to profile/setup
                            Log.w(TAG, "No users document for uid: " + uid);
                            goToStudentDashboard();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to read user role: " + e.getMessage());
                        // fall back to student dashboard to avoid blocking app
                        goToStudentDashboard();
                    });

        } else {
            // Not authenticated — check for anonymous token saved in prefs
            SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String anonTokenDocId = prefs.getString(KEY_ANON_TOKEN_DOC_ID, null);

                // No session — launch student login (or onboarding)
                goToLogin();

        }
    }

    private void goToAdminDashboard() {
        Intent i = new Intent(this, AdminDashboardActivity.class);
        // clear backstack — so back won't return here
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void goToStudentDashboard() {
        Intent i = new Intent(this, DashboardActivity.class); // student dashboard
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void goToAnonymousDashboard() {
        Intent i = new Intent(this,AnonymousVotingActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void goToLogin() {
        Intent i = new Intent(this, StudentLoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
