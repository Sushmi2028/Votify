package com.example.collegeelectionsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for showing election results grouped by position.
 * Each position shows candidate cards with name, party, and votes.
 * Winner is shown under the candidate list.
 */
public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.PositionViewHolder> {

    private final Context context;
    private final List<PositionResults> data;

    public ResultsAdapter(Context context, List<PositionResults> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public PositionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_position_result, parent, false);
        return new PositionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PositionViewHolder holder, int position) {
        PositionResults pr = data.get(position);
        holder.tvPositionTitle.setText(pr.position);

        // Clear previous candidate views before reusing the holder
        holder.container.removeAllViews();

        for (CandidateResult cr : pr.candidates) {
            View card = LayoutInflater.from(context).inflate(R.layout.item_candidate_result, holder.container, false);

            TextView tvName = card.findViewById(R.id.tvCandidateName);
            TextView tvParty = card.findViewById(R.id.tvCandidateParty);
            TextView tvVotes = card.findViewById(R.id.tvCandidateVotes);

            tvName.setText(cr.name);
            tvParty.setText(cr.party != null ? cr.party : "Independent");
            tvVotes.setText(String.valueOf(cr.votes));

            // Highlight top candidate (winner)
            if (!pr.candidates.isEmpty() && pr.candidates.get(0) == cr) {
                card.setBackgroundResource(R.drawable.winner_highlight_bg);
            } else {
                // reset background for non-winners (in case view is recycled)
                card.setBackgroundResource(android.R.color.transparent);
            }

            holder.container.addView(card);
        }

        // --- set winner summary under the candidate list ---
        if (pr.candidates != null && !pr.candidates.isEmpty()) {
            CandidateResult winner = pr.candidates.get(0);
            String partyText = (winner.party != null && !winner.party.isEmpty()) ? (" — " + winner.party) : "";
            String votesText = " (" + winner.votes + " votes)";
            holder.tvWinner.setText("Winner: " + winner.name + partyText + votesText);
            holder.tvWinner.setVisibility(View.VISIBLE);
        } else {
            holder.tvWinner.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // ---------- INNER CLASSES ----------

    /** Represents one position with a list of candidate results */
    public static class PositionResults {
        public String position;
        public List<CandidateResult> candidates;

        public PositionResults(String position, List<CandidateResult> candidates) {
            this.position = position;
            this.candidates = candidates;
        }
    }

    /** Represents a candidate's result details */
    public static class CandidateResult {
        public String id;
        public String name;
        public String party;
        public int votes;

        public CandidateResult(String id, String name, String party, int votes) {
            this.id = id;
            this.name = name;
            this.party = party;
            this.votes = votes;
        }
    }

    // ---------- VIEW HOLDER ----------
    static class PositionViewHolder extends RecyclerView.ViewHolder {
        TextView tvPositionTitle;
        LinearLayout container;
        TextView tvWinner;

        public PositionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPositionTitle = itemView.findViewById(R.id.tvPositionTitle);
            container = itemView.findViewById(R.id.layoutCandidatesContainer);
            tvWinner = itemView.findViewById(R.id.tvWinner);
        }
    }
}
