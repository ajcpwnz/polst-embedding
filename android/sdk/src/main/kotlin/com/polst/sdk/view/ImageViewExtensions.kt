package com.polst.sdk.view

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.polst.sdk.network.image.PolstImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View-system convenience for loading an image URL into an [ImageView] via the
 * SDK's first-party [PolstImageLoader].
 *
 * The load is scoped to the view's [androidx.lifecycle.LifecycleOwner] when one
 * is available, so it is cancelled automatically when the hosting activity or
 * fragment is destroyed. When no lifecycle is attached (e.g. the view is not
 * yet attached to a window), the load falls back to [GlobalScope].
 *
 * @param url HTTPS URL to load.
 * @param placeholderRes Drawable resource displayed synchronously before the
 *   network/cache load completes. Pass `0` to leave the current drawable.
 * @param errorRes Drawable resource displayed if the load fails (or returns
 *   `null`). Pass `0` to leave the placeholder in place.
 */
@Suppress("OPT_IN_USAGE")
public fun ImageView.polstSetImage(
    url: String,
    placeholderRes: Int = 0,
    errorRes: Int = 0,
) {
    if (placeholderRes != 0) {
        setImageResource(placeholderRes)
    }

    val loader: PolstImageLoader = PolstImageLoader.create(context)
    val owner = findViewTreeLifecycleOwner()

    val scope: CoroutineScope = if (owner != null) {
        owner.lifecycleScope
    } else {
        // Falls back to GlobalScope when not in a lifecycle-aware view tree.
        GlobalScope
    }

    scope.launch {
        val bitmap: Bitmap? = loader.load(url)
        withContext(Dispatchers.Main) {
            if (bitmap != null) {
                setImageBitmap(bitmap)
            } else if (errorRes != 0) {
                setImageResource(errorRes)
            }
        }
    }
}
