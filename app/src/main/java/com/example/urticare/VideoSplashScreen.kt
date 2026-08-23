package com.example.urticare

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoSplashScreen(onVideoFinished: () -> Unit) {
    val context = LocalContext.current

    // Set up the player with your local 2-second video
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.urticare_logo}")
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true

            // Listen for the absolute end of the 2-second video
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onVideoFinished() // Trigger the switch to the main screen
                    }
                }
            })
        }
    }

    // Fullscreen video layout with controllers hidden
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Completely hides play/pause/timeline bars
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Forces fullscreen fit
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release() // Clean up memory when leaving the splash screen
        }
    }
}