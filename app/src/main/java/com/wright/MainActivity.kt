package com.wright

import android.Manifest.permission.READ_MEDIA_AUDIO
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.wright.ui.theme.WrightMusicPlayerTheme

class MainActivity : ComponentActivity() {
    private val READ_STORAGE_PERMISSION_CODE = 101

    data class AudioFile(
        val id: Long,
        val title: String,
        val artist: String?,
        val album: String?,
        val duration: Long,
        val dataPath: String, // Path to the file
        val contentUri: Uri // Uri to access the file
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to find media files
                findMediaFiles()
            } else {
                // Permission denied
                // Handle the case where the user denies permission
                // (e.g., show a message, disable functionality)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val mediaFiles = getMediaFiles(applicationContext)
//
//        mediaFiles.forEach { mediaFile -> println(mediaFile) }

        if (checkAndRequestPermissions()) {
            findMediaFiles()
        }

        // https://developer.android.com/media/implement/playback-app
        val player = ExoPlayer.Builder(applicationContext).build()

        enableEdgeToEdge()
        setContent {
            WrightMusicPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionToRequest = READ_MEDIA_AUDIO

        return if (ContextCompat.checkSelfPermission(this, permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permissionToRequest), READ_STORAGE_PERMISSION_CODE)
            false
        }
    }

    @OptIn(UnstableApi::class)
    private fun findMediaFiles() {
        val mediaFiles = mutableListOf<AudioFile>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA // Path to the file, use with caution on Android 10+ for direct file access
        )

        // Filter for media files specifically by MIME type or file extension
        // Using MediaStore.Audio.Media.IS_MUSIC is a good general filter for music files.
        // For specifically media, you might filter by MIME type or DATA path extension.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("audio/mpeg") // MIME type for media

        // Or, to filter by file extension (less reliable than MIME type):
        // val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
        // val selectionArgs = arrayOf("%.media")

        // Sorting order (optional)
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        applicationContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, // Use EXTERNAL_CONTENT_URI for shared storage
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor -> // 'use' ensures the cursor is closed automatically
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val album = cursor.getString(albumColumn)
                val duration = cursor.getLong(durationColumn)
                val dataPath = cursor.getString(dataColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                mediaFiles.add(
                    AudioFile(
                        id,
                        title,
                        artist,
                        album,
                        duration,
                        dataPath,
                        contentUri
                    )
                )
                Log.d("MediaFinder", "Found: $title, Path: $dataPath, URI: $contentUri")
            }
        }

        if (mediaFiles.isEmpty()) {
            Log.d("MediaFinder", "No media files found.")
        } else {
            // Do something with the mediaFiles list
            // e.g., display them in a RecyclerView
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WrightMusicPlayerTheme {
        Greeting("Android")
    }
}