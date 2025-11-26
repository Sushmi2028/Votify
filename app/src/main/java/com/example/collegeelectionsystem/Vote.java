package com.example.collegeelectionsystem;

public class Vote {
    private String voterId;
    private String candidateName;
    private String position;

    public Vote() {}

    public Vote(String voterId, String candidateName, String position) {
        this.voterId = voterId;
        this.candidateName = candidateName;
        this.position = position;
    }

    public String getVoterId() { return voterId; }
    public String getCandidateName() { return candidateName; }
    public String getPosition() { return position; }
}
