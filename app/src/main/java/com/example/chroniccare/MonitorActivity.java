// MonitorActivity.java
package com.example.chroniccare;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
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


        LineChart chartGlucose = findViewById(R.id.chartGlucose);
        BarChart comparision = findViewById(R.id.barchart);

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


        // ================= Bar Chart (Before vs After Meal) - COMPLETE VERSION =================
        ArrayList<BarEntry> beforeMeal = new ArrayList<>();
        ArrayList<BarEntry> afterMeal = new ArrayList<>();

// Complete data for all 7 days
        beforeMeal.add(new BarEntry(0, 110f)); // Mon
        afterMeal.add(new BarEntry(0, 118f));

        beforeMeal.add(new BarEntry(1, 120f)); // Tue
        afterMeal.add(new BarEntry(1, 118f));

        beforeMeal.add(new BarEntry(2, 135f)); // Wed
        afterMeal.add(new BarEntry(2, 140f));

        beforeMeal.add(new BarEntry(3, 130f)); // Thu
        afterMeal.add(new BarEntry(3, 140f));

        beforeMeal.add(new BarEntry(4, 115f)); // Fri
        afterMeal.add(new BarEntry(4, 107f));

        beforeMeal.add(new BarEntry(5, 125f)); // Sat
        afterMeal.add(new BarEntry(5, 130f));

        beforeMeal.add(new BarEntry(6, 108f)); // Sun
        afterMeal.add(new BarEntry(6, 120f));

        BarDataSet setBefore = new BarDataSet(beforeMeal, "Before Meal");
        setBefore.setColor(ContextCompat.getColor(this, R.color.teal_700));

        BarDataSet setAfter = new BarDataSet(afterMeal, "After Meal");
        setAfter.setColor(ContextCompat.getColor(this, R.color.teal_200));

        BarData barData = new BarData(setBefore, setAfter);
        barData.setBarWidth(0.4f);

        comparision.setData(barData);
        comparision.groupBars(0f, 0.4f, 0.06f); // Adjust group spacing

        comparision.getDescription().setEnabled(false);
        comparision.setDrawGridBackground(false);
        comparision.setDrawBarShadow(false);
        comparision.getLegend().setEnabled(true); // Enable legend to show Before/After colors

// X-Axis - Show all day labels
        XAxis barXAxis = comparision.getXAxis();
        barXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        barXAxis.setGranularity(1f);
        barXAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        barXAxis.setDrawGridLines(false);
        barXAxis.setLabelCount(7); // Ensure all 7 labels are shown
        barXAxis.setCenterAxisLabels(true);

// Y-Axis
        YAxis barLeft = comparision.getAxisLeft();
        barLeft.setAxisMinimum(0f);
        barLeft.setAxisMaximum(160f);
        barLeft.setGranularity(20f); // Show grid lines at intervals of 20
        barLeft.setDrawGridLines(true);
        barLeft.setGridColor(Color.parseColor("#E0E0E0"));
        comparision.getAxisRight().setEnabled(false);

// Custom labels above bars showing the difference
        final int[] diffs = {+8, -2, +5, +10, -8, +5, +12};

        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                int xIndex = (int) barEntry.getX();
                // Show difference label only on AFTER meal bars
                if (barEntry.getData() != null && barEntry.getData().equals("after")) {
                    int diff = diffs[xIndex];
                    return (diff >= 0 ? "+" : "") + diff;
                }
                return "";
            }
        });

// Mark after meal entries for label display
        for (int i = 0; i < afterMeal.size(); i++) {
            afterMeal.get(i).setData("after");
        }

        barData.setValueTextSize(12f);
        barData.setValueTextColor(Color.BLACK);
        barData.setValueTypeface(Typeface.DEFAULT_BOLD);

// Additional chart styling
        comparision.setFitBars(true);
        comparision.setDrawValueAboveBar(true);
        comparision.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        comparision.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        comparision.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        comparision.getLegend().setDrawInside(false);
        comparision.getLegend().setYOffset(10f);

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
