package com.example.collegeelectionsystem;

import com.google.firebase.Timestamp;

public class Announcement {
    private String id;
    private String title;
    private String subtitle;
    private String content;
    private String type; // urgent | info | general
    private boolean pinned;
    private String author;
    private Timestamp timestamp;
    private String dateText;

    public Announcement() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getDateText() { return dateText; }
    public void setDateText(String dateText) { this.dateText = dateText; }
}
