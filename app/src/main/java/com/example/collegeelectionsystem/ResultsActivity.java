package com.example.collegeelectionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ResultsActivity with header + bottom navigation wired.
 * Loads votes and candidate info and displays per-position results.
 * Also respects settings/election.resultsPublished flag.
 */
public class ResultsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView tvEmpty;
    private ResultsAdapter adapter;
    private final List<ResultsAdapter.PositionResults> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        db = FirebaseFirestore.getInstance();

        recycler = findViewById(R.id.recyclerResults);
        progress = findViewById(R.id.progressLoading);
        tvEmpty = findViewById(R.id.tvEmpty);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultsAdapter(this, data);
        recycler.setAdapter(adapter);

        setupBottomNavigation();

        checkIfResultsPublishedAndLoad();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            // mark current tab (results) if present in menu
            try {
                bottomNav.setSelectedItemId(R.id.nav_results);
            } catch (Exception ignored) { }

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent;
                if (id == R.id.nav_home) {
                    intent = new Intent(ResultsActivity.this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_candidates) {
                    intent = new Intent(ResultsActivity.this, CandidatesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_vote) {
                    intent = new Intent(ResultsActivity.this, VotingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_news) {
                    intent = new Intent(ResultsActivity.this, AnnouncementsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_results) {
                    // already here
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Check settings/election.resultsPublished before loading results.
     * If document not present or published==false -> show message.
     */
    private void checkIfResultsPublishedAndLoad() {
        progress.setVisibility(android.view.View.VISIBLE);
        tvEmpty.setVisibility(android.view.View.GONE);

        db.collection("settings").document("election").get()
                .addOnSuccessListener(doc -> {
                    boolean published = false;
                    if (doc != null && doc.exists()) {
                        Boolean b = doc.getBoolean("resultsPublished");
                        published = (b != null && b);
                    }
                    if (published) {
                        // proceed to load actual votes & candidates
                        loadVotesAndCandidates();
                    } else {
                        progress.setVisibility(android.view.View.GONE);
                        tvEmpty.setVisibility(android.view.View.VISIBLE);
                        tvEmpty.setText("Results are not published yet.");
                    }
                })
                .addOnFailureListener(e -> {
                    // If check fails, still attempt to load (or you may choose to block)
                    progress.setVisibility(android.view.View.GONE);
                    Toast.makeText(ResultsActivity.this, "Failed to check results status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadVotesAndCandidates() {
        progress.setVisibility(android.view.View.VISIBLE);
        tvEmpty.setVisibility(android.view.View.GONE);

        // Step 1: read votes -> map position -> (candidateId -> count)
        db.collection("votes").get()
                .addOnSuccessListener(voteSnap -> {
                    Map<String, Map<String, Integer>> positionVotes = new HashMap<>();

                    for (QueryDocumentSnapshot vdoc : voteSnap) {
                        String pos = vdoc.getString("position");
                        String candidateId = vdoc.getString("candidateId");
                        if (pos == null || candidateId == null) continue;

                        positionVotes.putIfAbsent(pos, new HashMap<>());
                        Map<String, Integer> cm = positionVotes.get(pos);
                        cm.put(candidateId, cm.getOrDefault(candidateId, 0) + 1);
                    }

                    if (positionVotes.isEmpty()) {
                        progress.setVisibility(android.view.View.GONE);
                        tvEmpty.setVisibility(android.view.View.VISIBLE);
                        tvEmpty.setText("No votes cast yet.");
                        return;
                    }

                    // Step 2: load all candidates and fill name/party for candidate ids
                    db.collection("candidate").get()
                            .addOnSuccessListener(candSnap -> {
                                // map candidateId -> Candidate
                                Map<String, Candidate> candMap = new HashMap<>();
                                for (QueryDocumentSnapshot cdoc : candSnap) {
                                    Candidate c = cdoc.toObject(Candidate.class);
                                    if (c != null) {
                                        c.setId(cdoc.getId());
                                        candMap.put(cdoc.getId(), c);
                                    }
                                }

                                // Build per-position CandidateResult lists using ResultsAdapter.CandidateResult
                                data.clear();
                                for (Map.Entry<String, Map<String, Integer>> entry : positionVotes.entrySet()) {
                                    String position = entry.getKey();
                                    Map<String, Integer> cm = entry.getValue();

                                    List<ResultsAdapter.CandidateResult> crList = new ArrayList<>();
                                    for (Map.Entry<String, Integer> e : cm.entrySet()) {
                                        String cid = e.getKey();
                                        int cnt = e.getValue();
                                        Candidate cinfo = candMap.get(cid);
                                        String name = (cinfo != null && cinfo.getName() != null) ? cinfo.getName() : "Candidate";
                                        String party = (cinfo != null) ? cinfo.getParty() : null;
                                        crList.add(new ResultsAdapter.CandidateResult(cid, name, party, cnt));
                                    }

                                    // sort by votes desc
                                    crList.sort((a, b) -> Integer.compare(b.votes, a.votes));

                                    // convert into adapter's PositionResults container
                                    data.add(new ResultsAdapter.PositionResults(position, crList));
                                }

                                // refresh UI
                                adapter.notifyDataSetChanged();
                                progress.setVisibility(android.view.View.GONE);
                                if (data.isEmpty()) {
                                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                                    tvEmpty.setText("No results available.");
                                } else {
                                    tvEmpty.setVisibility(android.view.View.GONE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                progress.setVisibility(android.view.View.GONE);
                                Toast.makeText(this, "Failed loading candidates: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(android.view.View.GONE);
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    tvEmpty.setText("Failed to load votes: " + e.getMessage());
                });
    }
}
