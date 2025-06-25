package com.steph.reminder.calendar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

import java.util.List;

public class MultiDotSpan implements LineBackgroundSpan {

    private final int radius;
    private final List<Integer> colors;

    public MultiDotSpan(int radius, List<Integer> colors) {
        this.radius = radius;
        this.colors = colors.size() > 3 ? colors.subList(0, 3) : colors;
    }

    @Override
    public void drawBackground(
            Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom,
            CharSequence charSequence, int start, int end, int lineNum) {

        int total = colors.size();
        if (total == 0) return;

        int centerX = (left + right) / 2;
        int centerY = bottom + radius * 2;

        int spacing = 2 * radius + 4;
        int totalWidth = (total - 1) * spacing;
        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < total; i++) {
            paint.setColor(colors.get(i));
            canvas.drawCircle(startX + i * spacing, centerY, radius, paint);
        }
    }

}
