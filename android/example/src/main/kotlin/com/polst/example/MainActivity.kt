package com.polst.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(text = "PolstSDK Demo", fontSize = 28.sp)
                        Text(
                            text = "Backed by Environment.Production (canary-api.polst.app).\n" +
                                "Try airplane mode after first vote to see offline replay.",
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        DemoButton("Single Polst (Compose)", SinglePolstComposeActivity::class.java)
                        DemoButton("Single Polst (XML)", SinglePolstXmlActivity::class.java)
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoButton(label: String, target: Class<*>) {
    val context = LocalContext.current
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { context.startActivity(Intent(context, target)) },
    ) {
        Text(text = label)
    }
}
