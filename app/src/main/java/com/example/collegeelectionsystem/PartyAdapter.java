package com.example.collegeelectionsystem;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PartyAdapter extends RecyclerView.Adapter<PartyAdapter.PartyViewHolder> {

    private final Context context;
    private final List<Party> partyList;

    public PartyAdapter(Context context, List<Party> partyList) {
        this.context = context;
        this.partyList = partyList;
    }

    @NonNull
    @Override
    public PartyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_party, parent, false);
        return new PartyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PartyViewHolder holder, int position) {
        Party party = partyList.get(position);
        holder.tvName.setText(party.getName());
        holder.tvDescription.setText(party.getDescription());

        try {
            holder.tvColor.setBackgroundColor(Color.parseColor(party.getColor()));
        } catch (Exception e) {
            holder.tvColor.setBackgroundColor(Color.GRAY); // fallback if invalid color
        }
    }

    @Override
    public int getItemCount() {
        return partyList.size();
    }

    public static class PartyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvColor;

        public PartyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPartyName);
            tvDescription = itemView.findViewById(R.id.tvPartyDescription);
            tvColor = itemView.findViewById(R.id.tvPartyColor);
        }
    }
}
