package com.example.chroniccare;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chroniccare.adapters.FamilyDashboardAdapter;
import com.example.chroniccare.database.AppDatabase;
import com.example.chroniccare.database.FamilyMember;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FamilyDashboardActivity extends BaseActivity implements FamilyDashboardAdapter.OnMemberActionListener {

    private MaterialToolbar toolbar;
    private RecyclerView rvFamilyMembers;
    private FloatingActionButton fabAddMember;
    private TextView tvNoMembers;
    private FamilyDashboardAdapter adapter;
    private AppDatabase db;
    private String currentUserId;
    private ExecutorService executorService;
    private List<FamilyMember> memberList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_dashboard);

        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        
        SharedPreferences prefs = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadFamilyMembers();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        rvFamilyMembers = findViewById(R.id.rvFamilyMembers);
        fabAddMember = findViewById(R.id.fabAddMember);
        tvNoMembers = findViewById(R.id.tvNoMembers);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvFamilyMembers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FamilyDashboardAdapter(this, memberList);
        adapter.setOnMemberActionListener(this);
        rvFamilyMembers.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAddMember.setOnClickListener(v -> showAddMemberDialog());
    }

    private void loadFamilyMembers() {
        executorService.execute(() -> {
            List<FamilyMember> members = db.familyMemberDao().getFamilyMembersForOwner(currentUserId);
            runOnUiThread(() -> {
                memberList.clear();
                memberList.addAll(members);
                adapter.notifyDataSetChanged();
                tvNoMembers.setVisibility(memberList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showAddMemberDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_invite_family, null);
        EditText etEmail = view.findViewById(R.id.etInviteEmail);
        EditText etName = view.findViewById(R.id.etInviteName);
        AutoCompleteTextView spinnerRelation = view.findViewById(R.id.spinnerRelation);
        MaterialCheckBox cbCaretaker = view.findViewById(R.id.cbIsCaretaker);
        
        String[] relations = {"Spouse", "Parent", "Child", "Sibling", "Friend", "Doctor", "Other"};
        ArrayAdapter<String> relAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, relations);
        spinnerRelation.setAdapter(relAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Add Family Member")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    String relation = spinnerRelation.getText().toString();
                    boolean isCaretaker = cbCaretaker.isChecked();

                    if (email.isEmpty() || name.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addFamilyMember(email, name, relation, isCaretaker);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addFamilyMember(String email, String name, String relation, boolean isCaretaker) {
        executorService.execute(() -> {
            FamilyMember member = new FamilyMember();
            member.ownerUserId = currentUserId;
            member.memberUserId = email;
            member.email = email;
            member.name = name;
            member.relationship = relation;
            member.isCaretaker = isCaretaker;
            member.isAccepted = true;
            member.joinedDate = System.currentTimeMillis();
            member.canViewPersonalInfo = isCaretaker;
            member.canViewEmergencyInfo = isCaretaker;
            member.canViewMedicalInfo = isCaretaker;
            member.canViewGdprInfo = isCaretaker;
            
            db.familyMemberDao().insert(member);
            runOnUiThread(() -> {
                Toast.makeText(this, name + " added successfully", Toast.LENGTH_SHORT).show();
                loadFamilyMembers();
            });
        });
    }

    private void showInviteDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_invite_family, null);
        EditText etEmail = view.findViewById(R.id.etInviteEmail);
        EditText etName = view.findViewById(R.id.etInviteName);
        AutoCompleteTextView spinnerRelation = view.findViewById(R.id.spinnerRelation);
        MaterialCheckBox cbCaretaker = view.findViewById(R.id.cbIsCaretaker);
        MaterialCheckBox cbPersonal = view.findViewById(R.id.cbPersonalInfo);
        MaterialCheckBox cbEmergency = view.findViewById(R.id.cbEmergencyInfo);
        MaterialCheckBox cbMedical = view.findViewById(R.id.cbMedicalInfo);
        MaterialCheckBox cbGdpr = view.findViewById(R.id.cbGdprInfo);
        
        String[] relations = {"Spouse", "Parent", "Child", "Sibling", "Friend", "Doctor", "Other"};
        ArrayAdapter<String> relAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, relations);
        spinnerRelation.setAdapter(relAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Invite Member")
                .setView(view)
                .setPositiveButton("Invite", (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    String name = etName.getText().toString().trim();
                    String relation = spinnerRelation.getText().toString();
                    boolean isCaretaker = cbCaretaker.isChecked();
                    boolean canViewPersonal = cbPersonal.isChecked();
                    boolean canViewEmergency = cbEmergency.isChecked();
                    boolean canViewMedical = cbMedical.isChecked();
                    boolean canViewGdpr = cbGdpr.isChecked();

                    if (email.isEmpty() || name.isEmpty()) {
                        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!canViewPersonal && !canViewEmergency && !canViewMedical && !canViewGdpr && !isCaretaker) {
                        Toast.makeText(this, "Select at least one data type to share", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    inviteMember(email, name, relation, isCaretaker, canViewPersonal, canViewEmergency, canViewMedical, canViewGdpr);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void inviteMember(String email, String name, String relation, boolean isCaretaker, 
                             boolean canViewPersonal, boolean canViewEmergency, 
                             boolean canViewMedical, boolean canViewGdpr) {
        executorService.execute(() -> {
            FamilyMember member = new FamilyMember();
            member.ownerUserId = currentUserId;
            member.memberUserId = email;
            member.email = email;
            member.name = name;
            member.relationship = relation;
            member.isCaretaker = isCaretaker;
            member.joinedDate = System.currentTimeMillis();
            member.isAccepted = true;
            
            member.canViewPersonalInfo = isCaretaker || canViewPersonal;
            member.canViewEmergencyInfo = isCaretaker || canViewEmergency;
            member.canViewMedicalInfo = isCaretaker || canViewMedical;
            member.canViewGdprInfo = isCaretaker || canViewGdpr;
            
            db.familyMemberDao().insert(member);
            runOnUiThread(() -> {
                Toast.makeText(this, "Invitation sent to " + name, Toast.LENGTH_SHORT).show();
                loadFamilyMembers();
                shareInviteLink(email, name, relation);
            });
        });
    }

    private void shareInviteLink(String email, String name, String relation) {
        String inviteText = "Join my health circle on ChronicCare!\n\n" +
                "I'm inviting you to view my health information.\n\n" +
                "To accept, download ChronicCare app and register with: " + email + "\n\n" +
                "This invite was sent by " + name;

        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Health Data Sharing Invite");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, inviteText);
        startActivity(android.content.Intent.createChooser(shareIntent, "Send Invite"));
    }

    @Override
    public void onMemberClick(FamilyMember member) {
        showMemberDetailsDialog(member);
    }

    @Override
    public void onEditClick(FamilyMember member) {
        showEditPermissionsDialog(member);
    }

    @Override
    public void onRemoveClick(FamilyMember member) {
        confirmRemoveMember(member);
    }

    private void showMemberDetailsDialog(FamilyMember member) {
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(member.name).append("\n");
        details.append("Relationship: ").append(member.relationship).append("\n\n");
        details.append("Access Granted:\n");
        if (member.canViewPersonalInfo) details.append("- Personal Information\n");
        if (member.canViewEmergencyInfo) details.append("- Emergency Contact\n");
        if (member.canViewMedicalInfo) details.append("- Medical Information\n");
        if (member.canViewGdprInfo) details.append("- GDPR Data\n");
        
        if (member.isCaretaker) {
            details.append("\nRole: CARETAKER (Full Read Access to all data except medication dosage)");
        }

        new AlertDialog.Builder(this)
                .setTitle("Member Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showEditPermissionsDialog(FamilyMember member) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_invite_family, null);
        EditText etEmail = view.findViewById(R.id.etInviteEmail);
        EditText etName = view.findViewById(R.id.etInviteName);
        AutoCompleteTextView spinnerRelation = view.findViewById(R.id.spinnerRelation);
        MaterialCheckBox cbCaretaker = view.findViewById(R.id.cbIsCaretaker);
        MaterialCheckBox cbPersonal = view.findViewById(R.id.cbPersonalInfo);
        MaterialCheckBox cbEmergency = view.findViewById(R.id.cbEmergencyInfo);
        MaterialCheckBox cbMedical = view.findViewById(R.id.cbMedicalInfo);
        MaterialCheckBox cbGdpr = view.findViewById(R.id.cbGdprInfo);

        etEmail.setText(member.memberUserId);
        etEmail.setEnabled(false);
        etName.setText(member.name);
        
        String[] relations = {"Spouse", "Parent", "Child", "Sibling", "Friend", "Doctor", "Other"};
        ArrayAdapter<String> relAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, relations);
        spinnerRelation.setAdapter(relAdapter);
        spinnerRelation.setText(member.relationship, false);
        
        cbCaretaker.setChecked(member.isCaretaker);
        cbPersonal.setChecked(member.canViewPersonalInfo);
        cbEmergency.setChecked(member.canViewEmergencyInfo);
        cbMedical.setChecked(member.canViewMedicalInfo);
        cbGdpr.setChecked(member.canViewGdprInfo);

        new AlertDialog.Builder(this)
                .setTitle("Edit Permissions")
                .setView(view)
                .setPositiveButton("Update", (dialog, which) -> {
                    member.relationship = spinnerRelation.getText().toString();
                    member.isCaretaker = cbCaretaker.isChecked();
                    
                    if (member.isCaretaker) {
                        member.canViewPersonalInfo = true;
                        member.canViewEmergencyInfo = true;
                        member.canViewMedicalInfo = true;
                        member.canViewGdprInfo = true;
                    } else {
                        member.canViewPersonalInfo = cbPersonal.isChecked();
                        member.canViewEmergencyInfo = cbEmergency.isChecked();
                        member.canViewMedicalInfo = cbMedical.isChecked();
                        member.canViewGdprInfo = cbGdpr.isChecked();
                    }

                    executorService.execute(() -> {
                        db.familyMemberDao().update(member);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Permissions updated", Toast.LENGTH_SHORT).show();
                            loadFamilyMembers();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRemoveMember(FamilyMember member) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove " + member.name + " from your shared data?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    executorService.execute(() -> {
                        db.familyMemberDao().delete(member);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Member removed", Toast.LENGTH_SHORT).show();
                            loadFamilyMembers();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
