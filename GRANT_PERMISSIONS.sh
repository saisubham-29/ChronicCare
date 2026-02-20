#!/bin/bash

echo "Granting all required permissions for ChronicCare alarms..."

# Display over other apps
adb shell appops set com.example.chroniccare SYSTEM_ALERT_WINDOW allow
echo "✓ Display over other apps"

# Disable battery optimization
adb shell dumpsys deviceidle whitelist +com.example.chroniccare
echo "✓ Battery optimization disabled"

# Notification permission
adb shell pm grant com.example.chroniccare android.permission.POST_NOTIFICATIONS
echo "✓ Notification permission"

# Schedule exact alarms
adb shell appops set com.example.chroniccare SCHEDULE_EXACT_ALARM allow
echo "✓ Schedule exact alarms"

echo ""
echo "All permissions granted! Now test the alarm."
