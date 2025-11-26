package com.example.collegeelectionsystem;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import yuku.ambilwarna.AmbilWarnaDialog;

import java.util.HashMap;
import java.util.Map;

public class AddPartyActivity extends AppCompatActivity {

    private EditText etName, etDescription;
    private TextView tvSelectedColor;
    private FirebaseFirestore db;
    private int selectedColor = Color.BLUE; // default color

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_party);

        etName = findViewById(R.id.etPartyName);
        etDescription = findViewById(R.id.etPartyDescription);
        Button btnSave = findViewById(R.id.btnSaveParty);
        Button btnPickColor = findViewById(R.id.btnPickColor);
        tvSelectedColor = findViewById(R.id.tvSelectedColor);

        db = FirebaseFirestore.getInstance();

        btnPickColor.setOnClickListener(v -> openColorPicker());
        btnSave.setOnClickListener(v -> saveParty());
    }

    private void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, selectedColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                // Do nothing
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                selectedColor = color;
                String hexColor = String.format("#%06X", (0xFFFFFF & color));
                tvSelectedColor.setText("Selected Color: " + hexColor);
                tvSelectedColor.setBackgroundColor(color);
            }
        });
        colorPicker.show();
    }

    private void saveParty() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String hexColor = String.format("#%06X", (0xFFFFFF & selectedColor));

        if (name.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> party = new HashMap<>();
        party.put("name", name);
        party.put("description", description);
        party.put("color", hexColor);

        db.collection("parties").add(party)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Party added successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
