package com.example.cassettev01.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import com.example.cassettev01.model.Playlist
import com.example.cassettev01.model.Song
import com.example.cassettev01.player.PlayerViewModel

private enum class Destination(val label: String) {
    LIBRARY("Library"),
    FOLDERS("Folders"),
    DECK("Cassette"),
    SETTINGS("Settings")
}

@Composable
fun HomeScreen(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val allSongs by viewModel.allSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentTitle by viewModel.currentSongTitle.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val libraryError by viewModel.libraryError.collectAsState()
    val tutorialCompleted by viewModel.tutorialCompleted.collectAsState()
    var showTutorial by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf(Destination.LIBRARY) }

    LaunchedEffect(tutorialCompleted) {
        if (tutorialCompleted == false) showTutorial = true
    }

    val activity = context as? Activity
    DisposableEffect(destination, activity) {
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity?.window
        if (destination == Destination.DECK && activity != null && window != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (activity != null && window != null) {
                activity.requestedOrientation = previousOrientation
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let(viewModel::onFolderSelected)
    }
    val chooseFolder = { folderLauncher.launch(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (destination != Destination.DECK) Column {
                if (currentTitle != "Nothing playing") {
                    MiniPlayer(
                        title = currentTitle,
                        isPlaying = isPlaying,
                        onPlayPause = viewModel::togglePlayPause,
                        onOpenDeck = { destination = Destination.DECK }
                    )
                }
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 0.dp
                ) {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = {
                                Box(
                                    Modifier.size(width = 22.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(if (destination == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                )
                            },
                            label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (destination) {
                Destination.LIBRARY -> LibraryScreen(
                    songs = allSongs,
                    currentTitle = currentTitle,
                    onChooseFolder = chooseFolder,
                    onPlaySong = { songList, index -> viewModel.playList(songList, index) },
                    isScanning = isScanning,
                    error = libraryError
                )
                Destination.FOLDERS -> FoldersScreen(
                    playlists = playlists,
                    onChooseFolder = chooseFolder,
                    onPlaySong = { songList, index -> viewModel.playList(songList, index) }
                )
                Destination.DECK -> CassetteDeckScreen(
                    viewModel = viewModel,
                    onEject = { destination = Destination.LIBRARY }
                )
                Destination.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    songCount = allSongs.size,
                    onChooseFolder = chooseFolder,
                    onReplayTutorial = { showTutorial = true }
                )
            }
        }
    }

    if (showTutorial) {
        FirstLaunchTutorial(
            onFinish = {
                showTutorial = false
                viewModel.completeTutorial()
            }
        )
    }
}

@Composable
private fun PageHeader(
    eyebrow: String,
    title: String,
    centerBrand: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp)) {
        if (centerBrand) {
            Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(Modifier.size(25.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CASSETTE", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.2.sp)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(Modifier.size(27.dp))
                Spacer(Modifier.width(9.dp))
                Text("CASSETTE", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
            }
        }
        Spacer(Modifier.height(17.dp))
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f), letterSpacing = 1.8.sp)
        Text(title, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text("C", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
    }
}

@Composable
private fun LibraryScreen(
    songs: List<Song>,
    currentTitle: String,
    onChooseFolder: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit,
    isScanning: Boolean,
    error: String?
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter { it.title.contains(query.trim(), ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(eyebrow = "Your collection", title = "Music library", centerBrand = true)
        SearchField(query = query, onQueryChange = { query = it }, placeholder = "Search your music")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("${songs.size} TRACKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), letterSpacing = 1.4.sp)
                Text(if (query.isBlank()) "Ready when you are." else "${filtered.size} matching tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onChooseFolder, shape = RoundedCornerShape(12.dp)) {
                Text("Add music", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (isScanning) {
            Text("Scanning folders…", modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        if (!error.isNullOrBlank()) {
            Text(error, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        if (filtered.isEmpty()) {
            EmptyLibrary(onChooseFolder = onChooseFolder, searching = query.isNotBlank())
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        song = song,
                        index = index,
                        highlighted = currentTitle == song.title,
                        onClick = { onPlaySong(filtered, index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { SearchMark(Modifier.size(19.dp)) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun EmptyLibrary(onChooseFolder: () -> Unit, searching: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(78.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { TrackMark(Modifier.size(31.dp), MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(18.dp))
        Text(if (searching) "No matches" else "Your music belongs here.", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(7.dp))
        Text(
            if (searching) "Try a different search term." else "Choose a music folder to build your local library. Cassette reads the files you select; nothing is uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!searching) {
            Spacer(Modifier.height(18.dp))
            Button(onClick = onChooseFolder, shape = RoundedCornerShape(12.dp)) { Text("Choose music folder") }
        }
    }
}

@Composable
private fun SongRow(song: Song, index: Int, highlighted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(39.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (highlighted) TrackMark(Modifier.size(17.dp), MaterialTheme.colorScheme.primary)
            else Text(String.format("%02d", index + 1), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text("LOCAL AUDIO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
        }
        if (highlighted) {
            Text("PLAYING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
        } else {
            PlayPauseMark(Modifier.size(15.dp), MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), playing = false)
        }
    }
}

@Composable
private fun FoldersScreen(
    playlists: List<Playlist>,
    onChooseFolder: () -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit
) {
    var expandedFolder by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val filteredFolders = remember(playlists, query) {
        if (query.isBlank()) playlists else playlists.mapNotNull { folder ->
            val matches = folder.songs.filter { it.title.contains(query.trim(), ignoreCase = true) }
            if (matches.isEmpty()) null else folder.copy(songs = matches)
        }
    }
    val filteredCount = filteredFolders.sumOf { it.songs.size }

    Column(Modifier.fillMaxSize()) {
        PageHeader(eyebrow = "Browse by location", title = "Folders")
        SearchField(query = query, onQueryChange = { query = it }, placeholder = "Search songs in your folders")
        if (playlists.isEmpty()) {
            EmptyLibrary(onChooseFolder = onChooseFolder, searching = false)
        } else {
            Text(
                if (query.isBlank()) "${playlists.size} FOLDERS · ${playlists.sumOf { it.songs.size }} TRACKS" else "$filteredCount MATCHING TRACKS IN ${filteredFolders.size} FOLDERS",
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                letterSpacing = 1.sp
            )
            if (filteredFolders.isEmpty()) {
                Text("No songs match that search.", modifier = Modifier.fillMaxWidth().padding(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFolders, key = { it.name }) { folder ->
                        val expanded = query.isNotBlank() || expandedFolder == folder.name
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        if (query.isBlank()) expandedFolder = if (expanded) null else folder.name
                                    }.padding(15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        FolderMark(Modifier.size(23.dp), MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${folder.songs.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                }
                                AnimatedVisibility(visible = expanded) {
                                    Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                                        folder.songs.forEachIndexed { index, song ->
                                            Row(
                                                Modifier.fillMaxWidth().clickable { onPlaySong(folder.songs, index) }.padding(vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TrackMark(Modifier.size(15.dp), MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(10.dp))
                                                Text(song.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                                                PlayPauseMark(Modifier.size(14.dp), MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), playing = false)
                                            }
                                            if (index != folder.songs.lastIndex) {
                                                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CassetteDeckScreen(
    viewModel: PlayerViewModel,
    onEject: () -> Unit
) {
    val title by viewModel.currentSongTitle.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val seekable by viewModel.isSeekable.collectAsState()
    val isSeeking by viewModel.isSeeking.collectAsState()
    val repeatMode by viewModel.loopMode.collectAsState()
    val shuffle by viewModel.shuffleEnabled.collectAsState()
    val waveformStyle by viewModel.waveformStyle.collectAsState()
    val playbackError by viewModel.libraryError.collectAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "cassette-reels")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4200, easing = LinearEasing)),
        label = "reel-rotation"
    )
    val safeDuration = duration.coerceAtLeast(0L)
    val sliderMax = safeDuration.toFloat().coerceAtLeast(1f)
    val sliderValue = if (dragging) dragValue else position.toFloat().coerceIn(0f, sliderMax)
    val repeatLabel = "REPEAT"
    val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) DeckIcon.REPEAT_ONE else DeckIcon.REPEAT
    val repeatMark = when (repeatMode) {
        Player.REPEAT_MODE_ALL -> "ALL"
        Player.REPEAT_MODE_ONE -> "1"
        else -> ""
    }
    val indexedWaveform by viewModel.waveformLevels.collectAsState()
    val waveform = if (isSeeking || !isPlaying) List(indexedWaveform.size) { 0f } else indexedWaveform
    val reelRotation = if (isPlaying && !isSeeking) rotation else 0f

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(horizontal = 25.dp, vertical = 17.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
            shadowElevation = 3.dp
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("C-60  /  SIDE A", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.8.sp)
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onEject,
                        modifier = Modifier.height(29.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(9.dp)
                    ) {
                        Text("EJECT", style = MaterialTheme.typography.labelMedium, letterSpacing = 1.2.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Reel(Modifier.size(76.dp), reelRotation)
                        // Keep the visualizer in the middle so it does not cover the reel hubs.
                        Box(
                            Modifier.weight(1f).fillMaxHeight().padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            AudioWaveform(waveform, waveformStyle, Modifier.fillMaxSize())
                        }
                        Reel(Modifier.size(76.dp), reelRotation)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DeckControl("SHUFFLE", DeckIcon.SHUFFLE, shuffle, onClick = viewModel::toggleShuffle, modifier = Modifier.weight(1f))
                    DeckControl("PREVIOUS", DeckIcon.PREVIOUS, false, onClick = viewModel::previous, modifier = Modifier.weight(1f))
                    DeckControl("", if (isPlaying) DeckIcon.PAUSE else DeckIcon.PLAY, true, onClick = viewModel::togglePlayPause, modifier = Modifier.weight(1f), prominent = true)
                    DeckControl("NEXT", DeckIcon.NEXT, false, onClick = viewModel::next, modifier = Modifier.weight(1f))
                    DeckControl(repeatLabel, repeatIcon, repeatMode != Player.REPEAT_MODE_OFF, onClick = viewModel::toggleLoopMode, modifier = Modifier.weight(1f), mark = repeatMark)
                }

                Text(
                    title,
                    modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!playbackError.isNullOrBlank()) {
                    Text(playbackError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { value ->
                        if (!dragging) {
                            dragValue = sliderValue
                            viewModel.beginSeeking()
                        }
                        dragging = true
                        dragValue = value
                    },
                    onValueChangeFinished = {
                        viewModel.seekTo(dragValue.toLong())
                        dragging = false
                    },
                    valueRange = 0f..sliderMax,
                    enabled = seekable && safeDuration > 0L,
                    modifier = Modifier.fillMaxWidth().height(27.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    )
                )
                Row(Modifier.fillMaxWidth().padding(bottom = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(if (dragging) sliderValue.toLong() else position), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(duration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (title == "Nothing playing") {
                    Text("Choose a music folder from Library to load tracks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Reel(modifier: Modifier = Modifier, rotation: Float) {
    val colors = MaterialTheme.colorScheme
    val body = colors.surfaceVariant
    val rim = colors.primary.copy(alpha = 0.78f)
    val spoke = colors.onSurface.copy(alpha = 0.76f)
    val hub = colors.surface
    Canvas(modifier = modifier.rotate(rotation)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.46f
        drawCircle(body, radius = radius, center = center)
        drawCircle(rim, radius = radius * 0.88f, center = center, style = Stroke(width = size.minDimension * 0.045f))
        drawCircle(colors.outline.copy(alpha = 0.56f), radius = radius * 0.79f, center = center, style = Stroke(width = size.minDimension * 0.025f))
        repeat(8) { index ->
            rotate(index * 45f, pivot = center) {
                drawLine(
                    spoke,
                    Offset(center.x, center.y - radius * 0.72f),
                    Offset(center.x, center.y - radius * 0.29f),
                    strokeWidth = size.minDimension * 0.05f,
                    cap = StrokeCap.Round
                )
            }
        }
        drawCircle(hub, radius = radius * 0.29f, center = center)
        drawCircle(rim, radius = radius * 0.18f, center = center)
        drawCircle(colors.onSurface.copy(alpha = 0.86f), radius = radius * 0.075f, center = center)
    }
}

@Composable
private fun AudioWaveform(
    levels: List<Float>,
    style: WaveformStyle,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
    Canvas(modifier = modifier) {
        val count = levels.size.coerceAtLeast(1)
        val centerY = size.height / 2f
        val maxAmplitude = size.height * 0.44f
        val usableLevels = if (levels.isEmpty()) listOf(0f) else levels
        val normalized = FloatArray(count) { index -> usableLevels[index.coerceAtMost(usableLevels.lastIndex)].coerceIn(0f, 1f) }

        drawLine(
            color = track,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx()
        )

        when (style) {
            WaveformStyle.THICK_BARS, WaveformStyle.THIN_BARS -> {
                val isThin = style == WaveformStyle.THIN_BARS
                val slotWidth = size.width / count
                val strokeWidth = if (isThin) (slotWidth * 0.28f).coerceAtLeast(1.1.dp.toPx())
                    else (slotWidth * 0.68f).coerceAtMost(5.2.dp.toPx())
                val cap = StrokeCap.Round
                for (index in 0 until count) {
                    val level = normalized[index]
                    if (level <= 0.008f) continue
                    val amplitude = max(size.height * 0.025f, level * maxAmplitude)
                    val x = slotWidth * (index + 0.5f)
                    drawLine(
                        color = accent.copy(alpha = 0.48f + level * 0.5f),
                        start = Offset(x, centerY - amplitude),
                        end = Offset(x, centerY + amplitude),
                        strokeWidth = strokeWidth,
                        cap = cap
                    )
                }
            }

            WaveformStyle.SQUIGGLE -> {
                // The line stays in place; the music changes its height.
                val path = Path()
                val samples = (count * 8).coerceAtLeast(160)
                val cycles = 18.0
                for (sample in 0..samples) {
                    val xFraction = sample.toFloat() / samples
                    val scaled = xFraction * (count - 1)
                    val lower = floor(scaled).toInt().coerceIn(0, count - 1)
                    val upper = (lower + 1).coerceAtMost(count - 1)
                    val fraction = scaled - lower
                    val level = normalized[lower] + (normalized[upper] - normalized[lower]) * fraction
                    val envelope = size.height * (level * 0.43f)
                    val phase = xFraction * cycles * 2.0 * Math.PI
                    val y = centerY + (sin(phase).toFloat() * envelope)
                    if (sample == 0) path.moveTo(xFraction * size.width, y)
                    else path.lineTo(xFraction * size.width, y)
                }
                drawPath(
                    path = path,
                    color = accent,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
            }

            WaveformStyle.MIRROR -> {
                val path = Path()
                val slotWidth = size.width / (count - 1).coerceAtLeast(1)
                path.moveTo(0f, centerY)
                for (index in 0 until count) {
                    val amplitude = normalized[index] * maxAmplitude
                    path.lineTo(index * slotWidth, centerY - amplitude)
                }
                for (index in count - 1 downTo 0) {
                    val amplitude = normalized[index] * maxAmplitude
                    path.lineTo(index * slotWidth, centerY + amplitude)
                }
                path.close()
                drawPath(path, accent.copy(alpha = 0.30f))
                drawPath(path, accent.copy(alpha = 0.9f), style = Stroke(width = 1.4.dp.toPx()))
            }

            WaveformStyle.DOTS -> {
                val slotWidth = size.width / count
                for (index in 0 until count) {
                    val level = normalized[index]
                    val amplitude = level * maxAmplitude
                    val x = slotWidth * (index + 0.5f)
                    if (level <= 0.008f) continue
                    val radius = (1.25.dp.toPx() + level * 2.2.dp.toPx()).coerceAtMost(3.8.dp.toPx())
                    drawCircle(accent.copy(alpha = 0.92f), radius, Offset(x, centerY - amplitude))
                    drawCircle(accent.copy(alpha = 0.54f), radius, Offset(x, centerY + amplitude))
                }
            }
        }
    }
}

private enum class DeckIcon { SHUFFLE, PREVIOUS, PLAY, PAUSE, NEXT, REPEAT, REPEAT_ONE }

@Composable
private fun DeckControl(
    label: String,
    icon: DeckIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    mark: String = ""
) {
    val foreground = if (prominent) MaterialTheme.colorScheme.onPrimary else if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (prominent) MaterialTheme.colorScheme.primary else if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    Column(modifier = modifier.clickable(onClick = onClick).padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(width = if (prominent) 58.dp else 48.dp, height = if (prominent) 48.dp else 42.dp)
                .clip(if (prominent) RoundedCornerShape(13.dp) else RoundedCornerShape(11.dp))
                .background(background)
                .border(1.dp, if (selected && !prominent) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f) else Color.Transparent, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(if (prominent) 23.dp else 20.dp)) { drawDeckIcon(icon, foreground) }
            if (mark.isNotEmpty() && (icon == DeckIcon.REPEAT || icon == DeckIcon.REPEAT_ONE)) {
                Text(
                    text = mark,
                    modifier = Modifier.align(Alignment.Center),
                    color = foreground,
                    fontSize = if (mark == "ALL") 5.5.sp else 7.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = if (mark == "ALL") (-0.35).sp else 0.sp,
                    lineHeight = 7.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (label.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontSize = 8.sp, maxLines = 1)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDeckIcon(icon: DeckIcon, color: Color) {
    val w = size.width
    val h = size.height
    val stroke = (size.minDimension * 0.09f).coerceAtLeast(1.5f)
    when (icon) {
        DeckIcon.PLAY -> {
            val path = Path().apply { moveTo(w * 0.27f, h * 0.14f); lineTo(w * 0.78f, h * 0.5f); lineTo(w * 0.27f, h * 0.86f); close() }
            drawPath(path, color)
        }
        DeckIcon.PAUSE -> {
            drawRoundRect(color, Offset(w * 0.22f, h * 0.14f), androidx.compose.ui.geometry.Size(w * 0.19f, h * 0.72f), CornerRadius(stroke, stroke))
            drawRoundRect(color, Offset(w * 0.59f, h * 0.14f), androidx.compose.ui.geometry.Size(w * 0.19f, h * 0.72f), CornerRadius(stroke, stroke))
        }
        DeckIcon.PREVIOUS, DeckIcon.NEXT -> {
            val left = icon == DeckIcon.PREVIOUS
            val path = Path().apply {
                if (left) { moveTo(w * 0.73f, h * 0.15f); lineTo(w * 0.28f, h * 0.5f); lineTo(w * 0.73f, h * 0.85f) }
                else { moveTo(w * 0.27f, h * 0.15f); lineTo(w * 0.72f, h * 0.5f); lineTo(w * 0.27f, h * 0.85f) }
            }
            drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
            val x = if (left) w * 0.16f else w * 0.84f
            drawLine(color, Offset(x, h * 0.15f), Offset(x, h * 0.85f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        DeckIcon.SHUFFLE -> {
            drawLine(color, Offset(w * 0.15f, h * 0.25f), Offset(w * 0.42f, h * 0.25f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.42f, h * 0.25f), Offset(w * 0.72f, h * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.15f, h * 0.75f), Offset(w * 0.36f, h * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.36f, h * 0.75f), Offset(w * 0.72f, h * 0.25f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.61f, h * 0.25f), Offset(w * 0.79f, h * 0.25f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.79f, h * 0.25f), Offset(w * 0.66f, h * 0.12f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.79f, h * 0.25f), Offset(w * 0.66f, h * 0.38f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.61f, h * 0.75f), Offset(w * 0.79f, h * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.79f, h * 0.75f), Offset(w * 0.66f, h * 0.62f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(w * 0.79f, h * 0.75f), Offset(w * 0.66f, h * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        DeckIcon.REPEAT, DeckIcon.REPEAT_ONE -> {
            val arrows = Path().apply {
                moveTo(w * 0.12f, h * 0.18f)
                lineTo(w * 0.88f, h * 0.18f)
                moveTo(w * 0.88f, h * 0.18f)
                lineTo(w * 0.74f, h * 0.04f)
                moveTo(w * 0.88f, h * 0.18f)
                lineTo(w * 0.74f, h * 0.32f)

                moveTo(w * 0.88f, h * 0.82f)
                lineTo(w * 0.12f, h * 0.82f)
                moveTo(w * 0.12f, h * 0.82f)
                lineTo(w * 0.26f, h * 0.68f)
                moveTo(w * 0.12f, h * 0.82f)
                lineTo(w * 0.26f, h * 0.96f)
            }
            drawPath(arrows, color, style = Stroke(width = stroke * 1.12f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

@Composable
private fun MiniPlayer(title: String, isPlaying: Boolean, onPlayPause: () -> Unit, onOpenDeck: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenDeck).padding(horizontal = 17.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark(Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text("NOW PLAYING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            }
            IconButton(onClick = onPlayPause) {
                PlayPauseMark(Modifier.size(19.dp), MaterialTheme.colorScheme.primary, playing = isPlaying)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    viewModel: PlayerViewModel,
    songCount: Int,
    onChooseFolder: () -> Unit,
    onReplayTutorial: () -> Unit
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val folderUri by viewModel.selectedFolderUri.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val waveformStyle by viewModel.waveformStyle.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader(eyebrow = "Make it yours", title = "Settings")
        Text("APPEARANCE", modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), letterSpacing = 1.6.sp)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeAppearance.entries.forEach { mode ->
                val selected = appearance == mode
                OutlinedButton(
                    onClick = { viewModel.setAppearance(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                ) {
                    Text(if (mode == ThemeAppearance.DARK) "DARK" else "LIGHT", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("COLOURWAYS", modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), letterSpacing = 1.6.sp)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            CassetteThemePalette.entries.forEach { palette ->
                val selected = currentTheme == palette
                val accentPreview = if (appearance == ThemeAppearance.DARK) palette.darkAccent else palette.lightAccent
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                        .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.58f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(15.dp))
                        .clickable { viewModel.setTheme(palette) }.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewBackground = if (appearance == ThemeAppearance.DARK) palette.bg else Color(0xFFF7F7F4)
                    Box(Modifier.size(38.dp).clip(CircleShape).background(previewBackground).border(1.dp, accentPreview.copy(alpha = 0.68f), CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(13.dp).clip(CircleShape).background(accentPreview))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(palette.title, style = MaterialTheme.typography.titleMedium)
                        Text(if (selected) "CURRENT THEME" else "Tap to apply", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.9.sp)
                    }
                    if (selected) Text("SELECTED", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("DECK WAVEFORM", modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), letterSpacing = 1.6.sp)
        Text(
            "Choose how the music moves on the cassette deck.",
            modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val waveformPreview = remember { listOf(0.10f, 0.24f, 0.48f, 0.82f, 0.58f, 0.32f, 0.68f, 0.92f, 0.46f, 0.18f, 0.38f, 0.74f, 0.52f, 0.22f, 0.12f, 0.40f) }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WaveformStyle.entries.chunked(2).forEach { rowStyles ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowStyles.forEach { style ->
                        val selected = waveformStyle == style
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                                .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(13.dp)
                                )
                                .clickable { viewModel.setWaveformStyle(style) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AudioWaveform(waveformPreview, style, Modifier.fillMaxWidth().height(34.dp))
                            Spacer(Modifier.height(5.dp))
                            Text(style.title, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Text(if (selected) "SELECTED" else style.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (rowStyles.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("MUSIC LIBRARY", modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), letterSpacing = 1.6.sp)
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Local collection", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text("$songCount indexed tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (folderUri.isNullOrBlank()) "No folder selected" else "A folder is connected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onChooseFolder, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp)) { Text("Change folder") }
                    OutlinedButton(onClick = viewModel::refreshLibrary, modifier = Modifier.weight(1f), enabled = !folderUri.isNullOrBlank() && !isScanning, shape = RoundedCornerShape(11.dp)) { Text(if (isScanning) "Scanning…" else "Rescan") }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("APP GUIDE", modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), letterSpacing = 1.6.sp)
        OutlinedButton(onClick = onReplayTutorial, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Take the tutorial again")
        }
        Spacer(Modifier.height(22.dp))
        Text("ABOUT CASSETTE", modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), letterSpacing = 1.6.sp)
        Column(Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
            Text("Music you already own. A player that stays out of the way.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(7.dp))
            Text("Cassette is an offline-first local music player. Playback is handled on-device through Android Media3. Your selected music folder is read locally; there are no accounts or streaming dependencies.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Text("CASSETTE  ·  VERSION 1.0.1", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f), letterSpacing = 1.3.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text("Made with <3, AVK47", modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun FirstLaunchTutorial(onFinish: () -> Unit) {
    val pages = listOf(
        "Welcome to Cassette" to "A local music player for the tracks stored on your device. Your music stays on your device; Cassette does not stream or upload it.",
        "Choose your music folder" to "Open Library and tap Add music. Select the folder where your audio files are stored and grant access. Cassette indexes supported audio files inside that folder and its subfolders.",
        "Browse and find tracks" to "Use Library for one searchable track list. Use Folders to keep the folder structure and search for tracks within your selected collection.",
        "Play and make it yours" to "Tap a track to start playback. Open the Cassette deck from the mini-player or bottom navigation. Use the deck controls and seek bar; tap Eject to return. Themes and light or dark appearance live in Settings."
    )
    var page by remember { mutableStateOf(0) }
    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 10.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("GETTING STARTED", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.7.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${page + 1} / ${pages.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(24.dp))
                Text(pages[page].first, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(10.dp))
                Text(pages[page].second, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    pages.indices.forEach { index ->
                        Box(
                            Modifier.weight(1f).height(4.dp).clip(CircleShape)
                                .background(if (index <= page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (page == 0) {
                        Text("Skip", modifier = Modifier.clickable(onClick = onFinish).padding(10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text("Back", modifier = Modifier.clickable { page-- }.padding(10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { if (page == pages.lastIndex) onFinish() else page++ }, shape = RoundedCornerShape(12.dp)) {
                        Text(if (page == pages.lastIndex) "Finish" else "Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchMark(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        val radius = size.minDimension * 0.30f
        val center = Offset(size.width * 0.42f, size.height * 0.40f)
        drawCircle(accent, radius, center, style = Stroke(width = stroke))
        drawLine(accent, Offset(size.width * 0.63f, size.height * 0.63f), Offset(size.width * 0.88f, size.height * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun TrackMark(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val heights = listOf(0.34f, 0.72f, 0.48f, 0.9f, 0.55f)
        val gap = size.width * 0.11f
        val barWidth = (size.width - gap * 4f) / 5f
        heights.forEachIndexed { index, fraction ->
            val height = size.height * fraction
            drawRoundRect(color, Offset(index * (barWidth + gap), (size.height - height) / 2f), androidx.compose.ui.geometry.Size(barWidth, height), CornerRadius(barWidth, barWidth))
        }
    }
}

@Composable
private fun PlayPauseMark(modifier: Modifier = Modifier, color: Color, playing: Boolean) {
    Canvas(modifier) {
        if (playing) {
            drawRoundRect(color, Offset(size.width * 0.22f, size.height * 0.12f), androidx.compose.ui.geometry.Size(size.width * 0.18f, size.height * 0.76f), CornerRadius(2f, 2f))
            drawRoundRect(color, Offset(size.width * 0.60f, size.height * 0.12f), androidx.compose.ui.geometry.Size(size.width * 0.18f, size.height * 0.76f), CornerRadius(2f, 2f))
        } else {
            val path = Path().apply { moveTo(size.width * 0.25f, size.height * 0.10f); lineTo(size.width * 0.82f, size.height * 0.5f); lineTo(size.width * 0.25f, size.height * 0.90f); close() }
            drawPath(path, color)
        }
    }
}

@Composable
private fun FolderMark(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.075f
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.30f)
            lineTo(size.width * 0.43f, size.height * 0.30f)
            lineTo(size.width * 0.54f, size.height * 0.43f)
            lineTo(size.width * 0.88f, size.height * 0.43f)
            lineTo(size.width * 0.88f, size.height * 0.82f)
            lineTo(size.width * 0.12f, size.height * 0.82f)
            close()
        }
        drawPath(path, color.copy(alpha = 0.12f))
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

private fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0L) return "0:00"
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
