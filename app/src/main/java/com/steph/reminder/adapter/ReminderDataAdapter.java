package com.steph.reminder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.steph.reminder.R;
import com.steph.reminder.data.db.Reminder;
import com.steph.reminder.util.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderDataAdapter extends RecyclerView.Adapter<ReminderDataAdapter.ReminderViewHolder> {

    private List<Reminder> reminderList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Reminder reminder);
    }

    public ReminderDataAdapter(List<Reminder> reminderList, OnItemClickListener listener) {
        this.reminderList = reminderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {

        Reminder reminder = reminderList.get(position);

        holder.titleTextView.setText(reminder.getTitle());
        holder.typeTextView.setText(reminder.getType() + (reminder.getDescription().isEmpty() ? "" : ": " + reminder.getDescription()));
        holder.priorityTextView.setText(reminder.getPriority());

        String formattedDateFrom = DateUtil.formattedDate(new Date(reminder.getFromTime()));
        String formattedDateTo = DateUtil.formattedDate(new Date(reminder.getToTime()));

        holder.timeTextView.setText(formattedDateFrom.equals(formattedDateTo) ? formattedDateFrom : formattedDateFrom + " / " + formattedDateTo);

        holder.linearLayout.setBackgroundColor(reminder.getColor());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(reminder);
            }
        });

    }

    @Override
    public int getItemCount() {
        return reminderList != null ? reminderList.size() : 0;
    }

    public static class ReminderViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView, typeTextView, priorityTextView, timeTextView;
        CardView cardView;
        LinearLayout linearLayout;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.reminderTitle);
            typeTextView = itemView.findViewById(R.id.reminderType);
            priorityTextView = itemView.findViewById(R.id.priority);
            timeTextView = itemView.findViewById(R.id.reminderTime);
            cardView = itemView.findViewById(R.id.reminderCard);
            linearLayout = itemView.findViewById(R.id.reminderLayout);
        }
    }

}



