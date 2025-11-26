package com.example.collegeelectionsystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PartiesActivity extends AppCompatActivity {

    private PartyAdapter adapter;
    private List<Party> partyList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parties);

        RecyclerView recyclerParties = findViewById(R.id.recyclerParties);
        recyclerParties.setLayoutManager(new LinearLayoutManager(this));

        partyList = new ArrayList<>();
        adapter = new PartyAdapter(this, partyList);
        recyclerParties.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadParties();
    }

    private void loadParties() {
        db.collection("parties").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                partyList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Party party = doc.toObject(Party.class);
                    party.setId(doc.getId()); // set Firestore doc ID
                    partyList.add(party);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Error loading parties", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
