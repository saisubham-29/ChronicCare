# Alarm Not Working - Debug Steps

## The alarm IS triggering (logs confirm it), but the screen isn't showing.

### Issue: Android prevents apps from launching activities in background

## Solution Steps:

### 1. **Clear app data and reinstall:**
```bash
adb uninstall com.example.chroniccare
./gradlew installDebug
```

### 2. **Grant ALL permissions manually:**
```bash
# Display over other apps
adb shell appops set com.example.chroniccare SYSTEM_ALERT_WINDOW allow

# Disable battery optimization
adb shell dumpsys deviceidle whitelist +com.example.chroniccare

# Grant notification permission
adb shell pm grant com.example.chroniccare android.permission.POST_NOTIFICATIONS
```

### 3. **Test with phone unlocked first:**
- Set alarm for 1 minute from now
- Keep phone UNLOCKED and screen ON
- Wait for alarm
- If it works → permission issue
- If it doesn't work → alarm scheduling issue

### 4. **Check if alarm is scheduled:**
```bash
adb shell dumpsys alarm | grep chroniccare
```

### 5. **Force trigger alarm (test):**
```bash
# This simulates the alarm trigger
adb shell am start -n com.example.chroniccare/.AlarmActivity \
  --es medicationName "Test Med" \
  --es mealTime "B/F" \
  --es time "10:00 AM"
```

### 6. **Check notification settings on device:**
- Settings → Apps → ChronicCare → Notifications
- Enable ALL notification categories
- Set to "Alerting" not "Silent"
- Enable "Show on lock screen"

### 7. **Verify the notification appears:**
When alarm triggers, you should see:
- ✅ Notification appears
- ✅ Sound plays
- ✅ Phone vibrates
- ❌ Screen might not turn on (Android restriction)

**Tap the notification to open the alarm screen**

## Key Point:
Modern Android (12+) prevents apps from auto-launching activities in background for security. The notification WILL appear with sound - user must tap it to see the full alarm screen.
