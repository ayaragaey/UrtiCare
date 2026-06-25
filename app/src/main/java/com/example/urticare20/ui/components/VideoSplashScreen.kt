package com.example.urticare20.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.urticare20.R
import com.example.urticare20.ui.theme.AmoledBlack
import kotlinx.coroutines.delay

/**
 * VideoSplashScreen - A clean, standalone video-based splash/opener screen for UrtiCare.
 * It plays the local resource "urticare_logo.mp4" in full screen,
 * hides the playback controls, and automatically triggers navigation after the video ends
 * with an additional 4-second pause to let the user see the logo clearly.
 *
 * @param onVideoFinished Callback invoked when the video has finished playing or an error occurs.
 * @param safetyTimeoutMs Maximum time to wait in milliseconds before forcing transition (default 8000ms).
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoSplashScreen(
    onVideoFinished: () -> Unit,
    modifier: Modifier = Modifier,
    safetyTimeoutMs: Long = 8000L
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Flag to ensure navigation is only triggered once
    var isNavigating by remember { mutableStateOf(false) }

    // Initialize ExoPlayer safely
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Build the URI pointing to the raw resource folder
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.urticare_logo}")
            val mediaItem = MediaItem.fromUri(videoUri)
            
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // Listener to navigate as soon as the video finishes or if there is an error
    val listener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !isNavigating) {
                    isNavigating = true
                    coroutineScope.launch {
                        delay(4000) // Pause for 4 seconds on the final frame so user sees logo clearly
                        onVideoFinished()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // If a playback error occurs, immediately fallback and proceed to the app
                if (!isNavigating) {
                    isNavigating = true
                    onVideoFinished()
                }
            }
        }
    }

    // Bind and manage ExoPlayer lifecycle listener and resource release
    DisposableEffect(exoPlayer) {
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Sync ExoPlayer playback with standard Android Activity lifecycle changes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Only resume if we aren't already navigating/finished
                    if (!isNavigating) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Safety timeout fallback: navigates to main layout if video gets stuck or fails to fire events
    LaunchedEffect(Unit) {
        delay(safetyTimeoutMs)
        if (!isNavigating) {
            isNavigating = true
            onVideoFinished()
        }
    }

    // Render the PlayerView using Compose's AndroidView wrapper
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Hide video playback controls completely
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill full screen smoothly
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
