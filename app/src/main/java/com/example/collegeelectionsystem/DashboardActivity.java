package com.example.collegeelectionsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.Timestamp;

import java.util.HashSet;
import java.util.Set;

/**
 * DashboardActivity — listens for announcements and results publish events,
 * shows local notifications (NotificationHelper), subscribes to FCM topics once.
 *
 * Announcement document structure assumed:
 * {
 *   author: "Admin",
 *   content: "Tomorrow will be a project submission",
 *   dateText: "",
 *   pinned: false,
 *   subtitle: "Project Submission",
 *   timestamp: <Firestore Timestamp>,
 *   title: "Submission",
 *   type: "urgent"
 * }
 */
public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String PREFS = "votify_prefs";
    private static final String KEY_FCM_SUBSCRIBED = "fcm_subscribed";
    private static final String KEY_LAST_ANN_TS = "last_ann_ts";
    private static final String KEY_RESULTS_NOTIFIED = "results_notified";

    private ListenerRegistration notifListener;
    private ListenerRegistration settingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI elements (adjust ids in layout if needed)
        CardView btnCandidates = findViewById(R.id.btnCandidates);
        CardView btnVoting = findViewById(R.id.btnVoting);
        CardView btnAnnouncements = findViewById(R.id.btnAnnouncements);
        CardView btnResults = findViewById(R.id.btnResults);
        CardView btnViewParties = findViewById(R.id.btnViewParties);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        TextView tvWelcome = findViewById(R.id.tvWelcome);

        // Set selected bottom nav item
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            bottomNavigationView.setOnItemSelectedListener(this::onBottomNavItemSelected);
        }

        // Welcome text (email or generic)
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            tvWelcome.setText(email != null ? "Logged in as " + email : "Welcome Back!");
        } else {
            tvWelcome.setText("Welcome Back!");
        }

        // Subscribe to FCM topics once (remember in prefs)
        subscribeToTopicsOnce();

        // Quick-action clicks (CardViews)
        btnCandidates.setOnClickListener(v -> openCandidates());
        btnVoting.setOnClickListener(v -> openVoting());
        btnAnnouncements.setOnClickListener(v -> openAnnouncements());
        btnResults.setOnClickListener(v -> openResults());
        btnViewParties.setOnClickListener(v -> openParties());

        btnLogout.setOnClickListener(v -> performLogout());

        // Optional: load stats (kept small)
        loadStats();
    }

    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            findViewById(R.id.scrollView).scrollTo(0, 0);
            return true;
        } else if (id == R.id.nav_candidates) {
            openCandidates();
            return true;
        } else if (id == R.id.nav_vote) {
            openVoting();
            return true;
        } else if (id == R.id.nav_news) {
            openAnnouncements();
            return true;
        } else if (id == R.id.nav_results) {
            openResults();
            return true;
        }
        return false;
    }

    private void openCandidates() {
        Intent i = new Intent(this, CandidatesActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    private void openVoting() {
        Intent i = new Intent(this, VotingActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    private void openAnnouncements() {
        Intent i = new Intent(this, AnnouncementsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    private void openResults() {
        Intent i = new Intent(this, ResultsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    private void openParties() {
        Intent i = new Intent(this, PartiesActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    private void performLogout() {
        if (mAuth != null) {
            mAuth.signOut();
        }

        // Unsubscribe to avoid receiving notifications after logout
        FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements");
        FirebaseMessaging.getInstance().unsubscribeFromTopic("results");

        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_FCM_SUBSCRIBED).apply();

        Intent intent = new Intent(DashboardActivity.this, StudentLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadStats() {
        if (db == null) return;

        // Example: you can show counts in the UI if you add TextViews
        db.collection("users").get().addOnSuccessListener(QuerySnapshot::size).addOnFailureListener(e -> {});
    }

    // Subscribe to FCM topics only once per device-install/login cycle
    private void subscribeToTopicsOnce() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean already = prefs.getBoolean(KEY_FCM_SUBSCRIBED, false);
        if (already) return;

        FirebaseMessaging.getInstance().subscribeToTopic("announcements");
        FirebaseMessaging.getInstance().subscribeToTopic("results")
                .addOnCompleteListener(task -> prefs.edit().putBoolean(KEY_FCM_SUBSCRIBED, true).apply());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // start listeners
        startAnnouncementsListener();
        startResultsPublishedListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // remove listeners to avoid duplicate registration
        if (notifListener != null) {
            notifListener.remove();
            notifListener = null;
        }
        if (settingsListener != null) {
            settingsListener.remove();
            settingsListener = null;
        }
    }

    // ------------------- ANNOUNCEMENTS LISTENER -------------------
    /**
     * Starts a listener on the announcements collection.
     * Uses a persisted "last seen" timestamp to avoid duplicate notifications.
     * On first ever run (no stored last-seen), we initialize last-seen to now so that
     * the user doesn't get notifications for all historical announcements.
     */
    private void startAnnouncementsListener() {
        if (notifListener != null) return; // already running

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        long lastSeen = prefs.getLong(KEY_LAST_ANN_TS, 0L);

        // If first time ever (0), initialize to now and persist — avoid firing notifications for existing docs.
        if (lastSeen == 0L) {
            prefs.edit().putLong(KEY_LAST_ANN_TS, System.currentTimeMillis()).apply();
            lastSeen = prefs.getLong(KEY_LAST_ANN_TS, 0L);
        }

        // order by timestamp ascending so we can track newest
        long finalLastSeen = lastSeen;
        notifListener = db.collection("announcements")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    long newestSeen = finalLastSeen;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() != DocumentChange.Type.ADDED) continue;

                        // read fields consistent with your structure
                        String title = dc.getDocument().getString("title");
                        String content = dc.getDocument().getString("content");
                        Timestamp ts = dc.getDocument().getTimestamp("timestamp");

                        long docMillis = (ts != null) ? ts.toDate().getTime() : 0L;

                        // skip if not newer than persisted lastSeen
                        if (docMillis <= finalLastSeen) continue;

                        // show notification
                        String shortTitle = title != null ? title : "New Announcement";
                        String shortMsg = content != null ? content : dc.getDocument().getString("subtitle");
                        if (shortMsg == null) shortMsg = "Check announcements for details";

                        NotificationHelper.showNotification(this, shortTitle, shortMsg);

                        // advance newestSeen
                        if (docMillis > newestSeen) newestSeen = docMillis;
                    }

                    // persist newest if changed
                    if (newestSeen > finalLastSeen) {
                        prefs.edit().putLong(KEY_LAST_ANN_TS, newestSeen).apply();
                    }
                });
    }

    // ------------------- RESULTS PUBLISHED LISTENER -------------------
    /**
     * Listens for settings/election.resultsPublished and notifies once when it becomes true.
     * Uses a persisted boolean flag KEY_RESULTS_NOTIFIED to prevent duplicates.
     */
    private void startResultsPublishedListener() {
        if (settingsListener != null) return;

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        settingsListener = db.collection("settings").document("election")
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    Boolean published = doc.getBoolean("resultsPublished");
                    if (published != null && published) {
                        boolean alreadyNotified = prefs.getBoolean(KEY_RESULTS_NOTIFIED, false);
                        if (!alreadyNotified) {
                            NotificationHelper.showNotification(this,
                                    "Results Declared 🎉",
                                    "Winners have been announced. Tap to view results.");
                            prefs.edit().putBoolean(KEY_RESULTS_NOTIFIED, true).apply();
                        }
                    } else {
                        // reset flag if admin unpublishes and you want future publishes to notify again
                        prefs.edit().putBoolean(KEY_RESULTS_NOTIFIED, false).apply();
                    }
                });
    }
}
