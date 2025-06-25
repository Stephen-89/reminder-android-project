package com.steph.reminder;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Application;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.steph.reminder.adapter.ReminderDataAdapter;
import com.steph.reminder.data.db.Reminder;
import com.steph.reminder.data.model.ReminderViewModel;
import com.steph.reminder.receiver.ReminderReceiver;
import com.steph.reminder.util.DateUtil;
import com.steph.reminder.util.PermissionUtil;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private List<Reminder> reminderDataList = new ArrayList<>();
    private List<Reminder> currentReminderDataList = new ArrayList<>();

    private RecyclerView reminderRecyclerView;
    private ReminderDataAdapter reminderDataAdapter;
    private ReminderViewModel reminderViewModel;

    private TextView todayCountTextView, allCountTextView, previousCountTextView;
    private CardView cardToday, cardAll, cardPrevious;

    private int todayCount = 0;
    private int allCount = 0;
    private int previousCount = 0;

    private Typeface cartoonFont;

    private com.google.android.ump.ConsentForm consentForm;
    private ConsentInformation consentInformation;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadFonts();


        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build();

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings)
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> {
                    if (consentInformation.isConsentFormAvailable()) {
                        loadConsentForm();
                    } else {
                        continueAppInitialization();
                    }
                },
                formError -> {
                    Log.w("Consent", "Consent info update failed: " + formError.getMessage());
                    continueAppInitialization();
                }
        );

    }

    private void loadConsentForm() {
        UserMessagingPlatform.loadConsentForm(
            this,
            form -> {
                consentForm = form;
                if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                    consentForm.show(
                        this,
                        formError -> {
                            if (formError != null) {
                                Log.w("Consent", "Form display error: " + formError.getMessage());
                            }
                            if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED ||
                                    consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.NOT_REQUIRED) {
                                continueAppInitialization();
                            }
                        }
                    );
                } else {
                    continueAppInitialization();
                }
            },
            formError -> {
                Log.w("Consent", "Failed to load consent form: " + formError.getMessage());
                continueAppInitialization();
            }
        );
    }

    private void continueAppInitialization(){

        PermissionUtil.requestExactAlarmPermission(this);
        PermissionUtil.requestPostNotificationPermission(this);

        todayCountTextView = findViewById(R.id.todayCountTextView);
        allCountTextView = findViewById(R.id.allCountTextView);
        previousCountTextView = findViewById(R.id.previousCountTextView);
        todayCountTextView.setText(String.format("Today (%d)", 0));
        allCountTextView.setText(String.format("All (%d)", 0));
        previousCountTextView.setText(String.format("Previous Reminders (%d)", 0));

        setupView();
        setupFilterMenu();
        setupSearch();
        setupCardClick();

        setupAddReminderButton();
        setupEditReminderButton();

        setupDelete();

        FloatingActionButton btnCalendar = findViewById(R.id.btnCalandar);
        btnCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            intent.putExtra("reminders", (Serializable) reminderDataList);
            startActivity(intent);
        });

        setupAds();

    }

    private void setupAds() {
        adView = findViewById(R.id.adView);
        MobileAds.initialize(this, initializationStatus -> {
            AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
            if (consentInformation != null) {
                int consentStatus = consentInformation.getConsentStatus();
                if (consentStatus == ConsentInformation.ConsentStatus.REQUIRED || consentStatus == ConsentInformation.ConsentStatus.UNKNOWN) {
                    Bundle extras = new Bundle();
                    extras.putString("npa", "1");
                    adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
                }
            }
            AdRequest adRequest = adRequestBuilder.build();
            adView.loadAd(adRequest);
        });
    }

    private void loadFonts() {
        cartoonFont = Typeface.createFromAsset(getAssets(), "fonts/cartoon.otf");
    }

    private void setupCardClick(){

        cardToday = findViewById(R.id.cardToday);
        cardToday.setOnClickListener(v -> {
            filterRemindersBy("today");
        });

        cardAll = findViewById(R.id.cardAll);
        cardAll.setOnClickListener(v -> {
            filterRemindersBy("all");
        });

        cardPrevious = findViewById(R.id.cardPrevious);
        cardPrevious.setOnClickListener(v -> {
            filterByPrevious();
        });

    }

    private void sortRemindersBy(String criteria) {
        switch (criteria) {
            case "date_asc":
                Collections.sort(reminderDataList, (r1, r2) -> Long.compare(r1.getToTime(), r2.getToTime()));
                break;
            case "date_desc":
                Collections.sort(reminderDataList, (r1, r2) -> Long.compare(r2.getToTime(), r1.getToTime()));
                break;
            case "priority_asc":
                Collections.sort(reminderDataList, (r1, r2) -> Integer.compare(getPriorityLevel(r1.getPriority()), getPriorityLevel(r2.getPriority())));
                break;
            case "priority_desc":
                Collections.sort(reminderDataList, (r1, r2) -> Integer.compare(getPriorityLevel(r2.getPriority()), getPriorityLevel(r1.getPriority())));
                break;
        }
        reminderDataAdapter.notifyDataSetChanged();
    }

    private int getPriorityLevel(String priority) {
        switch (priority != null ? priority.toLowerCase() : "") {
            case "high":
                return 3;
            case "medium":
                return 2;
            case "low":
                return 1;
            case "no priority":
            default:
                return 0;
        }
    }

    private void filterRemindersBy(String timeframe) {
        reminderViewModel.getAllReminders().observe(this, reminders -> {
            if (reminders != null && !reminders.isEmpty()) {
                Log.d("MainActivity", "Reminders exist. Count: " + reminders.size());
                List<Reminder> filteredList = new ArrayList<>();
                Calendar now = Calendar.getInstance();
                for (Reminder reminder : reminders) {
                    Calendar reminderTime = Calendar.getInstance();
                    reminderTime.setTimeInMillis(reminder.getToTime());
                    switch (timeframe) {
                        case "today":
                            if (DateUtil.isSameDay(reminderTime, now)) {
                                filteredList.add(reminder);
                            }
                            break;
                        case "week":
                            if (DateUtil.isSameWeek(reminderTime, now)) {
                                filteredList.add(reminder);
                            }
                            break;
                        case "month":
                            if (reminderTime.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                                    && reminderTime.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                                filteredList.add(reminder);
                            }
                            break;
                        case "all":
                            filteredList.add(reminder);
                            break;
                    }
                }
                updateReminders(filteredList);
            } else {
                Log.d("MainActivity", "No reminders found.");
            }
        });
    }

    private void filterByPriority(String priority) {
        reminderViewModel.getAllReminders().observe(this, reminders -> {
            if (reminders != null && !reminders.isEmpty()) {
                Log.d("MainActivity", "Reminders exist. Count: " + reminders.size());
                List<Reminder> filteredList = new ArrayList<>();
                for (Reminder reminder : reminders) {
                    if (priority.equalsIgnoreCase("no priority")) {
                        if (reminder.getPriority() == null || reminder.getPriority().trim().isEmpty()) {
                            filteredList.add(reminder);
                        }
                    } else if (priority.equalsIgnoreCase(reminder.getPriority())) {
                        filteredList.add(reminder);
                    }
                }
                updateReminders(filteredList);
            } else {
                Log.d("MainActivity", "No reminders found.");
            }
        });
    }

    private void filterByPrevious() {

        reminderViewModel.getAllReminders().observe(this, reminders -> {

            if (reminders != null && !reminders.isEmpty()) {

                Log.d("MainActivity", "Reminders exist. Count: " + reminders.size());

                List<Reminder> filteredList = new ArrayList<>();

                long startOfToday = DateUtil.getStartOfDay();
                reminderDataList.clear();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    reminders.forEach(reminder -> {
                        long toTime = reminder.getToTime();
                        if (toTime < startOfToday) {
                            filteredList.add(reminder);
                        }
                    });
                } else {
                    for (Reminder reminder : reminders) {
                        long toTime = reminder.getToTime();
                        if (toTime < startOfToday) {
                            filteredList.add(reminder);
                        }
                    }
                }

                reminderDataList.addAll(filteredList);
                reminderDataAdapter.notifyDataSetChanged();

            } else {
                Log.d("MainActivity", "No reminders found.");
            }

        });

    }

    private void setupView() {

        reminderRecyclerView = findViewById(R.id.recyclerView);

        reminderDataAdapter = new ReminderDataAdapter(reminderDataList, reminder -> {
            showEditReminderDialog(reminder);
        });

        reminderRecyclerView.setAdapter(reminderDataAdapter);
        reminderRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);
        reminderViewModel.getAllReminders().observe(this, reminders -> {
            if (reminders != null && !reminders.isEmpty()) {
                Log.d("MainActivity", "Reminders exist. Count: " + reminders.size());
                getReminders(reminders);
            } else {
                Log.d("MainActivity", "No reminders found.");
            }
        });

    }

    private void setupFilterMenu() {

        ImageButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.filter_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {

                if (item.getItemId() == R.id.filter_date_asc) {
                    sortRemindersBy("date_asc");
                    return true;
                } else if (item.getItemId() == R.id.filter_date_desc) {
                    sortRemindersBy("date_desc");
                    return true;
                } else if (item.getItemId() == R.id.filter_priority_asc) {
                    sortRemindersBy("priority_asc");
                    return true;
                } else if (item.getItemId() == R.id.filter_priority_desc) {
                    sortRemindersBy("priority_desc");
                    return true;
                } else if (item.getItemId() == R.id.filter_today) {
                    filterRemindersBy("today");
                    return true;
                } else if (item.getItemId() == R.id.filter_week) {
                    filterRemindersBy("week");
                    return true;
                } else if (item.getItemId() == R.id.filter_month) {
                    filterRemindersBy("month");
                    return true;
                } else if (item.getItemId() == R.id.filter_all) {
                    filterRemindersBy("all");
                    return true;
                } else if (item.getItemId() == R.id.filter_priority_high) {
                    filterByPriority("high");
                    return true;
                } else if (item.getItemId() == R.id.filter_priority_medium) {
                    filterByPriority("medium");
                    return true;
                } else if (item.getItemId() == R.id.filter_priority_low) {
                    filterByPriority("low");
                    return true;
                } else if (item.getItemId() == R.id.filter_priority_none) {
                    filterByPriority("no priority");
                    return true;
                } else if (item.getItemId() == R.id.filter_previous) {
                    filterByPrevious();
                    return true;
                } else {
                    return false;
                }

            });
            popupMenu.show();
        });

    }

    private void getReminders(List<Reminder> reminders) {

        todayCount = 0;
        allCount = 0;
        previousCount = 0;

        long startOfToday = DateUtil.getStartOfDay();
        long endOfToday = DateUtil.getEndOfDay();

        reminderDataList.clear();
        currentReminderDataList.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            reminders.forEach(reminder -> {
                long fromTime = reminder.getFromTime();
                long toTime = reminder.getToTime();
                if (fromTime >= startOfToday && toTime <= endOfToday) {
                    todayCount += 1;
                }
                if (toTime >= startOfToday) {
                    allCount += 1;
                    currentReminderDataList.add(reminder);
                } else {
                    previousCount += 1;
                }
            });
        } else {
            for (Reminder reminder : reminders) {
                long toTime = reminder.getToTime();
                if (toTime >= startOfToday && toTime <= endOfToday) {
                    todayCount += 1;
                }
                if (toTime >= startOfToday) {
                    allCount += 1;
                    currentReminderDataList.add(reminder);
                } else {
                    previousCount += 1;
                }
            }
        }

        reminderDataList.addAll(currentReminderDataList);

        todayCountTextView.setText(String.format("Today (%d)", todayCount));
        allCountTextView.setText(String.format("All (%d)", allCount));
        previousCountTextView.setText(String.format("Previous Reminders (%d)", previousCount));


        reminderDataAdapter.notifyDataSetChanged();

    }

    private void updateReminders(List<Reminder> reminders) {

        todayCount = 0;
        allCount = 0;

        long startOfToday = DateUtil.getStartOfDay();
        long endOfToday = DateUtil.getEndOfDay();

        reminderDataList.clear();
        currentReminderDataList.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            reminders.forEach(reminder -> {
                long fromTime = reminder.getFromTime();
                long toTime = reminder.getToTime();
                if (fromTime >= startOfToday && toTime <= endOfToday) {
                    todayCount += 1;
                }
                if (toTime >= startOfToday) {
                    allCount += 1;
                    currentReminderDataList.add(reminder);
                }
            });
        } else {
            for (Reminder reminder : reminders) {
                long toTime = reminder.getToTime();
                if (toTime >= startOfToday && toTime <= endOfToday) {
                    todayCount += 1;
                }
                if (toTime >= startOfToday) {
                    allCount += 1;
                    currentReminderDataList.add(reminder);
                }
            }
        }

        reminderDataList.addAll(currentReminderDataList);

        todayCountTextView.setText(String.format("Today (%d)", todayCount));
        allCountTextView.setText(String.format("All (%d)", allCount));

        reminderDataAdapter.notifyDataSetChanged();

    }

    private void setupSearch(){
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                searchReminders(newText);
                return true;
            }
        });
    }

    private void searchReminders(String text) {
        reminderViewModel.getAllReminders().observe(this, reminders -> {
            if (reminders != null && !reminders.isEmpty()) {
                Log.d("MainActivity", "Reminders exist. Count: " + reminders.size());
                List<Reminder> filteredList = new ArrayList<>();
                for (Reminder reminder : reminders) {
                    if (reminder.getTitle().toLowerCase().contains(text.toLowerCase()) ||
                            (reminder.getDescription() != null && reminder.getDescription().toLowerCase().contains(text.toLowerCase()))) {
                        filteredList.add(reminder);
                    }
                }
                updateReminders(filteredList);
            } else {
                Log.d("MainActivity", "No reminders found.");
            }
        });
    }

    private void setupAddReminderButton() {

        FloatingActionButton fabReminderBtn = findViewById(R.id.btnAddReminder);

        fabReminderBtn.setOnClickListener(v -> {

            View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_reminder_add, null);

            TextView headerText = dialogView.findViewById(R.id.headerText);
            View colorPreview = dialogView.findViewById(R.id.colorPreview);

            ImageView paletteImage = dialogView.findViewById(R.id.paletteImage);
            ImageView listTypeImage = dialogView.findViewById(R.id.listTypeImage);
            ImageView repeatImage = dialogView.findViewById(R.id.repeatImage);
            ImageView priorityImage = dialogView.findViewById(R.id.priorityImage);
            ImageView notificationImage = dialogView.findViewById(R.id.notificationImage);

            int[] selectedColor = {ContextCompat.getColor(this, R.color.colorOption9)};

            colorPreview.setBackgroundColor(selectedColor[0]);
            headerText.setBackgroundColor(selectedColor[0]);
            paletteImage.setColorFilter(selectedColor[0]);
            listTypeImage.setColorFilter(selectedColor[0]);
            repeatImage.setColorFilter(selectedColor[0]);
            priorityImage.setColorFilter(selectedColor[0]);
            notificationImage.setColorFilter(selectedColor[0]);

            colorPreview.setOnClickListener(viewColor -> showMaterialColorPicker(this, colorPreview, color -> {
                selectedColor[0] = color;
                colorPreview.setBackgroundColor(selectedColor[0]);
                headerText.setBackgroundColor(selectedColor[0]);
                paletteImage.setColorFilter(selectedColor[0]);
                listTypeImage.setColorFilter(selectedColor[0]);
                repeatImage.setColorFilter(selectedColor[0]);
                priorityImage.setColorFilter(selectedColor[0]);
                notificationImage.setColorFilter(selectedColor[0]);

            }));

            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setView(dialogView)
                    .setPositiveButton("Save", null)
                    .setNegativeButton("Cancel", null)
                    .show();

            Button saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setEnabled(false);

            EditText edtTitle = dialogView.findViewById(R.id.edtTitle);
            edtTitle.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    saveButton.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            EditText edtDescription = dialogView.findViewById(R.id.edtDescription);

            Spinner spinnerType = dialogView.findViewById(R.id.spinnerType);
            setupSpinner(spinnerType, R.array.reminder_types);

            Spinner spinnerRepeat = dialogView.findViewById(R.id.spinnerRepeat);
            setupSpinner(spinnerRepeat, R.array.repeat_types);

            Spinner spinnerPriority = dialogView.findViewById(R.id.spinnerPriority);
            setupSpinner(spinnerPriority, R.array.priority_types);

            TextView datePickerFrom = dialogView.findViewById(R.id.datePicker);
            TextView datePickerTo = dialogView.findViewById(R.id.datePickerTo);
            TextView timePickerFrom = dialogView.findViewById(R.id.timePicker);
            TextView timePickerTo = dialogView.findViewById(R.id.timePickerTo);

            DateUtil.setDefaultDateTime(datePickerFrom, datePickerTo, timePickerFrom, timePickerTo);

            datePickerFrom.setOnClickListener(v1 -> showDatePickerDialog(datePickerFrom, datePickerTo));
            datePickerTo.setOnClickListener(v1 -> showDatePickerDialog(datePickerTo, datePickerFrom));

            timePickerFrom.setOnClickListener(v2 -> showTimePickerDialog(timePickerFrom));
            timePickerTo.setOnClickListener(v2 -> showTimePickerDialog(timePickerTo));

            Spinner spinnerTime = dialogView.findViewById(R.id.reminderTime);
            setupSpinner(spinnerTime, R.array.reminder_time);

            Spinner spinnerTimeDaily = dialogView.findViewById(R.id.reminderTimeDaily);
            setupSpinner(spinnerTimeDaily, R.array.reminder_time_daily);

            Switch switchBoxAllDay = dialogView.findViewById(R.id.switchBoxAllDay);
            setupAllDayToggle(switchBoxAllDay, timePickerFrom, timePickerTo, spinnerTime, spinnerTimeDaily);

            saveReminder(alertDialog, saveButton, selectedColor, edtTitle, edtDescription, spinnerType, spinnerRepeat, spinnerPriority, switchBoxAllDay, datePickerFrom, timePickerFrom, datePickerTo, timePickerTo, switchBoxAllDay.isChecked() ? spinnerTimeDaily : spinnerTime);

        });

    }

    private void setupEditReminderButton() {
        reminderDataAdapter = new ReminderDataAdapter(reminderDataList, reminder -> {
            showEditReminderDialog(reminder);
        });
        reminderRecyclerView.setAdapter(reminderDataAdapter);
    }

    private void showEditReminderDialog(Reminder reminder) {

        Toast.makeText(MainActivity.this, "Edit: " + reminder.getTitle(), Toast.LENGTH_SHORT).show();

        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_reminder_edit, null);

        int[] selectedColor = {reminder.getColor()};

        TextView headerText = dialogView.findViewById(R.id.headerText);
        View colorPreview = dialogView.findViewById(R.id.colorPreview);

        ImageView paletteImage = dialogView.findViewById(R.id.paletteImage);
        ImageView listTypeImage = dialogView.findViewById(R.id.listTypeImage);
        ImageView repeatImage = dialogView.findViewById(R.id.repeatImage);
        ImageView priorityImage = dialogView.findViewById(R.id.priorityImage);
        ImageView notificationImage = dialogView.findViewById(R.id.notificationImage);

        colorPreview.setBackgroundColor(selectedColor[0]);
        headerText.setBackgroundColor(selectedColor[0]);
        paletteImage.setColorFilter(selectedColor[0]);
        listTypeImage.setColorFilter(selectedColor[0]);
        repeatImage.setColorFilter(selectedColor[0]);
        priorityImage.setColorFilter(selectedColor[0]);
        notificationImage.setColorFilter(selectedColor[0]);

        colorPreview.setOnClickListener(viewColor -> {
            showMaterialColorPicker(this, colorPreview, color -> {
                selectedColor[0] = color;
                colorPreview.setBackgroundColor(selectedColor[0]);
                headerText.setBackgroundColor(selectedColor[0]);
                paletteImage.setColorFilter(selectedColor[0]);
                listTypeImage.setColorFilter(selectedColor[0]);
                repeatImage.setColorFilter(selectedColor[0]);
                priorityImage.setColorFilter(selectedColor[0]);
                notificationImage.setColorFilter(selectedColor[0]);
            });
        });

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                .setView(dialogView)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .show();

        Button editButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        editButton.setEnabled(true);

        EditText edtTitle = dialogView.findViewById(R.id.edtTitle);
        EditText edtDescription = dialogView.findViewById(R.id.edtDescription);
        Spinner spinnerType = dialogView.findViewById(R.id.spinnerType);
        Spinner spinnerRepeat = dialogView.findViewById(R.id.spinnerRepeat);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinnerPriority);
        Spinner spinnerTime = dialogView.findViewById(R.id.reminderTime);
        Spinner spinnerTimeDaily = dialogView.findViewById(R.id.reminderTimeDaily);

        setupSpinner(spinnerType, R.array.reminder_types);
        setupSpinner(spinnerRepeat, R.array.repeat_types);
        setupSpinner(spinnerPriority, R.array.priority_types);
        setupSpinner(spinnerTime, R.array.reminder_time);
        setupSpinner(spinnerTimeDaily, R.array.reminder_time_daily);

        TextView datePickerFrom = dialogView.findViewById(R.id.datePicker);
        TextView datePickerTo = dialogView.findViewById(R.id.datePickerTo);
        TextView timePickerFrom = dialogView.findViewById(R.id.timePicker);
        TextView timePickerTo = dialogView.findViewById(R.id.timePickerTo);
        Switch switchBoxAllDay = dialogView.findViewById(R.id.switchBoxAllDay);

        edtTitle.setText(reminder.getTitle());
        edtDescription.setText(reminder.getDescription());

        setSpinnerSelection(spinnerType, reminder.getType());
        setSpinnerSelection(spinnerRepeat, reminder.getRepeat());
        setSpinnerSelection(spinnerPriority, reminder.getPriority());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm", Locale.getDefault());

        datePickerFrom.setText(dateFormat.format(new Date(reminder.getFromTime())));
        timePickerFrom.setText(timeFormat.format(new Date(reminder.getFromTime())));

        datePickerTo.setText(dateFormat.format(new Date(reminder.getToTime())));
        timePickerTo.setText(timeFormat.format(new Date(reminder.getToTime())));

        setSpinnerSelection(spinnerTime, reminder.getTimeForNotification());
        setSpinnerSelection(spinnerTimeDaily, reminder.getTimeForNotification());

        switchBoxAllDay.setChecked(reminder.isAllDay());
        setupAllDayToggle(switchBoxAllDay, timePickerFrom, timePickerTo, spinnerTime, spinnerTimeDaily);

        edtTitle.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editButton.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
            public void afterTextChanged(Editable s) {}
        });

        datePickerFrom.setOnClickListener(v -> showDatePickerDialog(datePickerFrom, datePickerTo));
        datePickerTo.setOnClickListener(v -> showDatePickerDialog(datePickerTo, datePickerFrom));
        timePickerFrom.setOnClickListener(v -> showTimePickerDialog(timePickerFrom));
        timePickerTo.setOnClickListener(v -> showTimePickerDialog(timePickerTo));

        editReminder(alertDialog, editButton, reminder, selectedColor, edtTitle, edtDescription, spinnerType,
                spinnerRepeat, spinnerPriority, switchBoxAllDay, datePickerFrom, timePickerFrom, datePickerTo, timePickerTo,
                switchBoxAllDay.isChecked() ? spinnerTimeDaily : spinnerTime);

    }

    private void saveReminder(AlertDialog alertDialog, Button saveButton, int[] selectedColor, EditText edtTitle, EditText edtDescription, Spinner spinnerType, Spinner spinnerRepeat, Spinner spinnerPriority, Switch switchBoxAllDay, TextView datePickerFrom, TextView timePickerFrom, TextView datePickerTo, TextView timePickerTo, Spinner spinnerTime) {

        saveButton.setOnClickListener(v -> {

            String timeForNotification = spinnerTime.getSelectedItem().toString();

            String title = edtTitle.getText().toString();
            String description = edtDescription.getText().toString();
            String type = spinnerType.getSelectedItem().toString();
            String repeat = spinnerRepeat.getSelectedItem().toString();
            String priority = spinnerPriority.getSelectedItem().toString();

            long fromTime = DateUtil.getReminderTime(switchBoxAllDay, datePickerFrom.getText().toString(), timePickerFrom.getText().toString());
            long toTime = DateUtil.getReminderTime(switchBoxAllDay, datePickerTo.getText().toString(), timePickerTo.getText().toString());

            if (fromTime > toTime) {
                Toast.makeText(MainActivity.this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                return;
            }

            Reminder reminder = new Reminder(selectedColor[0], title, description, type, repeat, priority, fromTime, toTime, switchBoxAllDay.isChecked(), timeForNotification);

            ReminderViewModel reminderViewModel = new ViewModelProvider.AndroidViewModelFactory(
                    (Application) this.getApplicationContext()
            ).create(ReminderViewModel.class);
            reminderViewModel.insert(reminder).observe(this, id -> {
                if (id != null) {
                    Log.d("Reminder", "Inserted reminder with ID: " + id);
                    reminder.setId(id);
                    scheduleReminderNotification(reminder, timeForNotification);
                    reminderDataAdapter.notifyDataSetChanged();
                    alertDialog.dismiss();
                }
            });

        });

    }

    private void editReminder(AlertDialog alertDialog, Button editButton, Reminder originalReminder,
                              int[] selectedColor, EditText edtTitle, EditText edtDescription, Spinner spinnerType,
                              Spinner spinnerRepeat, Spinner spinnerPriority, Switch switchBoxAllDay, TextView datePickerFrom,
                              TextView timePickerFrom, TextView datePickerTo, TextView timePickerTo, Spinner spinnerTime) {

        editButton.setOnClickListener(v -> {

            String title = edtTitle.getText().toString();
            String description = edtDescription.getText().toString();
            String type = spinnerType.getSelectedItem().toString();
            String repeat = spinnerRepeat.getSelectedItem().toString();
            String priority = spinnerPriority.getSelectedItem().toString();
            String timeForNotification = spinnerTime.getSelectedItem().toString();

            long fromTime = DateUtil.getReminderTime(switchBoxAllDay, datePickerFrom.getText().toString(), timePickerFrom.getText().toString());
            long toTime = DateUtil.getReminderTime(switchBoxAllDay, datePickerTo.getText().toString(), timePickerTo.getText().toString());

            if (fromTime > toTime) {
                Toast.makeText(MainActivity.this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                return;
            }

            Reminder updatedReminder = new Reminder(originalReminder.getId(),selectedColor[0],title,description,type,repeat,priority,fromTime,toTime,switchBoxAllDay.isChecked(),timeForNotification);

            ReminderViewModel reminderViewModel = new ViewModelProvider.AndroidViewModelFactory(
                    (Application) getApplicationContext()
            ).create(ReminderViewModel.class);

            reminderViewModel.update(updatedReminder).observe(this, rowsAffected -> {
                if (rowsAffected != null && rowsAffected > 0) {
                    Toast.makeText(MainActivity.this, "Reminder updated", Toast.LENGTH_SHORT).show();
                    cancelReminderNotification(updatedReminder);
                    scheduleReminderNotification(updatedReminder, timeForNotification);
                    reminderDataAdapter.notifyDataSetChanged();
                    alertDialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to update reminder", Toast.LENGTH_SHORT).show();
                }
            });

        });

    }

    private void setupSpinner(Spinner spinner, int arrayResId) {
        String[] items = getResources().getStringArray(arrayResId);
        List<String> itemList = Arrays.asList(items);
        ArrayAdapter<String> adapter = new CustomFontAdapter(this,
                android.R.layout.simple_spinner_item, itemList, cartoonFont);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void showDatePickerDialog(TextView targetPicker, TextView otherPicker) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                    selectedDay, selectedMonth + 1, selectedYear);
            targetPicker.setText(selectedDate);
        }, year, month, day).show();
    }

    private void showTimePickerDialog(TextView targetTimePicker) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            targetTimePicker.setText(selectedTime);
        }, hour, minute, true).show();
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleReminderNotification(Reminder reminder, String timeForNotification) {

        long notificationTime = DateUtil.calculateNotificationTime(reminder.getFromTime(), timeForNotification);

        Intent intent = getIntent(reminder, notificationTime);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            (int) reminder.getId(),
            intent,
            PendingIntent.FLAG_MUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
                );
            }
        }

    }

    @NonNull
    private Intent getIntent(Reminder reminder, long notificationTime) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("id", reminder.getId());
        intent.putExtra("title", reminder.getTitle());
        intent.putExtra("description", reminder.getDescription());
        intent.putExtra("type", reminder.getType());
        intent.putExtra("repeat", reminder.getRepeat());
        intent.putExtra("reminderTimeFrom", reminder.getFromTime());
        intent.putExtra("reminderTimeTo", reminder.getToTime());
        intent.putExtra("notificationTime", notificationTime);
        return intent;
    }

    private void cancelReminderNotification(Reminder reminder) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent oldPendingIntent = PendingIntent.getBroadcast(
            this,
            (int) reminder.getId(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(oldPendingIntent);
        }
    }

    private void setupDelete(){
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Reminder reminderData = reminderDataList.get(position);
                showDeleteConfirmationDialog(reminderData);
            }
        });
        itemTouchHelper.attachToRecyclerView(reminderRecyclerView);
    }

    private void showDeleteConfirmationDialog(Reminder reminderData) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Yes", (dialog, which) -> delete(reminderData))
            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
    }

    private void delete(Reminder reminderData) {
        cancelReminderNotification(reminderData);
        reminderViewModel.delete(reminderData);
        reminderDataAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission granted");
            } else {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission denied");
            }
        }
    }

    public static void showMaterialColorPicker(Context context, View previewView, Consumer<Integer> onColorSelected) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        GridLayout colorGrid = dialogView.findViewById(R.id.colorGrid);

        int[] colors = {
                ContextCompat.getColor(context, R.color.colorOption1),
                ContextCompat.getColor(context, R.color.colorOption2),
                ContextCompat.getColor(context, R.color.colorOption3),
                ContextCompat.getColor(context, R.color.colorOption4),
                ContextCompat.getColor(context, R.color.colorOption5),
                ContextCompat.getColor(context, R.color.colorOption6),
                ContextCompat.getColor(context, R.color.colorOption7),
                ContextCompat.getColor(context, R.color.colorOption8),
                ContextCompat.getColor(context, R.color.colorOption9),
                ContextCompat.getColor(context, R.color.colorOption10)
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle("Pick a Color")
                .setView(dialogView)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        for (int color : colors) {

            View colorCircle = new View(context);
            int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(16, 16, 16, 16);
            colorCircle.setLayoutParams(params);
            colorCircle.setBackground(ContextCompat.getDrawable(context, R.drawable.color_circle));
            colorCircle.getBackground().setTint(color);

            colorCircle.setOnClickListener(v -> {
                onColorSelected.accept(color);
                dialog.dismiss();
            });

            colorGrid.addView(colorCircle);

        }

        dialog.show();

    }

    public static void setupAllDayToggle(Switch switchBox, View timeFrom, View timeTo, Spinner spinnerTime, Spinner spinnerTimeDaily) {
        boolean isChecked = switchBox.isChecked();
        updateTimeViewsVisibility(isChecked, timeFrom, timeTo, spinnerTime, spinnerTimeDaily);
        switchBox.setOnCheckedChangeListener((buttonView, checked) ->
                updateTimeViewsVisibility(checked, timeFrom, timeTo, spinnerTime, spinnerTimeDaily)
        );
    }

    public static void updateTimeViewsVisibility(boolean isAllDay, View timeFrom, View timeTo, Spinner spinnerTime, Spinner spinnerTimeDaily) {
        int timeViewsVisibility = isAllDay ? View.GONE : View.VISIBLE;
        timeFrom.setVisibility(timeViewsVisibility);
        timeTo.setVisibility(timeViewsVisibility);
        spinnerTime.setVisibility(isAllDay ? View.GONE : View.VISIBLE);
        spinnerTimeDaily.setVisibility(isAllDay ? View.VISIBLE : View.GONE);
    }

    public static void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    static class CustomFontAdapter extends ArrayAdapter<String> {
        private final Typeface font;
        public CustomFontAdapter(Context context, int resource, List<String> items, Typeface font) {
            super(context, resource, items);
            this.font = font;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setTypeface(font);
            return view;
        }
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setTypeface(font);
            return view;
        }
    }

}
