# Medication Consistency Tracker - Implementation Guide

## Overview
A GitHub-style consistency tracker has been added to the ProfileActivity that visualizes medication adherence over the last 84 days (12 weeks).

## Features

### Visual Representation
- **Grid Layout**: 12 columns × 7 rows = 84 days
- **Color Coding**:
  - 🟩 **Dark Green (#2E7D32)**: All medications taken (3+ medications)
  - 🟢 **Light Green (#A5D6A7)**: Partial adherence (1-2 medications)
  - ⬜ **Grey (#EEEEEE)**: No medications taken

### Location
- Positioned in ProfileActivity
- Between user profile section and Personal Information card
- Displayed in a Material Card with elevation

## Implementation Details

### Layout Changes
**File**: `activity_profile.xml`

Added a new MaterialCardView containing:
- Title: "Medication Consistency"
- Subtitle: "Last 84 days"
- GridLayout with 12×7 grid (84 squares)
- Legend showing color meanings

### Java Logic
**File**: `ProfileActivity.java`

#### Method: `loadConsistencyGrid()`
1. Fetches all medications from Firestore for current user
2. Groups taken medications by date
3. Generates 84 squares (last 84 days)
4. Colors each square based on medication count:
   - 0 medications = Grey
   - 1-2 medications = Light Green
   - 3+ medications = Dark Green

#### Data Source
- **Collection**: `users/{userId}/medications`
- **Fields Used**:
  - `taken` (Boolean): Whether medication was taken
  - `takenAt` (Timestamp): When it was marked as taken

## How It Works

### Data Flow
```
Firestore → medications collection
    ↓
Filter by taken=true
    ↓
Group by date (yyyy-MM-dd)
    ↓
Count medications per day
    ↓
Apply color based on count
    ↓
Display in grid
```

### Color Logic
```java
if (dayMeds == null || dayMeds.isEmpty()) {
    color = Grey;  // No medications
} else if (dayMeds.size() >= 3) {
    color = Dark Green;  // All medications
} else {
    color = Light Green;  // Partial
}
```

## Usage

### For Users
1. Open Profile screen
2. View consistency tracker below profile picture
3. Each square represents one day
4. Hover/tap to see adherence level

### For Developers
The tracker automatically updates when:
- User marks medication as taken in HomeActivity
- Data syncs from Firestore
- Profile screen is opened/refreshed

## Customization Options

### Adjust Time Period
Change the loop in `loadConsistencyGrid()`:
```java
for (int i = 83; i >= 0; i--) {  // 84 days
    // Change 83 to (days - 1)
}
```

### Adjust Thresholds
Modify the color logic:
```java
if (dayMeds.size() >= 3) {  // Change threshold
    color = 0xFF2E7D32;  // Dark green
}
```

### Change Colors
Update hex values:
```java
color = 0xFF2E7D32;  // Dark green
color = 0xFFA5D6A7;  // Light green
color = 0xFFEEEEEE;  // Grey
```

### Adjust Square Size
Modify the size calculation:
```java
int size = (int) (8 * density);  // Change 8 to desired dp
int margin = (int) (2 * density);  // Change 2 to desired dp
```

## Testing

### Test Scenarios
1. **No Data**: All squares should be grey
2. **Partial Adherence**: Mix of grey and light green
3. **Full Adherence**: All dark green
4. **Recent Activity**: Latest squares reflect recent medication taking

### Sample Test Data
Add test medications in Firestore:
```json
{
  "name": "Test Med",
  "taken": true,
  "takenAt": Timestamp(today),
  "time": "8:00 AM"
}
```

## Performance Considerations

- **Single Query**: Fetches all medications once
- **Client-Side Processing**: Groups and counts on device
- **Lightweight Views**: Uses simple View objects for squares
- **Async Loading**: Firestore query runs asynchronously

## Future Enhancements

### Possible Improvements
1. **Tooltip**: Show date and count on square tap
2. **Animation**: Fade in squares sequentially
3. **Streak Counter**: Display longest streak
4. **Weekly Summary**: Show adherence percentage
5. **Export**: Generate adherence report PDF
6. **Notifications**: Alert on declining adherence

### Advanced Features
- Click square to see medications for that day
- Filter by medication type
- Compare with previous months
- Set adherence goals
- Share progress with doctor

## Troubleshooting

### Squares Not Showing
- Check Firestore connection
- Verify userId is set correctly
- Ensure medications have `taken` and `takenAt` fields

### Wrong Colors
- Verify date format matches: "yyyy-MM-dd"
- Check timezone consistency
- Ensure `takenAt` is Timestamp type

### Layout Issues
- Adjust grid columnCount if squares overflow
- Modify square size for different screen sizes
- Check margins and padding

## Code References

### Key Files
- `app/src/main/res/layout/activity_profile.xml` (lines 88-180)
- `app/src/main/java/com/example/chroniccare/ProfileActivity.java`
  - `initializeViews()` - Calls loadConsistencyGrid()
  - `loadConsistencyGrid()` - Main implementation

### Dependencies
- Firebase Firestore (already included)
- GridLayout (Android framework)
- Material Components (already included)

## Integration with Existing Features

### HomeActivity
When user marks medication as taken:
```java
medicationRef.update("taken", true, "takenAt", FieldValue.serverTimestamp());
```
This automatically updates the consistency tracker.

### AddMedications
New medications added here will be tracked once marked as taken.

## Summary

The consistency tracker provides visual feedback on medication adherence, encouraging users to maintain regular medication schedules. The GitHub-style visualization is familiar and easy to understand, making it an effective motivational tool for chronic disease management.

---

**Implementation Date**: February 28, 2026  
**Status**: ✅ Complete and Working
