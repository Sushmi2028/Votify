package com.example.collegeelectionsystem;

public class CandidateResult {
    public String candidateId;
    public String name;
    public String party;
    public int votes;

    public CandidateResult() { }

    public CandidateResult(String candidateId, String name, String party, int votes) {
        this.candidateId = candidateId;
        this.name = name;
        this.party = party;
        this.votes = votes;
    }
}
