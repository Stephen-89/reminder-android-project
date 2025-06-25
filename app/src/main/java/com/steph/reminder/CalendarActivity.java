package com.steph.reminder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.steph.reminder.calendar.MultiDotDecorator;
import com.steph.reminder.calendar.ReminderDecorator;
import com.steph.reminder.data.db.Reminder;
import com.steph.reminder.adapter.ReminderDataAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private List<Reminder> reminderDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);

        Intent intent = getIntent();
        reminderDataList = (List<Reminder>) intent.getSerializableExtra("reminders");

        if (reminderDataList != null) {
            plotAllRemindersOnCalendar();
        } else {
            Toast.makeText(this, "No reminders to display", Toast.LENGTH_SHORT).show();
        }

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            showDayReminderBottomSheet(date);
        });

        FloatingActionButton btnCalendar = findViewById(R.id.btnBack);
        btnCalendar.setOnClickListener(v -> startActivity( new Intent(CalendarActivity.this, MainActivity.class)));

    }

    private void plotAllRemindersOnCalendar() {

        long today = Calendar.getInstance().getTimeInMillis();
        long cutoff = today + (1000L * 60 * 60 * 24 * 365);

        Map<CalendarDay, List<Integer>> dayToColorsMap = new HashMap<>();

        for (Reminder reminder : reminderDataList) {

            long from = reminder.getFromTime();
            long to = reminder.getToTime();
            String repeat = reminder.getRepeat();

            while (from <= cutoff) {

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(from);

                while (calendar.getTimeInMillis() <= to) {

                    CalendarDay day = CalendarDay.from(calendar);
                    int color = reminder.getColor();

                    List<Integer> colors = dayToColorsMap.get(day);
                    if (colors == null) {
                        colors = new ArrayList<>();
                        dayToColorsMap.put(day, colors);
                    }
                    colors.add(color);

                    calendar.add(Calendar.DAY_OF_MONTH, 1);

                }

                if ("No Repeat".equalsIgnoreCase(repeat)) break;

                from = getNextReminderTime(from, repeat);
                to = getNextReminderTime(to, repeat);

            }

        }

        for (Map.Entry<CalendarDay, List<Integer>> entry : dayToColorsMap.entrySet()) {

            CalendarDay day = entry.getKey();
            Set<Integer> uniqueColors = new HashSet<>(entry.getValue());

            List<Integer> colorList = new ArrayList<>(uniqueColors);
            if (colorList.size() > 3) {
                colorList = colorList.subList(0, 3);
            }

            calendarView.addDecorator(new MultiDotDecorator(day, colorList));
            calendarView.setDateSelected(day, true);

        }

    }

    private long getNextReminderTime(long reminderTime, String repeatType) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(reminderTime);
        switch (repeatType) {
            case "Daily":
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case "Weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "Monthly":
                calendar.add(Calendar.MONTH, 1);
                break;
            case "Yearly":
                calendar.add(Calendar.YEAR, 1);
                break;
        }
        return calendar.getTimeInMillis();
    }

    private void showDayReminderBottomSheet(CalendarDay date) {

        List<Reminder> dayReminders = new ArrayList<>();

        for (Reminder reminder : reminderDataList) {

            Calendar from = Calendar.getInstance();
            from.setTimeInMillis(reminder.getFromTime());
            clearTime(from);

            Calendar to = Calendar.getInstance();
            to.setTimeInMillis(reminder.getToTime());
            clearTime(to);

            Calendar clicked = Calendar.getInstance();
            clicked.set(date.getYear(), date.getMonth(), date.getDay());
            clearTime(clicked);

            if (!clicked.before(from) && !clicked.after(to)) {
                dayReminders.add(reminder);
            }

        }

        if (dayReminders.isEmpty()) {
            Toast.makeText(this, "No reminders for this day", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_reminders, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(view);

        TextView dateTitle = view.findViewById(R.id.reminderDateTitle);
        RecyclerView recyclerView = view.findViewById(R.id.remindersRecyclerView);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
        String formattedDateFrom = sdf.format(new Date(dayReminders.get(0).getFromTime()));

        dateTitle.setText("Reminders for " + formattedDateFrom);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ReminderDataAdapter reminderDataAdapter = new ReminderDataAdapter(dayReminders, reminder -> {
            Toast.makeText(CalendarActivity.this, "Clicked: " + reminder.getTitle(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(reminderDataAdapter);

        bottomSheetDialog.show();

    }

    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

}