package com.example.chroniccare.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "family_members")
public class FamilyMember {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String ownerUserId;
    public String memberUserId;
    public String name;
    public String email;
    public String phone;
    public String relationship;
    public String profileImageUrl;
    public boolean isCaretaker;
    public boolean isAccepted;
    public long joinedDate;
    public boolean canViewPersonalInfo;
    public boolean canViewEmergencyInfo;
    public boolean canViewMedicalInfo;
    public boolean canViewGdprInfo;
}
