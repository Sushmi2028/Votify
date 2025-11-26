package com.example.collegeelectionsystem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AnonymousVotingActivity extends AppCompatActivity {

    private RecyclerView recyclerVoting;
    private com.google.android.material.button.MaterialButton btnSubmitVote;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private PositionAdapter adapter;

    // SharedPreferences key where anonymous login should have stored the token document id
    private static final String PREFS = "votify_prefs";
    private static final String KEY_ANON_TOKEN_DOC_ID = "anon_token_doc_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anonymous_voting);

        // init views (must match IDs in activity_voting.xml)
        recyclerVoting = findViewById(R.id.recyclerVoting);
        btnSubmitVote = findViewById(R.id.btnSubmitVote);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Recycler setup
        recyclerVoting.setLayoutManager(new LinearLayoutManager(this));

        // Load candidates grouped by position
        loadCandidatesGroupedByPosition();
        btnLogout.setOnClickListener(v->performLogout());
        // Submit votes
        btnSubmitVote.setOnClickListener(v -> submitVotes());

        // Header back

        // Bottom navigation: highlight Vote and handle navigation to other screens
        bottomNavigation.setSelectedItemId(R.id.nav_vote);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
             if (id == R.id.nav_vote) {
                // already here
                return true;
            } else if (id == R.id.nav_results) {
                startActivity(new Intent(AnonymousVotingActivity.this, AnonymousResultsActivity.class));
                finish();
                return true;
            }
            return false;
        });

    }
    private void performLogout(){
        if (mAuth != null) {
            mAuth.signOut();
        }

        Intent intent = new Intent(AnonymousVotingActivity.this, StudentLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadCandidatesGroupedByPosition() {
        db.collection("candidate").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Candidate> all = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Candidate c = doc.toObject(Candidate.class);
                    // ensure candidate has its document id so we store candidateId for votes
                    c.setId(doc.getId());
                    all.add(c);
                }

                // Group by position
                Map<String, List<Candidate>> grouped = new HashMap<>();
                for (Candidate c : all) {
                    String pos = c.getPosition() != null ? c.getPosition() : "Others";
                    grouped.putIfAbsent(pos, new ArrayList<>());
                    grouped.get(pos).add(c);
                }

                List<String> positions = new ArrayList<>(grouped.keySet());
                adapter = new PositionAdapter(this, positions, grouped);
                recyclerVoting.setAdapter(adapter);
            } else {
                Toast.makeText(this, "Error loading candidates", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getVoterIdOrNull() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return prefs.getString(KEY_ANON_TOKEN_DOC_ID, null); // may be null if anonymous login did not save token id
        }
    }

    private void submitVotes() {
        if (adapter == null) {
            Toast.makeText(this, "Please wait — candidates are still loading.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Candidate> selected = adapter.getSelectedMap();
        if (selected == null || selected.isEmpty()) {
            Toast.makeText(this, "Please select at least one candidate", Toast.LENGTH_SHORT).show();
            return;
        }

        String voterId = getVoterIdOrNull();
        if (voterId == null || voterId.isEmpty()) {
            Toast.makeText(this, "Unable to identify voter. For anonymous login ensure token is used.", Toast.LENGTH_LONG).show();
            return;
        }

        // disable submit to prevent double taps
        btnSubmitVote.setEnabled(false);

        // Check existing votes by this voter to prevent duplicate voting for same positions
        db.collection("votes").whereEqualTo("voterId", voterId).get()
                .addOnSuccessListener(querySnapshot -> {
                    HashSet<String> already = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String pos = doc.getString("position");
                        if (pos != null) already.add(pos);
                    }

                    // find conflicts
                    List<String> conflicts = new ArrayList<>();
                    for (String pos : selected.keySet()) {
                        if (already.contains(pos)) conflicts.add(pos);
                    }

                    if (!conflicts.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < conflicts.size(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(conflicts.get(i));
                        }
                        String msg = "You have already voted for: " + sb.toString();
                        Toast.makeText(AnonymousVotingActivity.this, msg, Toast.LENGTH_LONG).show();
                        btnSubmitVote.setEnabled(true);
                        return;
                    }

                    // No conflicts, write votes in a batch
                    WriteBatch batch = db.batch();
                    boolean added = false;
                    for (Map.Entry<String, Candidate> e : selected.entrySet()) {
                        String pos = e.getKey();
                        Candidate c = e.getValue();
                        if (c == null) continue;
                        if (c.getId() == null || c.getId().isEmpty()) continue;

                        Map<String, Object> vote = new HashMap<>();
                        vote.put("candidateId", c.getId());
                        vote.put("position", pos);
                        vote.put("voterId", voterId);
                        vote.put("timestamp", Timestamp.now());

                        com.google.firebase.firestore.DocumentReference docRef = db.collection("votes").document();
                        batch.set(docRef, vote);
                        added = true;
                    }

                    if (!added) {
                        Toast.makeText(AnonymousVotingActivity.this, "No valid votes to submit.", Toast.LENGTH_SHORT).show();
                        btnSubmitVote.setEnabled(true);
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AnonymousVotingActivity.this, "Votes submitted successfully!", Toast.LENGTH_SHORT).show();
                                // Optionally navigate back to dashboard or show summary
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AnonymousVotingActivity.this, "Error submitting votes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                btnSubmitVote.setEnabled(true);
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AnonymousVotingActivity.this, "Error checking previous votes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSubmitVote.setEnabled(true);
                });
    }
}
