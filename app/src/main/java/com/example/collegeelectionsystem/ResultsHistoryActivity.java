package com.example.collegeelectionsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ResultsHistoryActivity extends AppCompatActivity {
    private ListView list;
    private FirebaseFirestore db;
    private List<String> ids = new ArrayList<>();
    private List<String> labels = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_history);
        list = findViewById(R.id.listArchives);
        db = FirebaseFirestore.getInstance();

        db.collection("election_results").orderBy("createdAt").get().addOnSuccessListener(sn -> {
            ids.clear(); labels.clear();
            sn.forEach(d -> {
                ids.add(d.getId());
                labels.add(d.getString("label")!=null? d.getString("label"): d.getId());
            });
            list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        });

        list.setOnItemClickListener((a, v, pos, id) -> {
            Intent i = new Intent(this, ResultsActivity.class);
            i.putExtra("archiveId", ids.get(pos)); // ResultsActivity will detect archive mode
            startActivity(i);
        });
    }
}
