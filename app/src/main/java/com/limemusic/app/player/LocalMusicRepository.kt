package com.limemusic.app.player

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import com.limemusic.app.data.MusicItem

object LocalMusicRepository {

    fun scan(
        resolver: ContentResolver
    ): List<MusicItem> {

        val result = mutableListOf<MusicItem>()

        val collection =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val sort =
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        resolver.query(
            collection,
            projection,
            selection,
            null,
            sort
        )?.use { cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )

            val titleIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )

            val artistIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                )

            val albumIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM
                )

            val durationIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DURATION
                )

            val nameIndex =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DISPLAY_NAME
                )

            while (cursor.moveToNext()) {

                val id =
                    cursor.getLong(idIndex)

                val title =
                    cursor.getString(titleIndex)
                        ?.takeIf { it.isNotBlank() }
                        ?: "未知歌曲"

                val artist =
                    cursor.getString(artistIndex)
                        ?.takeIf { it.isNotBlank() }
                        ?: "未知歌手"

                val album =
                    cursor.getString(albumIndex)
                        ?.takeIf { it.isNotBlank() }
                        ?: "未知专辑"

                val duration =
                    cursor.getLong(durationIndex)

                val fileName =
                    cursor.getString(nameIndex)
                        ?.takeIf { it.isNotBlank() }
                        ?: title

                val uri =
                    ContentUris.withAppendedId(
                        collection,
                        id
                    ).toString()

                val extension =
                    fileName.substringAfterLast(
                        ".",
                        ""
                    ).lowercase()

                result.add(
                    MusicItem(
                        id = "local_$id",
                        title = title,
                        artist = artist,
                        fileName = fileName,
                        path = uri,
                        extension = extension,
                        streamUrl = uri,
                        fallbackUrl = uri,
                        githubUrl = "",
                        isFavorite = false,
                        playCount = 0L,
                        lastPlayedAt = 0L,
                        durationMs = duration,
                        album = album,
                        artworkUrl = null
                    )
                )
            }
        }

        return result
    }
}
