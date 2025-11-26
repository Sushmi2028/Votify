package com.example.collegeelectionsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter that shows one RecyclerView item per position.
 * Each item contains a RadioGroup populated with that position's candidates.
 * Selected candidate per position is stored in selectedMap.
 */
public class PositionAdapter extends RecyclerView.Adapter<PositionAdapter.PositionViewHolder> {

    private final Context context;
    private final List<String> positions; // list of position names in order
    private final Map<String, List<Candidate>> groupedCandidates; // position -> list of candidates
    private final Map<String, Candidate> selectedMap = new HashMap<>(); // position -> selected candidate

    public PositionAdapter(Context ctx, List<String> positions, Map<String, List<Candidate>> grouped) {
        this.context = ctx;
        this.positions = positions;
        this.groupedCandidates = grouped;
    }

    @NonNull
    @Override
    public PositionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_position, parent, false);
        return new PositionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PositionViewHolder holder, int positionIndex) {
        String positionName = positions.get(positionIndex);
        holder.tvPositionTitle.setText(positionName);

        // Temporarily remove listener while we populate to avoid callbacks
        holder.rgCandidates.setOnCheckedChangeListener(null);
        holder.rgCandidates.removeAllViews();

        List<Candidate> list = groupedCandidates.get(positionName);
        if (list == null || list.isEmpty()) {
            // nothing to show
            return;
        }

        // Create radio buttons for candidates
        for (Candidate c : list) {
            RadioButton rb = new RadioButton(context);
            rb.setId(View.generateViewId());
            String label = (c.getName() != null ? c.getName() : "Candidate")
                    + " (" + (c.getParty() != null ? c.getParty() : "Independent") + ")";
            rb.setText(label);
            rb.setTag(c); // store candidate object for retrieval
            rb.setPadding(8, 12, 8, 12);
            holder.rgCandidates.addView(rb);

            // If previously selected for this position, restore checked state
            Candidate selected = selectedMap.get(positionName);
            if (selected != null && selected.getId() != null && selected.getId().equals(c.getId())) {
                rb.setChecked(true);
            }
        }

        // Now set listener to update selectedMap when user chooses one
        holder.rgCandidates.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton chosen = group.findViewById(checkedId);
            if (chosen != null) {
                Object tag = chosen.getTag();
                if (tag instanceof Candidate) {
                    selectedMap.put(positionName, (Candidate) tag);
                }
            } else {
                selectedMap.remove(positionName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return positions.size();
    }

    /**
     * Returns a mutable map of currently selected candidates per position.
     * Key = position name, Value = Candidate object selected for that position.
     */
    public Map<String, Candidate> getSelectedMap() {
        return selectedMap;
    }

    static class PositionViewHolder extends RecyclerView.ViewHolder {
        TextView tvPositionTitle;
        RadioGroup rgCandidates;

        public PositionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPositionTitle = itemView.findViewById(R.id.tvPositionTitle);
            rgCandidates = itemView.findViewById(R.id.rgCandidates);
        }
    }
}
