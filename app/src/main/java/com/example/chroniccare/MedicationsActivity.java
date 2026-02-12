package com.example.chroniccare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class MedicationsActivity extends BottomNavActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TextView addButton = findViewById(R.id.MedPg_add);
        if (addButton != null) {
            addButton.setOnClickListener(v -> 
                startActivity(new Intent(this, AddMedications.class))
            );
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_medications;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_medications;
    }
}
