// MonitorActivity.java
package com.example.chroniccare;

import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;

public class MonitorActivity extends BottomNavActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        LineChart chartGlucose = findViewById(R.id.chartGlucose);
        BarChart comparision = findViewById(R.id.comparision);

        // ================= Line Chart (Blood Glucose Trend) =================
        ArrayList<Entry> lineEntries = new ArrayList<>();
        lineEntries.add(new Entry(0, 106));
        lineEntries.add(new Entry(1, 118));
        lineEntries.add(new Entry(2, 99));
        lineEntries.add(new Entry(3, 141));
        lineEntries.add(new Entry(4, 105));
        lineEntries.add(new Entry(5, 115));
        lineEntries.add(new Entry(6, 112));

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Blood Glucose");
        lineDataSet.setColor(Color.parseColor("#26A69A"));
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleColor(Color.parseColor("#26A69A"));
        lineDataSet.setCircleRadius(4f);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chartGlucose.setData(new LineData(lineDataSet));
        chartGlucose.getDescription().setEnabled(false);
        chartGlucose.getLegend().setEnabled(false);

        // Y-Axis (Line chart)
        YAxis yAxisLeft = chartGlucose.getAxisLeft();
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setAxisMaximum(160f);
        chartGlucose.getAxisRight().setEnabled(false);
        yAxisLeft.enableGridDashedLine(10f, 5f, 0f);
        yAxisLeft.setAxisLineColor(Color.BLACK);

        // X-Axis (Line chart)
        XAxis xAxis = chartGlucose.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        xAxis.setDrawLabels(false);
        xAxis.setAxisLineColor(Color.BLACK);

        chartGlucose.invalidate();


        // ================= Bar Chart (Before vs After Meal) =================
        ArrayList<BarEntry> beforeMeal = new ArrayList<>();
        ArrayList<BarEntry> afterMeal = new ArrayList<>();

        // Example values for each day
        beforeMeal.add(new BarEntry(0, 110));
        afterMeal.add(new BarEntry(0, 118));

        beforeMeal.add(new BarEntry(1, 120));
        afterMeal.add(new BarEntry(1, 118));

        beforeMeal.add(new BarEntry(2, 135));
        afterMeal.add(new BarEntry(2, 140));

        beforeMeal.add(new BarEntry(3, 130));
        afterMeal.add(new BarEntry(3, 140));

        beforeMeal.add(new BarEntry(4, 115));
        afterMeal.add(new BarEntry(4, 107));

        beforeMeal.add(new BarEntry(5, 125));
        afterMeal.add(new BarEntry(5, 130));

        beforeMeal.add(new BarEntry(6, 108));
        afterMeal.add(new BarEntry(6, 120));

        BarDataSet setBefore = new BarDataSet(beforeMeal, "Before Meal");
        setBefore.setColor(ContextCompat.getColor(this, R.color.teal_700));

        BarDataSet setAfter = new BarDataSet(afterMeal, "After Meal");
        setAfter.setColor(ContextCompat.getColor(this, R.color.teal_200));

        BarData barData = new BarData(setBefore, setAfter);
        barData.setBarWidth(0.4f);

        comparision.setData(barData);
        comparision.groupBars(0f, 0.2f, 0.05f); // group spacing

        comparision.getDescription().setEnabled(false);
        comparision.setDrawGridBackground(false);
        comparision.setDrawBarShadow(false);
        comparision.getLegend().setEnabled(false);

        // X-Axis (days of week)
        XAxis barXAxis = comparision.getXAxis();
        barXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        barXAxis.setGranularity(1f);
        barXAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        barXAxis.setDrawGridLines(false);

        // Y-Axis
        YAxis barLeft = comparision.getAxisLeft();
        barLeft.setAxisMinimum(0f);
        barLeft.setAxisMaximum(160f);
        comparision.getAxisRight().setEnabled(false);

        // ✅ Custom labels above bars (difference between after & before)
        final int[] diffs = {+8, -2, +5, +10, -8, +5, +12};

        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                int xIndex = (int) barEntry.getX();
                // Show label only on AFTER meal bar
                if (barEntry.getY() == afterMeal.get(xIndex).getY()) {
                    int diff = diffs[xIndex];
                    return (diff >= 0 ? "+" : "") + diff;
                }
                return "";
            }
        });

        barData.setValueTextSize(12f);
        barData.setValueTextColor(Color.BLACK);

        comparision.invalidate();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_monitor;
    }

    protected int getBottomNavMenuItemId() {
        return R.id.nav_monitor;
    }
}
