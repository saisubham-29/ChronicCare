package com.example.chroniccare.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrescriptionScanner {

    private static final String TAG = "PrescriptionScanner";
    private final TextRecognizer textRecognizer;

    public interface ScanCallback {
        void onSuccess(List<MedicationInfo> medications);
        void onError(String errorMessage);
    }

    public static class MedicationInfo {
        public String name;
        public String dosage;
        public String instructions;
        public List<String> times;

        public MedicationInfo(String name, String dosage, String instructions, List<String> times) {
            this.name = name;
            this.dosage = dosage;
            this.instructions = instructions;
            this.times = times;
        }
    }

    public PrescriptionScanner() {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void scanPrescription(Bitmap bitmap, ScanCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        textRecognizer.process(image)
            .addOnSuccessListener(text -> {
                String fullText = text.getText();
                Log.d(TAG, "Recognized text: " + fullText);
                
                List<MedicationInfo> medications = parseMedications(fullText);
                
                if (medications.isEmpty()) {
                    callback.onError("No medications found in the prescription. Please try again with a clearer image.");
                } else {
                    callback.onSuccess(medications);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Text recognition failed", e);
                callback.onError("Failed to read prescription: " + e.getMessage());
            });
    }

    private List<MedicationInfo> parseMedications(String text) {
        List<MedicationInfo> medications = new ArrayList<>();
        
        String[] lines = text.split("\n");
        
        Pattern dosagePattern = Pattern.compile("(\\d+\\s*(?:mg|mcg|g|ml|mL|units?|iu))", Pattern.CASE_INSENSITIVE);
        Pattern timePattern = Pattern.compile("(morning|afternoon|night|evening|noon|bedtime|breakfast|lunch|dinner|8\\s*:\\s*00|2\\s*:\\s*00|9\\s*:\\s*00|once|twice|three times|1-0-1|0-1-0|1-1-1|bd|tds|od|once daily|twice daily)", Pattern.CASE_INSENSITIVE);
        Pattern freqPattern = Pattern.compile("(once|daily|twice|1-0-1|0-1-0|1-1-1|bd|tds|od|every|each)", Pattern.CASE_INSENSITIVE);
        
        String currentMedName = null;
        String currentDosage = null;
        String currentInstructions = null;
        List<String> currentTimes = new ArrayList<>();
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            line = line.replaceAll("[^a-zA-Z0-9\\s\\-\\d]", " ").trim();
            
            Matcher dosageMatch = dosagePattern.matcher(line);
            Matcher timeMatch = timePattern.matcher(line);
            Matcher freqMatch = freqPattern.matcher(line);
            
            if (currentMedName == null) {
                String potentialName = line.replaceAll(dosagePattern.pattern(), "").trim();
                potentialName = potentialName.replaceAll("\\d+", "").trim();
                
                if (potentialName.length() > 2 && !potentialName.matches(".*\\d.*")) {
                    if (!potentialName.toLowerCase().contains("prescription") && 
                        !potentialName.toLowerCase().contains("date") &&
                        !potentialName.toLowerCase().contains("doctor") &&
                        !potentialName.toLowerCase().contains("patient")) {
                        currentMedName = capitalizeFirst(potentialName);
                    }
                } else if (dosageMatch.find()) {
                    String namePart = line.replace(dosageMatch.group(), "").trim();
                    if (namePart.length() > 2) {
                        currentMedName = capitalizeFirst(namePart);
                    }
                }
            }
            
            if (currentDosage == null && dosageMatch.find()) {
                currentDosage = dosageMatch.group();
            }
            
            if (timeMatch.find()) {
                String time = timeMatch.group().toLowerCase();
                if (!currentTimes.contains(time)) {
                    currentTimes.add(time);
                }
            }
            
            if (freqMatch.find() && currentInstructions == null) {
                currentInstructions = freqMatch.group();
            }
            
            if (currentMedName != null && currentDosage != null) {
                if (currentTimes.isEmpty()) {
                    if (currentInstructions != null) {
                        if (currentInstructions.toLowerCase().contains("twice") || 
                            currentInstructions.equals("bd") ||
                            currentInstructions.equals("1-0-1")) {
                            currentTimes.add("morning");
                            currentTimes.add("night");
                        } else if (currentInstructions.toLowerCase().contains("three") || 
                                   currentInstructions.equals("tds") ||
                                   currentInstructions.equals("1-1-1")) {
                            currentTimes.add("morning");
                            currentTimes.add("afternoon");
                            currentTimes.add("night");
                        } else {
                            currentTimes.add("morning");
                        }
                    } else {
                        currentTimes.add("morning");
                    }
                }
                
                medications.add(new MedicationInfo(
                    currentMedName,
                    currentDosage,
                    currentInstructions != null ? currentInstructions : "As directed",
                    new ArrayList<>(currentTimes)
                ));
                
                currentMedName = null;
                currentDosage = null;
                currentInstructions = null;
                currentTimes = new ArrayList<>();
            }
        }
        
        if (currentMedName != null && currentDosage != null) {
            if (currentTimes.isEmpty()) {
                currentTimes.add("morning");
            }
            medications.add(new MedicationInfo(
                currentMedName,
                currentDosage,
                currentInstructions != null ? currentInstructions : "As directed",
                currentTimes
            ));
        }
        
        return medications;
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    public void close() {
        textRecognizer.close();
    }
}
