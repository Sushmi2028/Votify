package com.example.collegeelectionsystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AnnouncementDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ANNOUNCEMENT_ID = "announcementId";

    private ImageView ivType;
    private TextView tvTitle, tvSubtitle, tvContent, tvDate, tvAgo, tvAuthor;
    private ProgressBar progressBar;
    private Button btnShare;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcement_detail);

        ivType = findViewById(R.id.ivTypeIconDetail);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvSubtitle = findViewById(R.id.tvDetailSubtitle);
        tvContent = findViewById(R.id.tvDetailContent);
        tvDate = findViewById(R.id.tvDetailDate);
        tvAgo = findViewById(R.id.tvDetailAgo);
        tvAuthor = findViewById(R.id.tvDetailAuthor);
        progressBar = findViewById(R.id.progressLoading);
        btnShare = findViewById(R.id.btnShare);

        db = FirebaseFirestore.getInstance();

        String announcementId = getIntent().getStringExtra(EXTRA_ANNOUNCEMENT_ID);
        if (announcementId == null || announcementId.isEmpty()) {
            Toast.makeText(this, "Announcement not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadAnnouncement(announcementId);

        btnShare.setOnClickListener(v -> shareCurrentAnnouncement());
    }

    // cached for sharing
    private Announcement loadedAnnouncement = null;

    private void loadAnnouncement(String id) {
        progressBar.setVisibility(View.VISIBLE);
        // try server then cache fallback
        db.collection("announcements").document(id)
                .get(Source.SERVER) // try server first for freshest
                .addOnSuccessListener(this::onDocLoaded)
                .addOnFailureListener(serverEx -> {
                    // try cache if server failed
                    db.collection("announcements").document(id)
                            .get(Source.CACHE)
                            .addOnSuccessListener(this::onDocLoaded)
                            .addOnFailureListener(cacheEx -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed to load announcement", Toast.LENGTH_LONG).show();
                            });
                });
    }

    private void onDocLoaded(@NonNull DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);
        if (!doc.exists()) {
            Toast.makeText(this, "Announcement not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Announcement a = doc.toObject(Announcement.class);
        if (a == null) {
            Toast.makeText(this, "Invalid announcement data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        a.setId(doc.getId());
        loadedAnnouncement = a;

        tvTitle.setText(nonNull(a.getTitle()));
        tvSubtitle.setText(nonNull(a.getSubtitle()));
        tvContent.setText(nonNull(a.getContent()));
        tvAuthor.setText(a.getAuthor() != null ? "By " + a.getAuthor() : "");

        // date formatting
        if (a.getDateText() != null && !a.getDateText().isEmpty()) {
            tvDate.setText(a.getDateText());
        } else if (a.getTimestamp() != null) {
            tvDate.setText(formatTimestamp(a.getTimestamp()));
        } else {
            tvDate.setText("");
        }

        tvAgo.setText(getTimeAgo(a.getTimestamp()));

        // type icon / badge
        String type = a.getType() != null ? a.getType() : "general";
        if ("urgent".equalsIgnoreCase(type)) {
            ivType.setImageResource(R.drawable.ic_error_outline);
        } else if ("info".equalsIgnoreCase(type)) {
            ivType.setImageResource(R.drawable.ic_info_outline);
        } else {
            ivType.setImageResource(R.drawable.ic_announcement);
        }
    }

    private String nonNull(String s) {
        return s == null ? "" : s;
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        Date d = ts.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(d);
    }

    private String getTimeAgo(Timestamp ts) {
        if (ts == null) return "";
        long diffMillis = System.currentTimeMillis() - ts.toDate().getTime();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        if (minutes < 60) return minutes + " minutes ago";
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        if (hours < 24) return hours + " hours ago";
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);
        return days + " days ago";
    }

    private void shareCurrentAnnouncement() {
        if (loadedAnnouncement == null) {
            Toast.makeText(this, "Nothing to share yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String subject = loadedAnnouncement.getTitle() != null ? loadedAnnouncement.getTitle() : "Announcement";
        StringBuilder body = new StringBuilder();
        if (loadedAnnouncement.getSubtitle() != null && !loadedAnnouncement.getSubtitle().isEmpty()) {
            body.append(loadedAnnouncement.getSubtitle()).append("\n\n");
        }
        if (loadedAnnouncement.getContent() != null) {
            body.append(loadedAnnouncement.getContent()).append("\n\n");
        }
        if (loadedAnnouncement.getDateText() != null && !loadedAnnouncement.getDateText().isEmpty()) {
            body.append("Date: ").append(loadedAnnouncement.getDateText()).append("\n");
        } else if (loadedAnnouncement.getTimestamp() != null) {
            body.append("Date: ").append(formatTimestamp(loadedAnnouncement.getTimestamp())).append("\n");
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, subject);
        share.putExtra(Intent.EXTRA_TEXT, body.toString());
        startActivity(Intent.createChooser(share, "Share announcement via"));
    }
}
