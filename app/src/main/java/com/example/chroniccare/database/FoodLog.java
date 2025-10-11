package com.example.chroniccare.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "food_log")
public class FoodLog {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String foodName;
    private int calories;
    private double protein;  // grams
    private double carbs;    // grams
    private double fats;     // grams
    private String mealType; // e.g. Breakfast, Lunch, Dinner, Snack
    private String date;     // or use long timestamp

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getCarbs() { return carbs; }
    public void setCarbs(double carbs) { this.carbs = carbs; }

    public double getFats() { return fats; }
    public void setFats(double fats) { this.fats = fats; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}