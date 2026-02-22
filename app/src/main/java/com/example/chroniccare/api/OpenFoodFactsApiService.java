package com.example.chroniccare.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenFoodFactsApiService {
    @GET("cgi/search.pl")
    Call<OpenFoodSearchResponse> searchFoods(
            @Query("search_terms") String searchTerms,
            @Query("search_simple") int searchSimple,
            @Query("action") String action,
            @Query("json") int json,
            @Query("page_size") int pageSize,
            @Query("page") int page,
            @Query("fields") String fields
    );
}
