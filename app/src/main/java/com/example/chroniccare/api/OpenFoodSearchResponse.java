package com.example.chroniccare.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenFoodSearchResponse {
    @SerializedName("count")
    public int count;

    @SerializedName("products")
    public List<OpenFoodProduct> products;
}
