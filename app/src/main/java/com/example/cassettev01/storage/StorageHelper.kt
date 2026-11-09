package com.example.cassettev01.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.cassettev01.model.Playlist
import com.example.cassettev01.model.Song

object StorageHelper {
    private val audioExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma", "aiff", "ape")

    private fun isAudio(file: DocumentFile): Boolean {
        val mime = file.type.orEmpty()
        val extension = file.name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return mime.startsWith("audio/") || extension in audioExtensions
    }

    private fun toSong(file: DocumentFile): Song {
        val rawName = file.name ?: "Unknown track"
        val title = rawName.substringBeforeLast('.', rawName).ifBlank { rawName }
        return Song(id = file.uri.toString(), title = title, uri = file.uri.toString())
    }

    // Each track is listed under the folder it actually lives in.
    fun scanPlaylistsFromFolder(context: Context, rootUri: Uri): List<Playlist> {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw SecurityException("The selected music folder is no longer accessible. Choose it again.")
        if (!root.exists() || !root.isDirectory) {
            throw SecurityException("The selected music folder is no longer accessible. Choose it again.")
        }

        val groups = mutableListOf<Playlist>()
        fun visit(directory: DocumentFile, path: String, includeEmpty: Boolean = false) {
            val children = directory.listFiles().toList()
            val tracks = children.filter { it.isFile && isAudio(it) }
                .map(::toSong)
                .sortedBy { it.title.lowercase() }
            if (tracks.isNotEmpty() || includeEmpty) {
                groups += Playlist(name = path, songs = tracks)
            }
            children.filter { it.isDirectory }.sortedBy { it.name?.lowercase().orEmpty() }.forEach { child ->
                val childName = child.name ?: "Untitled folder"
                val childPath = if (path.isBlank()) childName else "$path / $childName"
                visit(child, childPath)
            }
        }
        visit(root, root.name?.takeIf { it.isNotBlank() } ?: "Music", includeEmpty = false)
        return groups
    }

    fun getSongsFromUri(context: Context, treeUri: Uri): List<Song> =
        scanPlaylistsFromFolder(context, treeUri).flatMap { it.songs }.distinctBy { it.id }

    fun takePersistableUriPermission(context: Context, uri: Uri) {
        // Read access is enough for playback; some providers reject a write grant.
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}
