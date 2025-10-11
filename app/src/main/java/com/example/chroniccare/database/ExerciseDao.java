package com.example.chroniccare.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ExerciseDao {

    @Insert
    void insertExercise(ExerciseLog exerciseLog);

    @Query("SELECT * FROM exercise_log ORDER BY date DESC")
    List<ExerciseLog> getAllExercises();
}