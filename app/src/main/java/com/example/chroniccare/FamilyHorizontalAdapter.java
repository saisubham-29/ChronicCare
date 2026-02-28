package com.example.chroniccare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chroniccare.database.FamilyMember;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;

public class FamilyHorizontalAdapter extends RecyclerView.Adapter<FamilyHorizontalAdapter.ViewHolder> {

    private Context context;
    private List<FamilyMember> members = new ArrayList<>();
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(FamilyMember member);
    }

    public FamilyHorizontalAdapter(Context context, OnMemberClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setMembers(List<FamilyMember> members) {
        this.members = members != null ? members : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_member_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyMember member = members.get(position);
        
        holder.tvMemberName.setText(member.name != null ? member.name : "Unknown");
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberClick(member);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivMemberPic;
        TextView tvMemberName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberPic = itemView.findViewById(R.id.ivMemberPic);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
        }
    }
}
