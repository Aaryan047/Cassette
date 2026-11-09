package com.example.cassettev01.storage

import android.content.Context
import com.example.cassettev01.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SongCache {
    private const val CACHE_FILE = "cassette_library_cache.json"

    suspend fun loadCache(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, CACHE_FILE)
        if (!file.exists()) return@withContext emptyList()

        val songs = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(file.readText())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                songs.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        uri = obj.getString("uri")
                    )
                )
            }
        } catch (e: Exception) { e.printStackTrace() }

        return@withContext songs
    }

    suspend fun saveCache(context: Context, songs: List<Song>) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        songs.forEach { song ->
            val obj = JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("uri", song.uri)
            }
            jsonArray.put(obj)
        }
        File(context.cacheDir, CACHE_FILE).writeText(jsonArray.toString())
    }
}