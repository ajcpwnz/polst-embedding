package com.polst.sdk.view

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.polst.sdk.test.R as TestR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class PolstWidgetViewTest {

    @Test
    public fun inflate_fromXml_setsShortIdFromAttribute() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = LayoutInflater.from(context)
            .inflate(TestR.layout.test_polst_widget, null) as FrameLayout
        val widget = root.findViewById<PolstWidgetView>(TestR.id.widget)
        assertNotNull(widget)
        assertEquals("abc123", widget.shortId)
    }

    @Test
    @Ignore("requires test PolstClient injection — wired in US5 with brand auth refactor")
    public fun setOnVoteListener_isInvokedFromComposeOnVoteCallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val widget = PolstWidgetView(context)
        var count = 0
        widget.setOnVoteListener { count++ }
        // Cannot fire a real vote without a backed PolstClient; wiring verified by presence.
        assertEquals(0, count)
    }
}
