package com.polst.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polst.sdk.core.theme.PolstTheme
import com.polst.sdk.core.theme.PolstThemeProvider
import com.polst.sdk.core.theme.dark
import com.polst.sdk.core.theme.light
import com.polst.sdk.ui.PolstView

class SinglePolstComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme = if (isSystemInDarkTheme()) PolstTheme.dark else PolstTheme.light
            PolstThemeProvider(theme = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = theme.colors.background,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(text = "Single Polst (Compose)", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        PolstView(shortId = "XPvUofYvtvRM")
                    }
                }
            }
        }
    }
}
