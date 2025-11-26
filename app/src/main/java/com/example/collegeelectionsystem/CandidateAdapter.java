package com.example.collegeelectionsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adapter for candidate list.
 */
public class CandidateAdapter extends RecyclerView.Adapter<CandidateAdapter.VH> {

    public interface OnItemClick {
        void onClick(Candidate c);
    }

    private final Context ctx;
    private final List<Candidate> items;
    private final OnItemClick listener;

    public CandidateAdapter(Context ctx, List<Candidate> items, OnItemClick listener) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_candidate, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Candidate c = items.get(position);
        holder.tvName.setText(c.getName() != null ? c.getName() : "—");
        holder.tvParty.setText(c.getParty() != null ? c.getParty() : "Independent");
        holder.tvPosition.setText(c.getPosition() != null ? c.getPosition() : "");
        holder.tvYear.setText(c.getYear() != null ? c.getYear() : "");

        // If you later store avatar URL, you can load using Glide/Picasso.
        holder.ivAvatar.setImageResource(R.drawable.ic_person_placeholder);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvParty, tvPosition, tvYear;

        VH(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivCandidateAvatar);
            tvName = itemView.findViewById(R.id.tvCandidateName);
            tvParty = itemView.findViewById(R.id.tvCandidateParty);
            tvPosition = itemView.findViewById(R.id.tvCandidatePosition);
            tvYear = itemView.findViewById(R.id.tvCandidateYear);
        }
    }
}
