package com.example.collegeelectionsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter used in admin screen with delete button for each candidate.
 */
public class CandidateAdminAdapter extends RecyclerView.Adapter<CandidateAdminAdapter.VH> {

    public interface OnDeleteClickListener {
        void onDeleteClicked(int position);
    }

    private final Context context;
    private final List<Candidate> data;
    private final OnDeleteClickListener listener;

    public CandidateAdminAdapter(Context context, List<Candidate> data, OnDeleteClickListener listener) {
        this.context = context;
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_candidate_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Candidate c = data.get(position);
        holder.tvName.setText(c.getName() != null ? c.getName() : "Unknown");
        holder.tvParty.setText(c.getParty() != null ? c.getParty() : "Independent");
        holder.tvPosition.setText(c.getPosition() != null ? c.getPosition() : "");

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvParty, tvPosition;
        ImageButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAdminCandidateName);
            tvParty = itemView.findViewById(R.id.tvAdminCandidateParty);
            tvPosition = itemView.findViewById(R.id.tvAdminCandidatePosition);
            btnDelete = itemView.findViewById(R.id.btnDeleteCandidate);
        }
    }
}
