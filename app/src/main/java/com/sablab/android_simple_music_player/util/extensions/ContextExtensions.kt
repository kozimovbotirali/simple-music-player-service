@file:Suppress("unused")

package com.sablab.android_simple_music_player.util.extensions

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.util.timberErrorLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayInputStream
import java.io.InputStream


/**
 * This method not working for some devices (checked on Redmi 6 Pro)
 */

const val ID = 0
const val ARTIST = 1
const val TITLE = 2
const val DATA = 3
const val DISPLAY_NAME = 4
const val DURATION = 5
const val ALBUM_ID = 6

fun Context.getPlayList(): Flow<Cursor> = flow {
    //Some audio may be explicitly marked as not being music
    val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

    val projection = arrayOf(
        MediaStore.Audio.Media._ID, //0
        MediaStore.Audio.Media.ARTIST, //1
        MediaStore.Audio.Media.TITLE, //2
        MediaStore.Audio.Media.DATA, //3
        MediaStore.Audio.Media.DISPLAY_NAME, //4
        MediaStore.Audio.Media.DURATION, //5
        MediaStore.Audio.Media.ALBUM_ID //5
    )

    val cursor: Cursor = contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        null
    ) ?: return@flow

    val songs: MutableList<String> = ArrayList()
    while (cursor.moveToNext()) {
        songs.add(
            cursor.getString(0)
                .toString() + "||" + cursor.getString(1) + "||" + cursor.getString(2) + "||" + cursor.getString(
                3
            ) + "||" + cursor.getString(4) + "||" + cursor.getString(5)
        )
//        timberLog(songs.last())
    }
    emit(cursor)
}.flowOn(Dispatchers.IO)

fun Context.songArt(path: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    val inputStream: InputStream
    retriever.setDataSource(path)
    return if (retriever.embeddedPicture != null) {
        inputStream = ByteArrayInputStream(retriever.embeddedPicture)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        retriever.release()
        bitmap
    } else {
        retriever.release()
        getLargeIcon(this)
    }
}

fun Context.songArt(albumId: Long): Uri? {
    try {
        val sArtworkUri: Uri = Uri
            .parse("content://media/external/audio/albumart")
        val uri = ContentUris.withAppendedId(sArtworkUri, albumId)
        val pfd: ParcelFileDescriptor? = this.contentResolver
            .openFileDescriptor(uri, "r")
        if (pfd != null) {
            return uri
        }
    } catch (e: Exception) {
        timberErrorLog(e.message.toString())
    }
    return null
}

private fun getLargeIcon(context: Context): Bitmap? {
    return BitmapFactory.decodeResource(context.resources, R.drawable.ic_music)
}