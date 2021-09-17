package com.sablab.android_simple_music_player.extensions

import android.content.Context
import android.provider.MediaStore
import android.widget.Toast
import com.sablab.android_simple_music_player.model.Music

/**
 * This method not working for some devices (checked on Redmi 6 Pro)
 */
fun Context.getPlayList(): ArrayList<Music> {
    //Some audio may be explicitly marked as not being music
    val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION
    )

    val list = arrayListOf<Music>()
    val contentResolver = contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val cursor = contentResolver.query(uri, projection, selection, null, null)
    if (cursor == null) {
        Toast.makeText(this, "Something Went Wrong.", Toast.LENGTH_LONG).show()
    } else if (!cursor.moveToFirst()) {
        Toast.makeText(this, "No Music Found on SD Card.", Toast.LENGTH_LONG).show()
    } else {
        do {
            val songID: Long = cursor.getLong(0)
            val songArtist: String = cursor.getString(1)
            val songTitle: String = cursor.getString(2)
            val songData: String = cursor.getString(3)
            val songDisplayName: String = cursor.getString(4)
            val songDuration: Long = cursor.getLong(5)
            list.add(Music(songID, songArtist, songTitle, songData, songDisplayName, songDuration))
        } while (cursor.moveToNext())
        cursor.close()
    }
    return list
}