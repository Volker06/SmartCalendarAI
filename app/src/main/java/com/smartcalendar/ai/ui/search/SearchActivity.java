package com.smartcalendar.ai.ui.search;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.smartcalendar.ai.R;
import com.smartcalendar.ai.ui.calendar.EventAdapter;
import com.smartcalendar.ai.ui.event.DeleteConfirmDialog;
import com.smartcalendar.ai.ui.event.EventFormActivity;
import com.smartcalendar.ai.util.EventColors;
import com.smartcalendar.ai.util.SearchParser;
import com.smartcalendar.ai.util.SystemBars;
import com.smartcalendar.ai.util.TimeUtils;
import com.smartcalendar.ai.viewmodel.SearchViewModel;

import java.util.Calendar;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private static final int PAST_YEARS = 2;
    private static final int FUTURE_YEARS = 2;

    private SearchViewModel viewModel;
    private EventAdapter adapter;

    private EditText etSearch;
    private TextView tvMessage;
    private LinearLayout colorFilterRow;

    private long rangeStart;
    private long rangeEnd;
    private int selectedColor = 0;
    private boolean searching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        SystemBars.apply(this, findViewById(R.id.appBar), findViewById(R.id.content));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        etSearch = findViewById(R.id.etSearch);
        tvMessage = findViewById(R.id.tvMessage);
        colorFilterRow = findViewById(R.id.colorFilterRow);
        TextView tvRange = findViewById(R.id.tvRange);
        RecyclerView rvResults = findViewById(R.id.rvResults);

        long thisMonth = TimeUtils.startOfMonth(System.currentTimeMillis());
        rangeStart = TimeUtils.addMonths(thisMonth, -12 * PAST_YEARS);
        rangeEnd = TimeUtils.addMonths(thisMonth, 12 * FUTURE_YEARS + 1);
        tvRange.setText(getString(R.string.search_range, rangeLabel()[0], rangeLabel()[1]));

        setupColorFilter();

        adapter = new EventAdapter(
                event -> {
                    Intent intent = new Intent(this, EventFormActivity.class);
                    intent.putExtra(EventFormActivity.EXTRA_EVENT_ID, event.id);
                    startActivity(intent);
                },
                event -> DeleteConfirmDialog.show(this, event, () -> {
                    viewModel.delete(event);
                    Toast.makeText(this, R.string.toast_deleted, Toast.LENGTH_SHORT).show();
                }));
        adapter.setShowDate(true);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        viewModel.getResults().observe(this, this::onResults);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                runSearch();
            }
        });
        etSearch.setOnEditorActionListener((v, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        runSearch();
    }

    private String[] rangeLabel() {
        return new String[]{TimeUtils.formatMonthYear(rangeStart), TimeUtils.formatMonthYear(rangeEnd - 1)};
    }

    private void setupColorFilter() {
        for (int color : EventColors.PALETTE) {
            View swatch = getLayoutInflater().inflate(R.layout.item_color_swatch, colorFilterRow, false);
            swatch.findViewById(R.id.swatchCircle).setBackgroundTintList(ColorStateList.valueOf(color));
            swatch.setOnClickListener(v -> {
                selectedColor = (selectedColor == color) ? 0 : color;
                updateColorSelection();
                runSearch();
            });
            colorFilterRow.addView(swatch);
        }
        updateColorSelection();
    }

    private void updateColorSelection() {
        for (int i = 0; i < colorFilterRow.getChildCount(); i++) {
            boolean selected = EventColors.PALETTE[i] == selectedColor;
            colorFilterRow.getChildAt(i).findViewById(R.id.swatchCheck)
                    .setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void runSearch() {
        SearchParser.Result parsed = SearchParser.parse(etSearch.getText().toString());

        if (parsed.invalidDate) {
            searching = false;
            viewModel.clear();
            showMessage(getString(R.string.search_invalid_date));
            return;
        }

        boolean hasFilter = !parsed.keyword.isEmpty() || parsed.hasMonth || parsed.hasDate || selectedColor != 0;

        if (!hasFilter) {
            searching = false;
            viewModel.clear();
            showMessage(getString(R.string.search_prompt));
            return;
        }

        long start = rangeStart;
        long end = rangeEnd;
        if (parsed.hasDate || parsed.hasMonth) {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(parsed.year, parsed.month - 1, parsed.hasDate ? parsed.day : 1);
            long from = c.getTimeInMillis();
            if (from < rangeStart || from >= rangeEnd) {
                searching = false;
                viewModel.clear();
                showMessage(getString(R.string.search_out_of_range, rangeLabel()[0], rangeLabel()[1]));
                return;
            }
            start = from;
            end = parsed.hasDate ? TimeUtils.nextDayStart(from) : TimeUtils.nextMonthStart(from);
        }

        searching = true;
        viewModel.search(start, end, selectedColor, parsed.keyword);
    }

    private void onResults(List<com.smartcalendar.ai.data.local.Event> events) {
        adapter.submitList(events);
        if (!searching) {
            return;
        }
        if (events == null || events.isEmpty()) {
            showMessage(getString(R.string.search_empty));
        } else {
            showMessage(getString(R.string.search_result_count, events.size()));
        }
    }

    private void showMessage(String message) {
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
    }
}
