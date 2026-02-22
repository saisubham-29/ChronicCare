package com.example.chroniccare.api;

import com.google.gson.annotations.SerializedName;

public class OpenFoodNutriments {
    @SerializedName("energy-kcal_100g")
    public Double energyKcal100g;

    @SerializedName("energy-kcal")
    public Double energyKcal;

    @SerializedName("carbohydrates_100g")
    public Double carbohydrates100g;

    @SerializedName("proteins_100g")
    public Double proteins100g;

    @SerializedName("fat_100g")
    public Double fat100g;
}
