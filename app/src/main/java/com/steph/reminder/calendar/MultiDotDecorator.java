package com.steph.reminder.calendar;

import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiDotDecorator implements DayViewDecorator {

    private final CalendarDay date;
    private final List<Integer> colors;

    public MultiDotDecorator(CalendarDay date, Collection<Integer> colors) {
        this.date = date;
        this.colors = new ArrayList<>(colors);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(date);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new MultiDotSpan(8, colors));
    }
}
