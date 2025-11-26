package com.example.collegeelectionsystem;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminCandidatesActivity - lists candidates and allows admin to delete a candidate.
 *
 * Assumptions:
 * - Candidate collection is "candidate" and candidate doc ID is used as candidate id.
 * - Votes collection is "votes" and each vote document has a field "candidateId" linking to candidate doc id.
 */
public class AdminCandidatesActivity extends AppCompatActivity implements CandidateAdminAdapter.OnDeleteClickListener {

    private FirebaseFirestore db;
    private RecyclerView recycler;
    private ProgressBar progress;
    private CandidateAdminAdapter adapter;
    private final List<Candidate> candidates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_candidates); // create layout or reuse existing

        db = FirebaseFirestore.getInstance();

        recycler = findViewById(R.id.recyclerCandidatesAdmin);
        progress = findViewById(R.id.progressCandidatesAdmin);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CandidateAdminAdapter(this, candidates, this);
        recycler.setAdapter(adapter);

        loadCandidates();
    }

    private void loadCandidates() {
        progress.setVisibility(View.VISIBLE);
        db.collection("candidate").get()
                .addOnSuccessListener(querySnapshot -> {
                    candidates.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Candidate c = doc.toObject(Candidate.class);
                        c.setId(doc.getId()); // ensure Candidate model has setId
                        candidates.add(c);
                    }
                    adapter.notifyDataSetChanged();
                    progress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(AdminCandidatesActivity.this, "Failed to load candidates: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Called by adapter when delete icon is tapped.
     */
    @Override
    public void onDeleteClicked(int position) {
        if (position < 0 || position >= candidates.size()) return;
        Candidate toDelete = candidates.get(position);

        // Confirm deletion
        new AlertDialog.Builder(this)
                .setTitle("Delete Candidate")
                .setMessage("Are you sure you want to delete \"" + toDelete.getName() + "\"? This will also remove related votes.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCandidate(toDelete, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes candidate doc and associated votes in a batch.
     * Note: if there are many votes (>500) you should delete in multiple batches (not handled here).
     */
    private void deleteCandidate(Candidate candidate, int positionInList) {
        progress.setVisibility(View.VISIBLE);

        String candidateId = candidate.getId();
        if (candidateId == null || candidateId.isEmpty()) {
            progress.setVisibility(View.GONE);
            Toast.makeText(this, "Invalid candidate id", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference votesRef = db.collection("votes");
        // First query votes for this candidate, then create a batch to delete them + candidate doc
        votesRef.whereEqualTo("candidateId", candidateId).get()
                .addOnSuccessListener(voteSnap -> {
                    WriteBatch batch = db.batch();

                    // delete each vote doc
                    for (QueryDocumentSnapshot vdoc : voteSnap) {
                        batch.delete(vdoc.getReference());
                    }

                    // delete candidate doc
                    batch.delete(db.collection("candidate").document(candidateId));

                    // commit batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                // remove locally and update UI
                                candidates.remove(positionInList);
                                adapter.notifyItemRemoved(positionInList);
                                progress.setVisibility(View.GONE);
                                Toast.makeText(AdminCandidatesActivity.this, "Candidate and related votes deleted.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progress.setVisibility(View.GONE);
                                Toast.makeText(AdminCandidatesActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(AdminCandidatesActivity.this, "Failed to fetch votes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
