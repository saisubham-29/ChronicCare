package com.example.chroniccare.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FamilyMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FamilyMember member);

    @Update
    void update(FamilyMember member);

    @Delete
    void delete(FamilyMember member);

    @Query("SELECT * FROM family_members WHERE ownerUserId = :ownerUserId")
    List<FamilyMember> getFamilyMembersByOwner(String ownerUserId);

    @Query("SELECT * FROM family_members WHERE ownerUserId = :ownerUserId")
    List<FamilyMember> getFamilyMembersForOwner(String ownerUserId);

    @Query("SELECT * FROM family_members WHERE memberUserId = :memberUserId AND ownerUserId = :ownerUserId LIMIT 1")
    FamilyMember getFamilyMember(String memberUserId, String ownerUserId);

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    FamilyMember getFamilyMemberById(int id);

    @Query("SELECT * FROM family_members WHERE memberUserId = :userId AND isAccepted = 1 LIMIT 1")
    FamilyMember getAcceptedMemberByUserId(String userId);

    @Query("DELETE FROM family_members WHERE id = :id")
    void deleteById(int id);
}
