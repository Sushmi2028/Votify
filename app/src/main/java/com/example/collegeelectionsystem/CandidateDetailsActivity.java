package com.example.collegeelectionsystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Shows a candidate's details. Expects Intent extra "candidateId".
 */
public class CandidateDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvPartyPosition, tvAgenda, tvInfo;
    private Button btnContact;
    private FirebaseFirestore db;
    private Candidate loaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate_details);

        tvName = findViewById(R.id.tvDetailName);
        tvPartyPosition = findViewById(R.id.tvDetailPartyPosition);
        tvAgenda = findViewById(R.id.tvDetailAgenda);
        tvInfo = findViewById(R.id.tvDetailInfo);
        btnContact = findViewById(R.id.btnContact);

        db = FirebaseFirestore.getInstance();

        String cid = getIntent().getStringExtra("candidateId");
        if (cid == null || cid.isEmpty()) {
            Toast.makeText(this, "Candidate not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCandidate(cid);
    }

    private void loadCandidate(String id) {
        db.collection("candidate").document(id).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Candidate not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Candidate c = doc.toObject(Candidate.class);
            if (c == null) {
                Toast.makeText(this, "Invalid candidate data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            c.setId(doc.getId());
            loaded = c;
            bind(c);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void bind(Candidate c) {
        tvName.setText(safe(c.getName()));
        tvPartyPosition.setText(String.format("%s · %s", safe(c.getParty()), safe(c.getPosition())));
        tvAgenda.setText(safe(c.getAgenda()));
        tvInfo.setText(String.format("%s · %s · %s", safe(c.getDepartment()), safe(c.getYear()), safe(c.getEmail())));

        btnContact.setOnClickListener(v -> {
            String email = c.getEmail();
            if (email == null || email.isEmpty()) {
                Toast.makeText(this, "No email available", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Student Elections");
            startActivity(Intent.createChooser(intent, "Contact via"));
        });
    }

    private String safe(String s) { return s == null ? "" : s; }
}
