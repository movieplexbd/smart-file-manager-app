package com.smart.filemanager.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.smart.filemanager.R;
import com.smart.filemanager.logic.FileViewModel;

import java.util.ArrayList;
import java.util.List;

public class StorageAnalyticsFragment extends Fragment {

    private FileViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_storage_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);

        PieChart pieChart = view.findViewById(R.id.pie_chart);
        TextView tvTotal = view.findViewById(R.id.tv_total_storage);
        TextView tvUsed = view.findViewById(R.id.tv_used_storage);
        TextView tvFree = view.findViewById(R.id.tv_free_storage);

        setupPieChart(pieChart);
        viewModel.loadStorageStats();

        viewModel.getStorageStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats == null) return;
            long total = stats[0];
            long used = stats[1];
            long free = stats[2];

            tvTotal.setText("Total: " + formatSize(total));
            tvUsed.setText("Used: " + formatSize(used));
            tvFree.setText("Free: " + formatSize(free));

            List<PieEntry> entries = new ArrayList<>();
            entries.add(new PieEntry((float) used, "Used"));
            entries.add(new PieEntry((float) free, "Free"));

            PieDataSet dataSet = new PieDataSet(entries, "Storage");
            dataSet.setColors(0xFF4DA3FF, 0xFFE0E0E0);
            dataSet.setSliceSpace(3f);
            dataSet.setValueTextSize(14f);

            PieData data = new PieData(dataSet);
            pieChart.setData(data);
            pieChart.invalidate();
        });
    }

    private void setupPieChart(PieChart chart) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(android.graphics.Color.WHITE);
        chart.setHoleRadius(55f);
        chart.setTransparentCircleRadius(60f);
        chart.setCenterText("Storage");
        chart.setCenterTextSize(18f);
        chart.setRotationEnabled(false);
        chart.getLegend().setEnabled(true);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        else return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
