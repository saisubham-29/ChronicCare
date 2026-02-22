package com.example.chroniccare;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chroniccare.api.OpenFoodFactsApiService;
import com.example.chroniccare.api.OpenFoodFactsClient;
import com.example.chroniccare.api.OpenFoodNutriments;
import com.example.chroniccare.api.OpenFoodProduct;
import com.example.chroniccare.api.OpenFoodSearchResponse;
import com.example.chroniccare.database.AppDatabase;
import com.example.chroniccare.database.FoodLog;
import com.example.chroniccare.utils.FirebaseSync;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogFood extends AppCompatActivity {

    private TextInputEditText etFoodSearch;
    private TextInputEditText etFoodName;
    private TextInputEditText etCalories;
    private TextInputEditText etCarbs;
    private TextInputEditText etTime;
    private TextInputEditText etDate;

    private Button btnAddOatmeal;
    private Button btnAddSalmon;
    private Button btnSaveFood;

    private AppDatabase db;
    private FirebaseSync firebaseSync;
    private OpenFoodFactsApiService openFoodApi;
    private String userId;
    private Calendar selectedDateTime;
    private String selectedFoodName;
    private Double selectedFoodProtein;
    private Double selectedFoodFats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_food);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();

        db = AppDatabase.getInstance(this);
        SharedPreferences prefs = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        firebaseSync = new FirebaseSync(userId);
        openFoodApi = OpenFoodFactsClient.getClient().create(OpenFoodFactsApiService.class);

        selectedDateTime = Calendar.getInstance();
        updateDateTimeFields();

        setupListeners();
    }

    private void initViews() {
        etFoodSearch = findViewById(R.id.etFoodSearch);
        etFoodName = findViewById(R.id.etFoodName);
        etCalories = findViewById(R.id.etCalories);
        etCarbs = findViewById(R.id.etCarbs);
        etTime = findViewById(R.id.etTime);
        etDate = findViewById(R.id.etDate);

        btnAddOatmeal = findViewById(R.id.btnAddOatmeal);
        btnAddSalmon = findViewById(R.id.btnAddSalmon);
        btnSaveFood = findViewById(R.id.btnSaveFood);
    }

    private void setupListeners() {
        etTime.setOnClickListener(v -> showTimePicker());
        etDate.setOnClickListener(v -> showDatePicker());
        etFoodSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                triggerFoodSearch();
                return true;
            }
            return false;
        });

        btnAddOatmeal.setOnClickListener(v -> logPresetFood(
                "Oatmeal with Berries",
                320,
                45
        ));

        btnAddSalmon.setOnClickListener(v -> logPresetFood(
                "Grilled Salmon Salad",
                450,
                20
        ));

        btnSaveFood.setOnClickListener(v -> saveManualEntry());
    }

    private void showTimePicker() {
        int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);
        new TimePickerDialog(this, (view, h, m) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, h);
            selectedDateTime.set(Calendar.MINUTE, m);
            updateDateTimeFields();
        }, hour, minute, false).show();
    }

    private void showDatePicker() {
        int year = selectedDateTime.get(Calendar.YEAR);
        int month = selectedDateTime.get(Calendar.MONTH);
        int day = selectedDateTime.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(this, (view, y, m, d) -> {
            selectedDateTime.set(Calendar.YEAR, y);
            selectedDateTime.set(Calendar.MONTH, m);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, d);
            updateDateTimeFields();
        }, year, month, day).show();
    }

    private void updateDateTimeFields() {
        etTime.setText(formatTime(selectedDateTime));
        etDate.setText(formatDate(selectedDateTime));
    }

    private void saveManualEntry() {
        String name = getTrimmedText(etFoodName);
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a food name", Toast.LENGTH_SHORT).show();
            return;
        }

        int calories = parseIntSafe(getTrimmedText(etCalories));
        double carbs = parseDoubleSafe(getTrimmedText(etCarbs));
        double protein = 0;
        double fats = 0;

        if (selectedFoodName != null && selectedFoodName.equalsIgnoreCase(name)) {
            if (selectedFoodProtein != null) {
                protein = selectedFoodProtein;
            }
            if (selectedFoodFats != null) {
                fats = selectedFoodFats;
            }
        }

        logFoodEntry(
                name,
                calories,
                carbs,
                protein,
                fats,
                inferMealType(selectedDateTime),
                (Calendar) selectedDateTime.clone(),
                true
        );
    }

    private void logPresetFood(String name, int calories, double carbs) {
        clearSelectedFood();
        Calendar now = Calendar.getInstance();
        logFoodEntry(
                name,
                calories,
                carbs,
                0,
                0,
                inferMealType(now),
                now,
                false
        );
    }

    private void logFoodEntry(
            String name,
            int calories,
            double carbs,
            double protein,
            double fats,
            String mealType,
            Calendar dateTime,
            boolean clearInputs
    ) {
        FoodLog log = new FoodLog();
        log.setFoodName(name);
        log.setCalories(calories);
        log.setCarbs(carbs);
        log.setProtein(protein);
        log.setFats(fats);
        log.setMealType(mealType);
        log.setDate(formatStorageDate(dateTime));

        Executors.newSingleThreadExecutor().execute(() -> {
            db.foodDao().insertFood(log);
            syncFoodToCloud(name, calories, carbs, protein, fats, mealType, dateTime);

            runOnUiThread(() -> {
                Toast.makeText(this, "Food entry logged", Toast.LENGTH_SHORT).show();
                if (clearInputs) {
                    clearManualFields();
                }
            });
        });
    }

    private void syncFoodToCloud(
            String name,
            int calories,
            double carbs,
            double protein,
            double fats,
            String mealType,
            Calendar dateTime
    ) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("foodName", name);
        data.put("calories", calories);
        data.put("carbs", carbs);
        data.put("protein", protein);
        data.put("fats", fats);
        data.put("mealType", mealType);
        data.put("date", formatStorageDate(dateTime));
        data.put("timestamp", dateTime.getTimeInMillis());
        data.put("userId", userId);

        firebaseSync.syncFood(String.valueOf(dateTime.getTimeInMillis()), data);
    }

    private void clearManualFields() {
        etFoodName.setText("");
        etCalories.setText("");
        etCarbs.setText("");
        clearSelectedFood();
    }

    private void clearSelectedFood() {
        selectedFoodName = null;
        selectedFoodProtein = null;
        selectedFoodFats = null;
    }

    private void triggerFoodSearch() {
        String query = getTrimmedText(etFoodSearch);
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter a food to search", Toast.LENGTH_SHORT).show();
            return;
        }
        searchFoods(query);
    }

    private void searchFoods(String query) {
        if (openFoodApi == null) {
            openFoodApi = OpenFoodFactsClient.getClient().create(OpenFoodFactsApiService.class);
        }

        Call<OpenFoodSearchResponse> call = openFoodApi.searchFoods(
                query,
                1,
                "process",
                1,
                10,
                1,
                "product_name,brands,nutriments"
        );

        call.enqueue(new Callback<OpenFoodSearchResponse>() {
            @Override
            public void onResponse(Call<OpenFoodSearchResponse> call, Response<OpenFoodSearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LogFood.this, "No results found", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<OpenFoodProduct> products = response.body().products;
                if (products == null || products.isEmpty()) {
                    Toast.makeText(LogFood.this, "No results found", Toast.LENGTH_SHORT).show();
                    return;
                }

                showFoodPicker(products);
            }

            @Override
            public void onFailure(Call<OpenFoodSearchResponse> call, Throwable t) {
                Toast.makeText(LogFood.this, "Search failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFoodPicker(List<OpenFoodProduct> products) {
        List<OpenFoodProduct> displayProducts = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (OpenFoodProduct product : products) {
            if (product == null || product.productName == null || product.productName.trim().isEmpty()) {
                continue;
            }
            displayProducts.add(product);
            labels.add(buildProductLabel(product));
        }

        if (displayProducts.isEmpty()) {
            Toast.makeText(this, "No named foods found", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Food")
                .setItems(labels.toArray(new String[0]), (dialog, which) ->
                        applyProductToForm(displayProducts.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String buildProductLabel(OpenFoodProduct product) {
        String name = product.productName != null ? product.productName.trim() : "Food item";
        String brand = product.brands != null ? product.brands.trim() : "";
        Double calories = resolveCalories(product.nutriments);
        String caloriesText = calories != null ? Math.round(calories) + " kcal/100g" : "kcal N/A";

        if (!brand.isEmpty()) {
            return name + " • " + brand + " • " + caloriesText;
        }
        return name + " • " + caloriesText;
    }

    private void applyProductToForm(OpenFoodProduct product) {
        if (product == null) {
            return;
        }

        String name = product.productName != null ? product.productName.trim() : "";
        if (!name.isEmpty()) {
            etFoodName.setText(name);
            selectedFoodName = name;
        }

        OpenFoodNutriments nutriments = product.nutriments;
        Double calories = resolveCalories(nutriments);
        Double carbs = nutriments != null ? nutriments.carbohydrates100g : null;
        Double protein = nutriments != null ? nutriments.proteins100g : null;
        Double fats = nutriments != null ? nutriments.fat100g : null;

        if (calories != null) {
            etCalories.setText(String.valueOf(Math.round(calories)));
        } else {
            etCalories.setText("");
        }

        if (carbs != null) {
            etCarbs.setText(formatDecimal(carbs));
        } else {
            etCarbs.setText("");
        }

        selectedFoodProtein = protein;
        selectedFoodFats = fats;

        Toast.makeText(this, "Loaded per 100g values. Adjust if needed.", Toast.LENGTH_SHORT).show();
    }

    private String inferMealType(Calendar dateTime) {
        int hour = dateTime.get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) {
            return "Breakfast";
        }
        if (hour >= 11 && hour < 16) {
            return "Lunch";
        }
        if (hour >= 16 && hour < 21) {
            return "Dinner";
        }
        return "Snack";
    }

    private String formatTime(Calendar dateTime) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(dateTime.getTime());
    }

    private String formatDate(Calendar dateTime) {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dateTime.getTime());
    }

    private String formatStorageDate(Calendar dateTime) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(dateTime.getTime());
    }

    private Double resolveCalories(OpenFoodNutriments nutriments) {
        if (nutriments == null) {
            return null;
        }
        if (nutriments.energyKcal100g != null) {
            return nutriments.energyKcal100g;
        }
        return nutriments.energyKcal;
    }

    private String formatDecimal(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private String getTrimmedText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
