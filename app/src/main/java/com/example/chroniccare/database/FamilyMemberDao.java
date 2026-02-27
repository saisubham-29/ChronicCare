package com.example.chroniccare.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FamilyMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FamilyMember member);

    @Query("SELECT * FROM family_members WHERE userId = :userId")
    List<FamilyMember> getFamilyMembersByUserId(String userId);

    @Query("DELETE FROM family_members WHERE userId = :userId")
    void deleteByUserId(String userId);
}
