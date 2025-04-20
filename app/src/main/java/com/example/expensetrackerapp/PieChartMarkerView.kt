package com.example.expensetrackerapp

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.example.expensetrackerapp.R

class PieChartMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    private val tvMarker: TextView = findViewById(R.id.tvMarker)

    override fun refreshContent(e: com.github.mikephil.charting.data.Entry?, highlight: Highlight?) {
        if (e is PieEntry) {
            tvMarker.text = e.label
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // center the marker horizontally and place above the entry
        return MPPointF((-width / 2).toFloat(), (-height).toFloat())
    }
}
