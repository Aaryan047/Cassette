package com.example.cassettev01.model

data class Song(
    val id: String,
    val title: String,
    val uri: String
)

data class Playlist(
    val name: String,
    val songs: List<Song>
)