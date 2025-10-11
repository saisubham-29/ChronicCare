package com.example.chroniccare.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "exercise_log")
public class ExerciseLog {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String exerciseName;
    private int durationMinutes;
    private int caloriesBurned;
    private String date; // or use long timestamp

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(int caloriesBurned) { this.caloriesBurned = caloriesBurned; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}