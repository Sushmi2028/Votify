package com.example.collegeelectionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads candidates from Firestore, supports search & basic filtering.
 */
public class CandidatesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recycler;
    private CandidateAdapter adapter;
    private final List<Candidate> candidates = new ArrayList<>();
    private final List<Candidate> filtered = new ArrayList<>();

    private EditText etSearch;
    private Spinner spinnerPosition, spinnerParty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidates);

        db = FirebaseFirestore.getInstance();

        recycler = findViewById(R.id.recyclerCandidates);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        etSearch = findViewById(R.id.etSearch);
        spinnerPosition = findViewById(R.id.spinnerPosition);
        spinnerParty = findViewById(R.id.spinnerParty);

        adapter = new CandidateAdapter(this, filtered, c -> {
            Intent i = new Intent(CandidatesActivity.this, CandidateDetailsActivity.class);
            i.putExtra("candidateId", c.getId());
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        setupBottomNavigation();
        setupSearchAndFilters();

        loadCandidates();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_candidates);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent;
            if (id == R.id.nav_home) {
                intent = new Intent(CandidatesActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_candidates) {
                return true;
            } else if (id == R.id.nav_vote) {
                intent = new Intent(CandidatesActivity.this, VotingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_news) {
                intent = new Intent(CandidatesActivity.this, AnnouncementsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_results) {
                intent = new Intent(CandidatesActivity.this, ResultsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){
                applyFilters();
            }
            @Override public void afterTextChanged(Editable e){}
        });

        spinnerPosition.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) { applyFilters(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        spinnerParty.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) { applyFilters(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void loadCandidates() {
        db.collection("candidate").get().addOnSuccessListener(qs -> {
            candidates.clear();
            for (QueryDocumentSnapshot doc : qs) {
                Candidate c = doc.toObject(Candidate.class);
                c.setId(doc.getId());
                candidates.add(c);
            }

            // prepare spinners (positions & parties)
            Set<String> positions = new HashSet<>();
            Set<String> parties = new HashSet<>();
            positions.add("All Positions");
            parties.add("All Parties");

            for (Candidate c : candidates) {
                if (c.getPosition() != null) positions.add(c.getPosition());
                if (c.getParty() != null) parties.add(c.getParty());
            }

            ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(positions));
            posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPosition.setAdapter(posAdapter);

            ArrayAdapter<String> partyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(parties));
            partyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerParty.setAdapter(partyAdapter);

            // initial display
            applyFilters();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed loading candidates: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void applyFilters() {
        String q = etSearch.getText().toString().trim().toLowerCase();
        String posFilter = spinnerPosition.getSelectedItem() != null ? spinnerPosition.getSelectedItem().toString() : "All Positions";
        String partyFilter = spinnerParty.getSelectedItem() != null ? spinnerParty.getSelectedItem().toString() : "All Parties";

        filtered.clear();
        for (Candidate c : candidates) {
            boolean matches = true;

            if (!q.isEmpty()) {
                String combined = (safe(c.getName()) + " " + safe(c.getParty()) + " " + safe(c.getPosition())).toLowerCase();
                if (!combined.contains(q)) matches = false;
            }

            if (matches && !"All Positions".equals(posFilter) && c.getPosition() != null) {
                if (!posFilter.equals(c.getPosition())) matches = false;
            }

            if (matches && !"All Parties".equals(partyFilter) && c.getParty() != null) {
                if (!partyFilter.equals(c.getParty())) matches = false;
            }

            if (matches) filtered.add(c);
        }

        adapter.notifyDataSetChanged();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
