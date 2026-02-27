package com.example.chroniccare.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "family_members")
public class FamilyMember {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String userId;
    public String memberName;
    public String relation;
    public String chronicConditions;
    public String profilePicUri;
}
