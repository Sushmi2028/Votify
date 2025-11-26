package com.example.collegeelectionsystem;

public class Candidate {
    private String id;   // Firestore document ID
    private String name;
    private String email;
    private String party;
    private String position;
    private String year;
    private String department;
    private String agenda;

    // Required empty constructor for Firestore
    public Candidate() {}

    // Constructor with fields (optional, for manual creation)
    public Candidate(String id, String name, String email, String party,
                     String position, String year, String department, String agenda) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.party = party;
        this.position = position;
        this.year = year;
        this.department = department;
        this.agenda = agenda;
    }

    // ✅ Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getParty() { return party; }
    public void setParty(String party) { this.party = party; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getAgenda() { return agenda; }
    public void setAgenda(String agenda) { this.agenda = agenda; }
}
