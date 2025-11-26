package com.example.collegeelectionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * AnnouncementsActivity
 * - Layout: res/layout/activity_announcement.xml (must contain recyclerAnnouncements and bottomNavigation)
 * - Uses AnnouncementAdapter to display items (click opens AnnouncementDetailActivity)
 */
public class AnnouncementsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recycler;
    private AnnouncementAdapter adapter;
    private final List<Announcement> announcements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        db = FirebaseFirestore.getInstance();

        recycler = findViewById(R.id.recyclerAnnouncements);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnnouncementAdapter(this, announcements, a -> {
            // open detail on click
            Intent i = new Intent(AnnouncementsActivity.this, AnnouncementDetailActivity.class);
            i.putExtra(AnnouncementDetailActivity.EXTRA_ANNOUNCEMENT_ID, a.getId());
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        setupBottomNavigation();

        loadAnnouncements();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        // mark current tab (news)
        bottomNav.setSelectedItemId(R.id.nav_news);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent;
            if (id == R.id.nav_home) {
                intent = new Intent(AnnouncementsActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_candidates) {
                intent = new Intent(AnnouncementsActivity.this, CandidatesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_vote) {
                intent = new Intent(AnnouncementsActivity.this, VotingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_news) {
                // already here
                return true;
            } else if (id == R.id.nav_results) {
                intent = new Intent(AnnouncementsActivity.this, ResultsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void loadAnnouncements() {
        // Fetch all announcements ordered by timestamp desc (newest first)
        db.collection("announcements")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    announcements.clear();
                    for (QueryDocumentSnapshot doc : qs) {
                        Announcement a = doc.toObject(Announcement.class);
                        a.setId(doc.getId());
                        announcements.add(a);
                    }

                    // Sort: pinned first, then by timestamp desc (already mostly ordered by timestamp)
                    Collections.sort(announcements, new Comparator<Announcement>() {
                        @Override
                        public int compare(Announcement a1, Announcement a2) {
                            boolean p1 = a1 != null && a1.isPinned();
                            boolean p2 = a2 != null && a2.isPinned();
                            if (p1 != p2) {
                                return p1 ? -1 : 1; // pinned first
                            }
                            // Both same pinned state -> order by timestamp desc
                            if (a1.getTimestamp() == null && a2.getTimestamp() == null) return 0;
                            if (a1.getTimestamp() == null) return 1;
                            if (a2.getTimestamp() == null) return -1;
                            return a2.getTimestamp().compareTo(a1.getTimestamp());
                        }
                    });

                    adapter.notifyDataSetChanged();

                    if (announcements.isEmpty()) {
                        Toast.makeText(this, "No announcements available.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed loading announcements: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
