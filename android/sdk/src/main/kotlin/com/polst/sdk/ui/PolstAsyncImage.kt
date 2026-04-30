package com.polst.sdk.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.polst.sdk.network.image.PolstImageLoader

/**
 * Internal UI state for [PolstAsyncImage]. Kept file-private so the sealed class
 * is not part of the SDK's public API surface.
 */
private sealed class ImageState {
    data object Loading : ImageState()
    data class Success(val bitmap: Bitmap) : ImageState()
    data class Failure(val cause: Throwable) : ImageState()
}

/**
 * First-party async image composable backed by [PolstImageLoader].
 *
 * Constitution Principle II forbids third-party image libraries, so this
 * composable is the canonical way to render remote bitmaps inside SDK widgets
 * (and is public so hosts can reuse the same loader in their own UI).
 *
 * While loading, [placeholder] is rendered if provided; otherwise an empty
 * [Box] with the supplied [modifier] is rendered to reserve layout space. On
 * failure the [error] slot is invoked with the causing [Throwable], again
 * falling back to an empty [Box].
 */
@Composable
public fun PolstAsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: (@Composable () -> Unit)? = null,
    error: (@Composable (Throwable) -> Unit)? = null,
) {
    val context = LocalContext.current
    val loader = remember(context) { PolstImageLoader.create(context) }
    var state: ImageState by remember { mutableStateOf(ImageState.Loading) }

    LaunchedEffect(url) {
        state = ImageState.Loading
        state = try {
            val bitmap: Bitmap? = loader.load(url)
            if (bitmap == null) {
                ImageState.Failure(RuntimeException("Image load returned null"))
            } else {
                ImageState.Success(bitmap)
            }
        } catch (t: Throwable) {
            ImageState.Failure(t)
        }
    }

    when (val s = state) {
        is ImageState.Loading -> placeholder?.invoke() ?: Box(modifier)
        is ImageState.Success -> Image(
            bitmap = s.bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        is ImageState.Failure -> error?.invoke(s.cause) ?: Box(modifier)
    }
}
