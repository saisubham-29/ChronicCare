package com.example.chroniccare.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Include all entity classes here
@Database(
        entities = {ExerciseLog.class, FoodLog.class, User.class, MedicalDocument.class, ChatMessage.class, FamilyMember.class},
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // --- DAOs ---
    public abstract ExerciseDao exerciseDao();
    public abstract FoodDao foodDao();
    public abstract UserDao userDao();
    public abstract MedicalDocumentDao medicalDocumentDao();
    public abstract ChatMessageDao chatMessageDao();
    public abstract FamilyMemberDao familyMemberDao();

    // --- Singleton instance ---
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "chroniccare_db"
                            )
                            .fallbackToDestructiveMigration() // Deletes old data if schema changes
                            .allowMainThreadQueries() // optional (avoid for production)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}