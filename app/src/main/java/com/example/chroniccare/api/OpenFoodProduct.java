package com.example.chroniccare.api;

import com.google.gson.annotations.SerializedName;

public class OpenFoodProduct {
    @SerializedName("product_name")
    public String productName;

    @SerializedName("brands")
    public String brands;

    @SerializedName("nutriments")
    public OpenFoodNutriments nutriments;
}
