# Testing Alarm

## Steps to test:

1. **Install the app:**
   ```bash
   cd /Users/saisubhamsahu/StudioProjects/ChronicCare
   ./gradlew installDebug
   ```

2. **Check logs in real-time:**
   ```bash
   adb logcat -s AlarmReceiver:D AlarmService:D AddMedications:D
   ```

3. **Set an alarm for 2 minutes from now**

4. **Close the app completely** (swipe away from recents)

5. **Wait for alarm to trigger**

## Check battery optimization:

```bash
adb shell dumpsys deviceidle whitelist +com.example.chroniccare
```

## Manual test trigger:

```bash
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.example.chroniccare
```

## Check if alarm is scheduled:

```bash
adb shell dumpsys alarm | grep chroniccare
```
