package com.example.collegeelectionsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingAdapter extends RecyclerView.Adapter<VotingAdapter.VotingViewHolder> {

    private final Context context;
    private final List<Candidate> candidates;
    private final Map<String, Candidate> selectedVotes = new HashMap<>();

    public VotingAdapter(Context context, List<Candidate> candidates) {
        this.context = context;
        this.candidates = candidates;
    }

    @NonNull
    @Override
    public VotingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_candidate_vote, parent, false);
        return new VotingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VotingViewHolder holder, int position) {
        Candidate c = candidates.get(position);
        holder.tvName.setText(c.getName());
        holder.tvParty.setText(c.getParty());
        holder.tvPosition.setText(c.getPosition());

        holder.rbSelect.setOnCheckedChangeListener(null);
        holder.rbSelect.setChecked(selectedVotes.containsKey(c.getPosition()) &&
                selectedVotes.get(c.getPosition()).getId().equals(c.getId()));

        holder.rbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedVotes.put(c.getPosition(), c);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return candidates.size();
    }

    public Map<String, Candidate> getSelectedVotes() {
        return selectedVotes;
    }

    static class VotingViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvParty, tvPosition;
        RadioButton rbSelect;
        ImageView ivPhoto;

        public VotingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvParty = itemView.findViewById(R.id.tvParty);
            tvPosition = itemView.findViewById(R.id.tvPosition);
            rbSelect = itemView.findViewById(R.id.rbSelect);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
        }
    }
}
