package com.smartcalendar.ai.ui.calendar;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smartcalendar.ai.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    private static final int MAX_BARS = 3;

    public static class DayCell {
        public final long dayStart;
        public final int dayOfMonth;

        public DayCell(long dayStart, int dayOfMonth) {
            this.dayStart = dayStart;
            this.dayOfMonth = dayOfMonth;
        }
    }

    public interface OnDayClickListener {
        void onDayClick(long dayStart);
    }

    private final OnDayClickListener listener;
    private List<DayCell> cells = new ArrayList<>();
    private Map<Long, List<Integer>> dayColors = Collections.emptyMap();
    private long selectedDay;
    private long today;

    public CalendarDayAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<DayCell> cells, Map<Long, List<Integer>> dayColors, long selectedDay, long today) {
        this.cells = cells;
        this.dayColors = dayColors;
        this.selectedDay = selectedDay;
        this.today = today;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        DayCell cell = cells.get(position);

        if (cell.dayOfMonth == 0) {
            holder.tvDay.setText("");
            holder.tvDay.setBackground(null);
            for (View bar : holder.bars) {
                bar.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            return;
        }

        holder.tvDay.setText(String.valueOf(cell.dayOfMonth));
        boolean selected = cell.dayStart == selectedDay;
        boolean isToday = cell.dayStart == today;
        int column = position % 7;

        int textColor;
        if (selected) {
            holder.tvDay.setBackgroundResource(R.drawable.bg_day_selected);
            textColor = R.color.background;
        } else {
            if (isToday) {
                holder.tvDay.setBackgroundResource(R.drawable.bg_day_today);
            } else {
                holder.tvDay.setBackground(null);
            }
            if (column == 5) {
                textColor = R.color.weekend_sat;
            } else if (column == 6) {
                textColor = R.color.weekend_sun;
            } else {
                textColor = isToday ? R.color.primary : R.color.text_primary;
            }
        }
        holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), textColor));

        List<Integer> colors = dayColors.get(cell.dayStart);
        for (int i = 0; i < MAX_BARS; i++) {
            View bar = holder.bars[i];
            if (colors != null && i < colors.size()) {
                bar.setBackgroundTintList(ColorStateList.valueOf(colors.get(i)));
                bar.setVisibility(View.VISIBLE);
            } else {
                bar.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onDayClick(cell.dayStart));
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDay;
        final View[] bars = new View[MAX_BARS];

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            bars[0] = itemView.findViewById(R.id.bar1);
            bars[1] = itemView.findViewById(R.id.bar2);
            bars[2] = itemView.findViewById(R.id.bar3);
        }
    }
}
