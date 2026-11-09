package com.example.cassettev01.player

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.cassettev01.model.Playlist
import com.example.cassettev01.model.Song
import com.example.cassettev01.storage.SettingsManager
import com.example.cassettev01.storage.SongCache
import com.example.cassettev01.storage.StorageHelper
import com.example.cassettev01.ui.CassetteThemePalette
import com.example.cassettev01.ui.ThemeAppearance
import com.example.cassettev01.ui.WaveformStyle
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private var controller: MediaController? = null
    @Volatile private var fallbackDuration = 0L
    private var isUserSeeking = false
    private var seekResetJob: Job? = null
    private var shouldResumeAfterSeek = false
    private var pendingQueue: Pair<List<Song>, Int>? = null
    private var scanJob: Job? = null

    private val _currentTheme = MutableStateFlow(CassetteThemePalette.CARBON)
    val currentTheme: StateFlow<CassetteThemePalette> = _currentTheme
    private val _appearance = MutableStateFlow(ThemeAppearance.DARK)
    val appearance: StateFlow<ThemeAppearance> = _appearance
    private val _waveformStyle = MutableStateFlow(WaveformStyle.THICK_BARS)
    val waveformStyle: StateFlow<WaveformStyle> = _waveformStyle
    // Wait for saved settings so the tutorial does not flash on every launch.
    private val _tutorialCompleted = MutableStateFlow<Boolean?>(null)
    val tutorialCompleted: StateFlow<Boolean?> = _tutorialCompleted
    private val _isSeekable = MutableStateFlow(false)
    val isSeekable: StateFlow<Boolean> = _isSeekable
    private val _isSeeking = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _waveformLevels = MutableStateFlow(List(AudioVisualizerState.BAR_COUNT) { 0f })
    val waveformLevels: StateFlow<List<Float>> = _waveformLevels
    private val _currentSongTitle = MutableStateFlow("Nothing playing")
    val currentSongTitle: StateFlow<String> = _currentSongTitle
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration
    private val _loopMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val loopMode: StateFlow<Int> = _loopMode
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists
    private val _selectedFolderUri = MutableStateFlow<String?>(null)
    val selectedFolderUri: StateFlow<String?> = _selectedFolderUri
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    private val _libraryError = MutableStateFlow<String?>(null)
    val libraryError: StateFlow<String?> = _libraryError

    init {
        val app = getApplication<Application>()
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                controller = controllerFuture.get()
                setupListener()
                loadSavedPlayerSettings()
                startPositionTracker()
                pendingQueue?.let { (songs, index) ->
                    pendingQueue = null
                    playList(songs, index)
                }
            } catch (error: Exception) {
                _libraryError.value = "The playback service could not be started. Restart the app and try again."
            }
        }, MoreExecutors.directExecutor())

        viewModelScope.launch {
            val cachedSongs = SongCache.loadCache(app)
            if (cachedSongs.isNotEmpty() && _allSongs.value.isEmpty()) _allSongs.value = cachedSongs
        }
        viewModelScope.launch {
            SettingsManager.getTheme(app).collect { themeName ->
                _currentTheme.value = CassetteThemePalette.fromString(themeName)
            }
        }
        viewModelScope.launch {
            SettingsManager.getAppearance(app).collect { mode ->
                _appearance.value = runCatching { ThemeAppearance.valueOf(mode) }.getOrDefault(ThemeAppearance.DARK)
            }
        }
        viewModelScope.launch {
            SettingsManager.getWaveformStyle(app).collect { styleName ->
                _waveformStyle.value = WaveformStyle.fromString(styleName)
            }
        }
        viewModelScope.launch {
            SettingsManager.getTutorialCompleted(app).collect { completed ->
                _tutorialCompleted.value = completed
            }
        }
        viewModelScope.launch {
            SettingsManager.getFolderUri(app).collect { uriString ->
                _selectedFolderUri.value = uriString
                if (!uriString.isNullOrBlank()) scanFolderInBackground(uriString.toUri())
            }
        }
    }

    private fun loadSavedPlayerSettings() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val savedLoop = SettingsManager.getLoopMode(app).first()
            controller?.repeatMode = savedLoop
            _loopMode.value = savedLoop
            val savedShuffle = SettingsManager.getShuffle(app).first()
            controller?.shuffleModeEnabled = savedShuffle
            _shuffleEnabled.value = savedShuffle
        }
    }

    private fun setupListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (!isPlaying) AudioVisualizerState.clear()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _loopMode.value = repeatMode
                viewModelScope.launch { SettingsManager.saveLoopMode(getApplication(), repeatMode) }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
                viewModelScope.launch { SettingsManager.saveShuffle(getApplication(), shuffleModeEnabled) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                AudioVisualizerState.clear()
                isUserSeeking = false
                _isSeeking.value = false
                seekResetJob?.cancel()
                seekResetJob = null
                shouldResumeAfterSeek = false
                _waveformLevels.value = List(AudioVisualizerState.BAR_COUNT) { 0f }
                _currentSongTitle.value = mediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing"
                fallbackDuration = 0L
                _currentPosition.value = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
                _duration.value = 0L
                _isSeekable.value = controller?.isCurrentMediaItemSeekable == true
                mediaItem?.localConfiguration?.uri?.let { uri ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val metadata = android.media.MediaMetadataRetriever()
                        try {
                            metadata.setDataSource(getApplication(), uri)
                            fallbackDuration = metadata.extractMetadata(
                                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                            )?.toLongOrNull() ?: 0L
                        } catch (_: Exception) {
                            fallbackDuration = 0L
                        } finally {
                            try { metadata.release() } catch (_: Exception) { }
                        }
                    }
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK ||
                    reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                    _currentPosition.value = newPosition.positionMs.coerceAtLeast(0L)
                    completeSeeking()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isUserSeeking = false
                _isSeeking.value = false
                shouldResumeAfterSeek = false
                seekResetJob?.cancel()
                AudioVisualizerState.clear()
                _waveformLevels.value = List(AudioVisualizerState.BAR_COUNT) { 0f }
                _currentSongTitle.value = "Unable to play this track"
                _isPlaying.value = false
                val detail = error.cause?.message?.takeIf { it.isNotBlank() }
                _libraryError.value = "Playback failed (${error.errorCodeName}). ${detail ?: "Check that the file is accessible and uses a supported audio format."}"
            }

            override fun onEvents(player: Player, events: Player.Events) {
                _isSeekable.value = player.isCurrentMediaItemSeekable
                _isPlaying.value = player.isPlaying
                if (player.mediaItemCount == 0) _currentSongTitle.value = "Nothing playing"
            }
        })
    }

    private fun startPositionTracker() {
        viewModelScope.launch {
            var lastPositionUpdate = 0L
            while (true) {
                val player = controller
                if (player != null) {
                    val now = SystemClock.elapsedRealtime()
                    if (!isUserSeeking && now - lastPositionUpdate >= 300L) {
                        _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                        val mediaDuration = player.duration
                        _duration.value = if (mediaDuration > 0L) mediaDuration else fallbackDuration
                        _isSeekable.value = player.isCurrentMediaItemSeekable
                        _isPlaying.value = player.isPlaying
                        lastPositionUpdate = now
                    }

                    if (isUserSeeking || !player.isPlaying) {
                        _waveformLevels.value = List(AudioVisualizerState.BAR_COUNT) { 0f }
                    } else {
                        // These are fixed FFT bands; only their heights change with the music.
                        _waveformLevels.value = if (AudioVisualizerState.hasLiveSamples.value) {
                            AudioVisualizerState.levels.value
                        } else {
                            List(AudioVisualizerState.BAR_COUNT) { 0f }
                        }
                    }
                }
                // Poll often enough to keep the deck animation feeling smooth.
                delay(16L)
            }
        }
    }

    private fun completeSeeking() {
        if (!_isSeeking.value) return
        isUserSeeking = false
        _isSeeking.value = false
        seekResetJob?.cancel()
        seekResetJob = null
        AudioVisualizerState.clearToSilence()
        _waveformLevels.value = List(AudioVisualizerState.BAR_COUNT) { 0f }
        val resume = shouldResumeAfterSeek
        shouldResumeAfterSeek = false
        if (resume) controller?.play()
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                StorageHelper.takePersistableUriPermission(app, uri)
                SettingsManager.saveFolderUri(app, uri.toString())
                _libraryError.value = null
            } catch (error: Exception) {
                _libraryError.value = "Cassette couldn't keep access to that folder. Please select it again. (${error.message ?: "permission error"})"
            }
        }
    }

    fun refreshLibrary() {
        _selectedFolderUri.value?.let { scanFolderInBackground(it.toUri()) }
    }

    private fun scanFolderInBackground(uri: Uri) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _libraryError.value = null
            try {
                val scanned = StorageHelper.scanPlaylistsFromFolder(getApplication(), uri)
                val songs = scanned.flatMap { it.songs }.distinctBy { it.id }
                withContext(Dispatchers.Main) {
                    _playlists.value = scanned
                    _allSongs.value = songs
                    _isScanning.value = false
                }
                SongCache.saveCache(getApplication(), songs)
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    _isScanning.value = false
                    _libraryError.value = error.message ?: "The selected folder could not be scanned. Check folder access in Android settings."
                }
            }
        }
    }

    fun playList(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val safeIndex = startIndex.coerceIn(songs.indices)
        val player = controller
        if (player == null) {
            pendingQueue = songs to safeIndex
            return
        }

        // Some folder providers are slow, so let Media3 open each track when it is needed.
        val items = songs.map { song ->
            MediaItem.Builder()
                .setUri(Uri.parse(song.uri))
                .setMediaId(song.id)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).build())
                .build()
        }

        _libraryError.value = null
        _currentSongTitle.value = songs[safeIndex].title
        isUserSeeking = false
        _isSeeking.value = false
        shouldResumeAfterSeek = false
        seekResetJob?.cancel()
        _waveformLevels.value = List(AudioVisualizerState.BAR_COUNT) { 0f }
        AudioVisualizerState.clear()

        // Media3 opens the queue asynchronously and reports failures through the player listener.
        player.setMediaItems(items, safeIndex, 0L)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }

    fun beginSeeking() {
        if (_isSeeking.value) return
        val player = controller
        shouldResumeAfterSeek = player?.playWhenReady == true
        isUserSeeking = true
        _isSeeking.value = true
        AudioVisualizerState.clearToSilence()
        _waveformLevels.value = List(AudioVisualizerState.BAR_COUNT) { 0f }
        if (shouldResumeAfterSeek) player?.pause()
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: run {
            shouldResumeAfterSeek = false
            isUserSeeking = false
            _isSeeking.value = false
            return
        }
        if (!player.isCurrentMediaItemSeekable) {
            completeSeeking()
            return
        }
        beginSeeking()
        val safePosition = positionMs.coerceIn(0L, _duration.value.takeIf { it > 0L } ?: Long.MAX_VALUE)
        _currentPosition.value = safePosition

        // Some providers report a seek immediately; others take a bit longer.
        seekResetJob?.cancel()
        seekResetJob = viewModelScope.launch {
            delay(2200L)
            completeSeeking()
        }
        player.seekTo(safePosition)
    }

    fun toggleLoopMode() {
        val nextMode = when (controller?.repeatMode ?: Player.REPEAT_MODE_OFF) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = nextMode
        _loopMode.value = nextMode
    }

    fun toggleShuffle() {
        val enabled = !(controller?.shuffleModeEnabled ?: _shuffleEnabled.value)
        controller?.shuffleModeEnabled = enabled
        _shuffleEnabled.value = enabled
    }

    fun setTheme(theme: CassetteThemePalette) {
        _currentTheme.value = theme
        viewModelScope.launch { SettingsManager.saveTheme(getApplication(), theme.name) }
    }

    fun setAppearance(appearance: ThemeAppearance) {
        _appearance.value = appearance
        viewModelScope.launch { SettingsManager.saveAppearance(getApplication(), appearance.name) }
    }

    fun setWaveformStyle(style: WaveformStyle) {
        _waveformStyle.value = style
        viewModelScope.launch { SettingsManager.saveWaveformStyle(getApplication(), style.name) }
    }

    fun completeTutorial() {
        _tutorialCompleted.value = true
        viewModelScope.launch { SettingsManager.saveTutorialCompleted(getApplication(), true) }
    }
}
