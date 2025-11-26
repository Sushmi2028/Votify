package com.example.collegeelectionsystem;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StudentRegisterStep1Activity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register_step1);

        EditText etStudentId = findViewById(R.id.etStudentId);
        Button btnVerifyId = findViewById(R.id.btnVerifyId);
        TextView btnBackToLogin = findViewById(R.id.tvBackToLogin);

        // Verify ID Button Click
        btnVerifyId.setOnClickListener(v -> {
            String studentId = etStudentId.getText().toString().trim();

            if (studentId.isEmpty()) {
                Toast.makeText(this, "Please enter Student ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if ID is exactly 9 digits
            if (!studentId.matches("^[0-9]{9}$")) {
                Toast.makeText(this, "Invalid Student ID (must be 9 digits)", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Valid ID → move to Step 2 (Complete Profile)
            Intent intent = new Intent(StudentRegisterStep1Activity.this, StudentRegisterStep2Activity.class);
            intent.putExtra("studentId", studentId); // pass to next activity
            startActivity(intent);
        });

        // Back to Login
        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(StudentRegisterStep1Activity.this, StudentLoginActivity.class));
            finish();
        });
    }
}
