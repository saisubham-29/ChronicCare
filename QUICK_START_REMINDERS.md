# Quick Start - Medication Reminder System

## 🎯 How to Use

### Adding a Medication with Repeated Doses

1. **Open Add Medications** from Home or Medications tab

2. **Fill Basic Info**
   ```
   Medication Name: Metformin
   Dosage: 500mg
   ```

3. **Select Times**
   - ☑️ Morning (8:00 AM) - B/F
   - ☐ Afternoon (2:00 PM) - A/F  
   - ☑️ Night (9:00 PM) - B/F
   
4. **Customize Times** (Optional)
   - Tap on "8:00 AM" to change morning time
   - Tap on "9:00 PM" to change night time

5. **Choose Meal Timing**
   - Select B/F (Before Food) or A/F (After Food) for each

6. **Tap "Add Medication"**
   - ✅ Saved to Firestore
   - ✅ Alarms scheduled
   - ✅ Will repeat daily

---

## 📱 When Alarm Rings

### Visual Layout
```
┌─────────────────────────────┐
│                             │
│         🔔 Bell Icon        │
│                             │
│         8:00 AM             │
│                             │
│     Metformin 500mg         │
│       Before Food           │
│                             │
│    Swipe to respond         │
│                             │
│  ┌───────────────────────┐  │
│  │ 🕐 5min  [●]  Taken ✓ │  │
│  └───────────────────────┘  │
│                             │
│         Dismiss             │
└─────────────────────────────┘
```

### Actions

**Swipe RIGHT (→)** - Medication Taken
- Marks as taken in Firestore
- Updates home schedule
- Dismisses alarm
- ✅ Done!

**Swipe LEFT (←)** - Snooze 5 Minutes
- Alarm rings again in 5 min
- Follow-up reminder in 10 min
- Can only snooze once
- ⏰ Reminder set

**Tap Dismiss** - Close Without Action
- Alarm dismissed
- Not marked as taken
- Will ring again tomorrow

---

## ⏰ Snooze Flow

### Timeline
```
8:00 AM - Initial Alarm
   ↓ (Swipe Left)
8:05 AM - Snooze Reminder
   ↓ (Swipe Left Again - Not Allowed)
8:10 AM - Follow-up Reminder
   ↓ (Must Respond)
```

### Follow-up Message
```
Metformin 500mg - Follow Up
Did you take your medication?
```

---

## 🎨 UI Features

### Add Medications Page

**Frequency Card**
```
┌─────────────────────────────────────┐
│ Daily Schedule                      │
│                                     │
│ ☑️ Morning    8:00 AM    ⚪B/F ⚫A/F │
│ ☐ Afternoon  2:00 PM    ⚪B/F ⚫A/F │
│ ☑️ Night      9:00 PM    ⚪B/F ⚫A/F │
└─────────────────────────────────────┘
```

### Alarm Screen

**Swipe Indicator**
```
┌─────────────────────────────────────┐
│  🕐 5min  ←  [●]  →  Taken ✓       │
│  Orange      Teal     Green         │
└─────────────────────────────────────┘
```

---

## 🧪 Quick Test

### Test in 2 Minutes
1. Add medication
2. Set morning time to **current time + 2 minutes**
3. Check morning checkbox
4. Save
5. Wait 2 minutes
6. Alarm will ring!

### Test Swipe Actions
1. When alarm appears, try swiping right
2. Check home screen - medication marked as taken
3. Add another medication for +2 minutes
4. When alarm rings, swipe left
5. Wait 5 minutes - alarm rings again
6. Swipe right to mark as taken

---

## 📋 Checklist

Before using in production:

- [ ] Test alarm at actual medication time
- [ ] Verify alarm wakes screen when locked
- [ ] Check notification appears
- [ ] Test swipe right (taken)
- [ ] Test swipe left (snooze)
- [ ] Verify follow-up reminder
- [ ] Check Firestore updates
- [ ] Test multiple medications
- [ ] Verify daily repeat works
- [ ] Test on different Android versions

---

## 🔧 Troubleshooting

### Alarm Not Ringing?
1. Check app has notification permission
2. Verify "Alarms & reminders" permission granted
3. Ensure battery optimization disabled for app
4. Check Do Not Disturb settings

### Swipe Not Working?
1. Swipe from the circular button
2. Swipe at least 30% of screen width
3. Don't swipe too fast or too slow
4. Try swiping horizontally (not diagonally)

### Not Marked as Taken?
1. Ensure internet connection
2. Check user is logged in
3. Verify Firestore rules allow writes
4. Check logcat for errors

---

## 💡 Tips

1. **Set realistic times** - Don't set too many reminders
2. **Use meal timing** - Helps remember when to take
3. **Test first** - Set alarm for 2 min away to test
4. **Check battery** - Disable optimization for app
5. **Enable notifications** - Don't miss reminders

---

## 📞 Support

If you encounter issues:
1. Check logcat: `adb logcat | grep ChronicCare`
2. Verify permissions in Settings
3. Test with simple case (one medication, one time)
4. Check Firestore console for data

---

**System Ready!** ✅  
Start adding your medications and never miss a dose! 💊
