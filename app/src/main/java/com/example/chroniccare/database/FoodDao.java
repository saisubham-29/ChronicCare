package com.example.chroniccare.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FoodDao {

    @Insert
    void insertFood(FoodLog foodLog);

    @Query("SELECT * FROM food_log ORDER BY date DESC")
    List<FoodLog> getAllFoods();
}