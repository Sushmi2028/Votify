package com.example.collegeelectionsystem;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * AddAnnouncementActivity
 *
 * Expects layout: res/layout/activity_add_announcement.xml
 * Required view IDs in that layout:
 *  - EditText: etTitle, etSubtitle, etContent, etDateText
 *  - RadioGroup: rgType (with radio buttons rbGeneral, rbInfo, rbUrgent)
 *  - CheckBox: cbPinned
 *  - Button: btnAddAnnouncement
 *
 * Writes a document to collection "announcements" with fields:
 *  title, subtitle, content, type, pinned, author, timestamp, dateText
 */
public class AddAnnouncementActivity extends AppCompatActivity {

    private static final String TAG = "AddAnnouncementAct";

    private EditText etTitle;
    private EditText etSubtitle;
    private EditText etContent;
    private EditText etDateText;
    private RadioGroup rgType;
    private CheckBox cbPinned;
    private Button btnAddAnnouncement;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure this matches the XML filename you put in res/layout
        setContentView(R.layout.activity_add_announcement);

        // bind views
        etTitle = findViewById(R.id.etTitle);
        etSubtitle = findViewById(R.id.etSubtitle);
        etContent = findViewById(R.id.etContent);
        etDateText = findViewById(R.id.etDateText);
        rgType = findViewById(R.id.rgType);
        cbPinned = findViewById(R.id.cbPinned);
        btnAddAnnouncement = findViewById(R.id.btnAddAnnouncement);

        db = FirebaseFirestore.getInstance();

        btnAddAnnouncement.setOnClickListener(v -> saveAnnouncement());
    }

    private void saveAnnouncement() {
        String title = safeText(etTitle);
        String subtitle = safeText(etSubtitle);
        String content = safeText(etContent);
        String dateText = safeText(etDateText);
        boolean pinned = cbPinned.isChecked();

        if (title.isEmpty()) {
            etTitle.requestFocus();
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            etContent.requestFocus();
            Toast.makeText(this, "Please enter announcement content", Toast.LENGTH_SHORT).show();
            return;
        }

        // determine type from radio group
        int checkedId = rgType.getCheckedRadioButtonId();
        String type = "general";
        // IDs in your XML: rbGeneral (default), rbInfo, rbUrgent
        if (checkedId == R.id.rbInfo) type = "info";
        else if (checkedId == R.id.rbUrgent) type = "urgent";

        // prepare document
        Map<String, Object> doc = new HashMap<>();
        doc.put("title", title);
        doc.put("subtitle", subtitle);
        doc.put("content", content);
        doc.put("type", type);
        doc.put("pinned", pinned);
        // author placeholder; replace with admin identity if available
        doc.put("author", "Admin");
        doc.put("timestamp", Timestamp.now());
        doc.put("dateText", dateText);

        // disable button to prevent duplicate submits
        btnAddAnnouncement.setEnabled(false);
        btnAddAnnouncement.setVisibility(View.INVISIBLE);

        db.collection("announcements")
                .add(doc)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Announcement saved", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Announcement saved id=" + ref.getId());

                    // OPTIONAL:
                    // If you later set up a secure server to send topic notifications,
                    // call it here with the announcement id/title/summary.
                    //
                    // Example (pseudo):
                    // notifyServer(ref.getId(), title, subtitle.isEmpty() ? content : subtitle);

                    // return success to caller and finish
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnAddAnnouncement.setEnabled(true);
                    btnAddAnnouncement.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed saving announcement", e);
                });
    }

    private String safeText(EditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }
}
