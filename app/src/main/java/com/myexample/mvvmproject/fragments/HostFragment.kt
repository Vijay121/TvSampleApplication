package com.myexample.mvvmproject.fragments

import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraCaptureSession
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerGlue
import androidx.leanback.media.PlaybackGlue
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import coil.Coil
import coil.api.get
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.myexample.mvvmproject.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class HostFragment : VideoSupportFragment() {
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var playerGlue: MediaPlayerGlue

    private inner class MediaPlayerGlue(context: Context, adapter: LeanbackPlayerAdapter) :
        PlaybackTransportControlGlue<LeanbackPlayerAdapter>(context, adapter) {

        private val actionRewind = PlaybackControlsRow.RewindAction(context)
        private val actionFastForward = PlaybackControlsRow.FastForwardAction(context)
        private val actionClosedCaptions = PlaybackControlsRow.ClosedCaptioningAction(context)

        fun skipForward(millis: Long = SKIP_PLAYBACK_MILLIS) =
            // Ensures we don't advance past the content duration (if set)
            player.seekTo(
                if (player.contentDuration > 0) {
                    min(player.contentDuration, player.currentPosition + millis)
                } else {
                    player.currentPosition + millis
                }
            )

        fun skipBackward(millis: Long = SKIP_PLAYBACK_MILLIS) =
            // Ensures we don't go below zero position
            player.seekTo(max(0, player.currentPosition - millis))

        override fun onCreatePrimaryActions(adapter: ArrayObjectAdapter) {
            super.onCreatePrimaryActions(adapter)
            // Append rewind and fast forward actions to our player, keeping the play/pause actions
            // created by default by the glue
            adapter.add(actionRewind)
            adapter.add(actionFastForward)
            adapter.add(actionClosedCaptions)
        }

        override fun onActionClicked(action: Action) = when (action) {
            actionRewind -> skipBackward()
            actionFastForward -> skipForward()
            else -> super.onActionClicked(action)
        }

        /** Custom function used to update the metadata displayed for currently playing media */
        fun setMetadata(metadata: String) {
            // Displays basic metadata in the player
            title = "Vijaysantosh"
            subtitle = "Number is calling"
            val uris = Uri.parse(metadata)

            // Prepares metadata playback
            val dataSourceFactory = DefaultDataSourceFactory(
                requireContext(), getString(R.string.app_name)
            )
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uris)
            player.prepare(mediaSource, false, true)
        }
    }
    private val updateMetadataTask: Runnable = object : Runnable {
        override fun run() {

            // Make sure that the view has not been destroyed
            view ?: return

            // Schedules the next metadata update in METADATA_UPDATE_INTERVAL_MILLIS milliseconds
            Log.d(TAG, "Media metadata updated successfully")
            view?.postDelayed(this, METADATA_UPDATE_INTERVAL_MILLIS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backgroundType = PlaybackSupportFragment.BG_NONE
        player = ExoPlayerFactory.newSimpleInstance(requireActivity())
        mediaSession = MediaSessionCompat(requireContext(), getString(R.string.app_name))
        mediaSessionConnector = MediaSessionConnector(mediaSession)

        mediaSessionConnector.setCaptionCallback(object : MediaSessionConnector.CaptionCallback {
            override fun onCommand(
                player: Player,
                controlDispatcher: ControlDispatcher,
                command: String,
                extras: Bundle?,
                cb: ResultReceiver?
            ): Boolean {
                Log.i(TAG, "onCommand: ${player.bufferedPercentage}")
                return false
            }

            override fun hasCaptions(player: Player): Boolean {
                Log.d(TAG, "onSetCaptioningEnabled() hascaptions= ${player.bufferedPosition}")
                return false
            }

            override fun onSetCaptioningEnabled(player: Player, enabled: Boolean) {
                Log.d(TAG, "onSetCaptioningEnabled() enabled=$enabled")
            }
        })

        val playerAdapter =
            LeanbackPlayerAdapter(requireActivity(), player, PLAYER_UPDATE_INTERVAL_MILLIS)

        playerGlue = MediaPlayerGlue(requireActivity(), playerAdapter).apply {
            host = VideoSupportFragmentGlueHost(this@HostFragment)

            addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
                override fun onPreparedStateChanged(glue: PlaybackGlue?) {
                    super.onPreparedStateChanged(glue)
                    if (glue?.isPrepared == true) {
                        seekTo(0)
                    }
                }

                override fun onPlayCompleted(glue: PlaybackGlue?) {
                    super.onPlayCompleted(glue)
                    Toast.makeText(requireActivity(), "Completed", Toast.LENGTH_SHORT).show()
                }
            })
            playWhenPrepared()
            setMetadata("https://android-tv-classics.firebaseapp.com/content/le_voyage_dans_la_lun/media_le_voyage_dans_la_lun.mp4")
        }

        adapter = ArrayObjectAdapter(playerGlue.playbackRowPresenter).apply {
            add(playerGlue.controlsRow)
        }
        playerGlue.host.setOnKeyInterceptListener { view, keyCode, event ->

            // Early exit: if the controls overlay is visible, don't intercept any keys
            if (playerGlue.host.isControlsOverlayVisible) return@setOnKeyInterceptListener false

            // TODO(owahltinez): This workaround is necessary for navigation library to work with
            //  Leanback's [PlaybackSupportFragment]
            if (!playerGlue.host.isControlsOverlayVisible &&
                keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN
            ) {
                Log.d(TAG, "Intercepting BACK key for fragment navigation")
                val navController = Navigation.findNavController(
                    requireActivity(), R.id.fragment_container
                )
                navController.currentDestination?.id?.let { navController.popBackStack(it, true) }
                return@setOnKeyInterceptListener true
            }

            // Skips ahead when user presses DPAD_RIGHT
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                playerGlue.skipForward()
                preventControlsOverlay(playerGlue)
                return@setOnKeyInterceptListener true
            }

            // Rewinds when user presses DPAD_LEFT
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.action == KeyEvent.ACTION_DOWN) {
                playerGlue.skipBackward()
                preventControlsOverlay(playerGlue)
                return@setOnKeyInterceptListener true
            }

            false
        }

    }

    /** Workaround used to prevent controls overlay from showing and taking focus */
    private fun preventControlsOverlay(playerGlue: MediaPlayerGlue) = view?.postDelayed({
        playerGlue.host.showControlsOverlay(false)
        playerGlue.host.hideControlsOverlay(false)
    }, 10)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(Color.BLACK)
    }

    override fun onResume() {
        super.onResume()

        mediaSessionConnector.setPlayer(player)
        mediaSession.isActive = true

        view?.postDelayed(updateMetadataTask, METADATA_UPDATE_INTERVAL_MILLIS)

    }

    /**
     * Deactivates and removes callbacks from [MediaSessionCompat] since the [Player] instance is
     * destroyed in onStop and required metadata could be missing.
     */
    override fun onPause() {
        super.onPause()

        playerGlue.pause()
        mediaSession.isActive = false
        mediaSessionConnector.setPlayer(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }

    companion object {
        private val TAG = HostFragment::class.java.simpleName

        /** How often the player refreshes its views in milliseconds */
        private const val PLAYER_UPDATE_INTERVAL_MILLIS: Int = 100

        /** Time between metadata updates in milliseconds */
        private val METADATA_UPDATE_INTERVAL_MILLIS: Long =
            java.util.concurrent.TimeUnit.SECONDS.toMillis(10)

        /** Default time used when skipping playback in milliseconds */
        private val SKIP_PLAYBACK_MILLIS: Long = java.util.concurrent.TimeUnit.SECONDS.toMillis(10)

    }
}