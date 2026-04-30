package com.polst.sdk.core.logging

import android.util.Log
import com.polst.sdk.BuildConfig

public interface PolstLogger {
    public fun verbose(tag: String, message: String)
    public fun debug(tag: String, message: String)
    public fun info(tag: String, message: String)
    public fun warn(tag: String, message: String, throwable: Throwable? = null)
    public fun error(tag: String, message: String, throwable: Throwable? = null)

    public companion object {
        @JvmStatic
        public fun default(): PolstLogger =
            if (BuildConfig.DEBUG) AndroidLogcatLogger() else NoOpPolstLogger
    }
}

public object NoOpPolstLogger : PolstLogger {
    override fun verbose(tag: String, message: String) {}
    override fun debug(tag: String, message: String) {}
    override fun info(tag: String, message: String) {}
    override fun warn(tag: String, message: String, throwable: Throwable?) {}
    override fun error(tag: String, message: String, throwable: Throwable?) {}
}

public class AndroidLogcatLogger(private val enableVerbose: Boolean = false) : PolstLogger {

    override fun verbose(tag: String, message: String) {
        if (!enableVerbose) return
        Log.v(prefix(tag), scrub(message))
    }

    override fun debug(tag: String, message: String) {
        Log.d(prefix(tag), scrub(message))
    }

    override fun info(tag: String, message: String) {
        Log.i(prefix(tag), scrub(message))
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.w(prefix(tag), scrub(message), throwable)
        } else {
            Log.w(prefix(tag), scrub(message))
        }
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(prefix(tag), scrub(message), throwable)
        } else {
            Log.e(prefix(tag), scrub(message))
        }
    }

    private fun prefix(tag: String): String = "Polst-$tag"

    private fun scrub(message: String): String = message.replace(QUERY_REGEX, "?<scrubbed>")

    private companion object {
        private val QUERY_REGEX = Regex("\\?[^\\s]*")
    }
}
