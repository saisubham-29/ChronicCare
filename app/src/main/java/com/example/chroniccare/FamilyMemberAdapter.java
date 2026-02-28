package com.example.chroniccare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chroniccare.database.FamilyMember;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;

public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.ViewHolder> {

    private Context context;
    private List<FamilyMember> familyMembers = new ArrayList<>();
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(FamilyMember member);
        void onMoreClick(FamilyMember member, View anchorView);
    }

    public FamilyMemberAdapter(Context context, OnMemberClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFamilyMembers(List<FamilyMember> members) {
        this.familyMembers = members != null ? members : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyMember member = familyMembers.get(position);
        
        holder.tvMemberName.setText(member.name != null ? member.name : "Unknown");
        holder.tvMemberRelation.setText(member.relationship != null ? member.relationship : "Family");

        holder.tvPersonalAccess.setVisibility(member.canViewPersonalInfo ? View.VISIBLE : View.GONE);
        holder.tvMedicalAccess.setVisibility(member.canViewMedicalInfo ? View.VISIBLE : View.GONE);
        holder.tvEmergencyAccess.setVisibility(member.canViewEmergencyInfo ? View.VISIBLE : View.GONE);
        holder.tvGdprAccess.setVisibility(member.canViewGdprInfo ? View.VISIBLE : View.GONE);

        holder.tvCaretakerBadge.setVisibility(member.isCaretaker ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberClick(member);
            }
        });

        holder.btnMoreOptions.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreClick(member, v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return familyMembers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivMemberProfile;
        TextView tvMemberName;
        TextView tvMemberRelation;
        TextView tvPersonalAccess;
        TextView tvMedicalAccess;
        TextView tvEmergencyAccess;
        TextView tvGdprAccess;
        TextView tvCaretakerBadge;
        ImageButton btnMoreOptions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberProfile = itemView.findViewById(R.id.ivMemberProfile);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberRelation = itemView.findViewById(R.id.tvMemberRelation);
            tvPersonalAccess = itemView.findViewById(R.id.tvPersonalAccess);
            tvMedicalAccess = itemView.findViewById(R.id.tvMedicalAccess);
            tvEmergencyAccess = itemView.findViewById(R.id.tvEmergencyAccess);
            tvGdprAccess = itemView.findViewById(R.id.tvGdprAccess);
            tvCaretakerBadge = itemView.findViewById(R.id.tvCaretakerBadge);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}
