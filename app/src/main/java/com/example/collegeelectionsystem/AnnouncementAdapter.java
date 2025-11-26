package com.example.collegeelectionsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.Holder> {

    private final Context context;
    private final List<Announcement> list;
    private final OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Announcement a);
    }

    public AnnouncementAdapter(Context ctx, List<Announcement> items, OnItemClickListener listener) {
        this.context = ctx;
        this.list = items;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_announcement, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Announcement a = list.get(position);

        holder.tvTitle.setText(a.getTitle() != null ? a.getTitle() : "");
        holder.tvSubtitle.setText(a.getSubtitle() != null ? a.getSubtitle() : "");
        holder.tvContent.setText(a.getContent() != null ? a.getContent() : "");
        holder.tvDate.setText(a.getDateText() != null ? a.getDateText() : formatTimestamp(a.getTimestamp()));
        holder.tvAgo.setText(getTimeAgo(a.getTimestamp()));

        String type = a.getType() != null ? a.getType() : "general";
        if ("urgent".equalsIgnoreCase(type)) {
            holder.ivType.setImageResource(R.drawable.ic_error_outline);
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText("URGENT");
        } else if ("info".equalsIgnoreCase(type)) {
            holder.ivType.setImageResource(R.drawable.ic_info_outline);
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText("INFO");
        } else {
            holder.ivType.setImageResource(R.drawable.ic_announcement);
            holder.tvBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) onItemClickListener.onItemClick(a);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView ivType;
        TextView tvTitle, tvSubtitle, tvContent, tvDate, tvAgo, tvBadge;

        Holder(@NonNull View itemView) {
            super(itemView);
            ivType = itemView.findViewById(R.id.ivTypeIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAgo = itemView.findViewById(R.id.tvAgo);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        Date d = ts.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(d);
    }

    private String getTimeAgo(Timestamp ts) {
        if (ts == null) return "";
        long diffMillis = System.currentTimeMillis() - ts.toDate().getTime();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        if (minutes < 60) return minutes + " minutes ago";
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        if (hours < 24) return hours + " hours ago";
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);
        return days + " days ago";
    }
}
