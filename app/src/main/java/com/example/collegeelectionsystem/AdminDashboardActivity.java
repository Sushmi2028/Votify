package com.example.collegeelectionsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin dashboard for publishing results, archiving, and navigation.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Button btnAddCandidate = findViewById(R.id.btnAddCandidate);
        Button btnAddParty = findViewById(R.id.btnAddParty);
        Button btnAddAnnouncement = findViewById(R.id.btnAddAnnouncement);
        Button btnManageResults = findViewById(R.id.btnManageResults);
        Button btnGenerateTokens = findViewById(R.id.btnGenerateTokens);
        Button btnToggleResults = findViewById(R.id.btnToggleResults);
        Button btnManageCandidate = findViewById(R.id.btnManageCandidate);
        ImageButton logout = findViewById(R.id.logoutBtn);

        db = FirebaseFirestore.getInstance();

        logout.setOnClickListener(v -> {
            // Sign out from Firebase Authentication
            FirebaseAuth.getInstance().signOut();

            // Unsubscribe from topics so admin doesn’t keep getting notifications
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements");
            FirebaseMessaging.getInstance().unsubscribeFromTopic("results");

            // Show confirmation (optional)
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

            // Redirect to login screen
            Intent i = new Intent(AdminDashboardActivity.this, AdminLoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        // Navigation
        btnAddCandidate.setOnClickListener(v -> startActivity(new Intent(this, AddCandidateActivity.class)));
        btnAddParty.setOnClickListener(v -> startActivity(new Intent(this, AddPartyActivity.class)));
        btnAddAnnouncement.setOnClickListener(v -> startActivity(new Intent(this, AddAnnouncementActivity.class)));
        btnManageResults.setOnClickListener(v -> startActivity(new Intent(this, ResultsActivity.class)));
        btnGenerateTokens.setOnClickListener(v -> startActivity(new Intent(this, GenerateTokenActivity.class)));
        btnManageCandidate.setOnClickListener(v -> startActivity(new Intent(this, AdminCandidatesActivity.class)));

        // Publish / Unpublish Results
        btnToggleResults.setOnClickListener(v -> toggleResults());
    }

    /**
     * Toggle resultsPublished flag. When publishing, archive current votes first,
     * then set resultsPublished=true and create an announcement document.
     */
    private void toggleResults() {
        DocumentReference electionRef = db.collection("settings").document("election");

        electionRef.get().addOnSuccessListener(doc -> {
            boolean current = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("resultsPublished"));
            boolean newValue = !current;

            if (newValue) {
                // Publish: archive current results first, then set flag + announcement
                archiveCurrentResults(new ArchiveCallback() {
                    @Override
                    public void onSuccess() {
                        // after archive success, set resultsPublished
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("resultsPublished", true);
                        payload.put("publishedAt", Timestamp.now());

                        electionRef.set(payload, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminDashboardActivity.this, "Results published ✅", Toast.LENGTH_SHORT).show();
                                    // create announcement to notify clients listening to announcements collection
                                    createResultsAnnouncement();
                                })
                                .addOnFailureListener(e -> Toast.makeText(AdminDashboardActivity.this, "Failed to publish results: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(AdminDashboardActivity.this, "Archiving failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // Unpublish: simply set resultsPublished = false
                Map<String, Object> payload = new HashMap<>();
                payload.put("resultsPublished", false);
                electionRef.set(payload, SetOptions.merge())
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(AdminDashboardActivity.this, "Results unpublished ❌", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(AdminDashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }
        }).addOnFailureListener(e ->
                Toast.makeText(AdminDashboardActivity.this, "Error reading current state: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Archive current votes into election_results/{archiveId}/positions/{position} documents.
     * Calls the callback on completion.
     *
     * Note: For large numbers of votes (>500 deletes/writes) you should paginate / chunk operations.
     */
    private void archiveCurrentResults(ArchiveCallback callback) {
        String archiveId = new java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(new java.util.Date());

        db.collection("votes").get()
                .addOnSuccessListener(querySnapshot -> {
                    // Build map: position -> (candidateId -> count)
                    java.util.Map<String, java.util.Map<String, Integer>> map = new java.util.HashMap<>();
                    querySnapshot.forEach(d -> {
                        String pos = d.getString("position");
                        String cid = d.getString("candidateId");
                        if (pos == null || cid == null) return;
                        map.putIfAbsent(pos, new java.util.HashMap<>());
                        java.util.Map<String, Integer> inner = map.get(pos);
                        inner.put(cid, inner.getOrDefault(cid, 0) + 1);
                    });

                    // Write tallies under election_results/{archiveId}/positions/{position}
                    // Use simple writes; if you want everything in a single batch, note Firestore batch limit (500).
                    // We'll write per-position docs (one write per position + one metadata doc).
                    try {
                        for (Map.Entry<String, java.util.Map<String, Integer>> entry : map.entrySet()) {
                            String position = entry.getKey();
                            java.util.Map<String, Integer> tallies = entry.getValue();

                            Map<String, Object> payload = new HashMap<>();
                            payload.put("tallies", tallies);
                            payload.put("position", position);

                            db.collection("election_results")
                                    .document(archiveId)
                                    .collection("positions")
                                    .document(sanitizeDocId(position))
                                    .set(payload, SetOptions.merge());
                        }

                        // metadata for archive
                        Map<String, Object> meta = new HashMap<>();
                        meta.put("createdAt", Timestamp.now());
                        meta.put("label", "Election " + archiveId);
                        db.collection("election_results").document(archiveId).set(meta, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(callback::onFailure);

                    } catch (Exception ex) {
                        callback.onFailure(ex);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Create an announcement document after publishing results so clients listening to announcements
     * will show notifications (Firestore-listener approach).
     */
    private void createResultsAnnouncement() {
        Map<String, Object> ann = new HashMap<>();
        ann.put("author", "Admin");
        ann.put("title", "Results Published");
        ann.put("subtitle", "Election Results");
        ann.put("content", "Final election results have been published. Tap to view winners.");
        ann.put("timestamp", Timestamp.now());
        ann.put("type", "urgent");
        ann.put("pinned", false);
        ann.put("dateText", "");

        db.collection("announcements").add(ann)
                .addOnSuccessListener(docRef -> {
                    // optional: toast or logging
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Published results but failed to create announcement: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Firestore document ids should not contain forward slashes; convert spaces to underscores etc.
     * Keep simple sanitization to create a valid doc id from position name.
     */
    private String sanitizeDocId(String s) {
        if (s == null) return java.util.UUID.randomUUID().toString();
        return s.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    // Simple callback used by archiveCurrentResults
    private interface ArchiveCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}
