package com.polst.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.polst.sdk.view.PolstWidgetView

class SinglePolstXmlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_polst_xml)
        findViewById<PolstWidgetView>(R.id.polst).setOnVoteListener { vote ->
            Log.d("polst-example", "voted ${vote.optionId}")
        }
    }
}
