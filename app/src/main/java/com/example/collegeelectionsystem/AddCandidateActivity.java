package com.example.collegeelectionsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AddCandidateActivity — party list comes from Firestore (collection "party")
 * Position list is a fixed array (provided by user).
 */
public class AddCandidateActivity extends AppCompatActivity {

    private EditText etName, etEmail, etYear, etDepartment, etAgenda;
    private Spinner spParty, spPosition;
    private Button btnSave;
    private FirebaseFirestore db;

    // fixed position list as requested
    private static final String[] POSITIONS = new String[]{
            "Student Body President",
            "Vice President",
            "General Secretary",
            "Treasurer (or Finance Secretary)",
            "Cultural Secretary (or Events Coordinator)",
            "Sports Secretary",
            "Academic Affairs Secretary",
            "Student Welfare Officer",
            "First-Year (or Fresher's) Representative",
            "International Student Representative"
    };

    private final List<String> partyList = new ArrayList<>();
    private ArrayAdapter<String> partyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_candidate);

        etName = findViewById(R.id.etCandidateName);
        etEmail = findViewById(R.id.etCandidateEmail);
        etYear = findViewById(R.id.etCandidateYear);
        etDepartment = findViewById(R.id.etCandidateDepartment);
        etAgenda = findViewById(R.id.etCandidateAgenda);

        spParty = findViewById(R.id.spCandidateParty);
        spPosition = findViewById(R.id.spCandidatePosition);

        btnSave = findViewById(R.id.btnSaveCandidate);

        db = FirebaseFirestore.getInstance();

        // Set up position spinner with fixed list
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, POSITIONS);
        posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPosition.setAdapter(posAdapter);

        // Set up party spinner with an adapter; initially contains a placeholder
        partyList.add("Loading parties...");
        partyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, partyList);
        partyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spParty.setAdapter(partyAdapter);

        // load parties from Firestore
        loadPartiesFromFirestore();

        btnSave.setOnClickListener(v -> saveCandidate());
    }

    private void loadPartiesFromFirestore() {
        db.collection("parties").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    partyList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Expecting a field "name" in each party document; adjust if different
                        String pname = doc.getString("name");
                        if (pname == null) {
                            // fallback to doc id if no name field
                            pname = doc.getId();
                        }
                        if (!partyList.contains(pname)) {
                            partyList.add(pname);
                        }
                    }

                    // If no parties exist in DB, at least keep "Independent"
                    if (partyList.isEmpty()) {
                        partyList.add("Independent");
                    }
                    partyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // If party load fails, fall back to a minimal list
                    partyList.clear();
                    partyList.add("Independent");
                    partyAdapter.notifyDataSetChanged();
                    Toast.makeText(AddCandidateActivity.this, "Failed loading parties: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveCandidate() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String party = (spParty.getSelectedItem() != null) ? spParty.getSelectedItem().toString() : "";
        String position = (spPosition.getSelectedItem() != null) ? spPosition.getSelectedItem().toString() : "";
        String year = etYear.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String agenda = etAgenda.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || party.isEmpty() || position.isEmpty()
                || year.isEmpty() || department.isEmpty() || agenda.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> candidate = new HashMap<>();
        candidate.put("name", name);
        candidate.put("email", email);
        candidate.put("party", party);
        candidate.put("position", position);
        candidate.put("year", year);
        candidate.put("department", department);
        candidate.put("agenda", agenda);

        btnSave.setEnabled(false);
        db.collection("candidate").add(candidate)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Candidate added successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }

    private void clearForm() {
        etName.setText("");
        etEmail.setText("");
        etYear.setText("");
        etDepartment.setText("");
        etAgenda.setText("");
        // reset spinners to first item
        spParty.setSelection(0);
        spPosition.setSelection(0);
    }
}
