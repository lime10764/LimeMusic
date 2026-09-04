package com.limemusic.app.player

import com.limemusic.app.data.MusicItem

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import kotlin.math.abs

data class LyricsLine(
    val timeMs: Long,
    val text: String
)

data class LyricsResult(
    val lines: List<LyricsLine>,
    val synced: Boolean,
    val source: String,
    val language: LyricsLanguage
)

enum class LyricsLanguage {
    CHINESE,
    ENGLISH,
    JAPANESE,
    KOREAN,
    OTHER,
    MIXED,
    UNKNOWN
}

object LyricsRepository {

    private const val BASE_URL = "https://lrclib.net/api/get"

    private val client = OkHttpClient.Builder()
        .build()

    private val gson = Gson()

    suspend fun getLyrics(
        music: MusicItem,
        durationMs: Long
    ): LyricsResult? = withContext(Dispatchers.IO) {

        val title = music.displayTitle().trim()
        val artist = music.displayArtist().trim()

        if (title.isBlank()) return@withContext null

        val durationSeconds =
            if (durationMs > 0L) durationMs / 1000.0 else null

        val album = music.album
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != "未知专辑" }

        val exactResults = mutableListOf<LyricsCandidate>()

        // 第一优先：精确查询
        requestExact(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds
        )?.let {
            exactResults += it
        }

        val bestExact = selectBestCandidate(
            candidates = exactResults,
            title = title,
            artist = artist,
            durationMs = durationMs
        )

        if (bestExact != null) {
            return@withContext convertCandidate(bestExact)
        }

        // 第二优先：搜索接口
        val searchResults = requestSearch(
            title = title,
            artist = artist
        )

        val bestSearch = selectBestCandidate(
            candidates = searchResults,
            title = title,
            artist = artist,
            durationMs = durationMs
        )

        if (bestSearch != null) {
            return@withContext convertCandidate(bestSearch)
        }

        null
    }

    private fun requestExact(
        title: String,
        artist: String,
        album: String?,
        durationSeconds: Double?
    ): LyricsCandidate? {
        return try {
            val urlBuilder = BASE_URL.toHttpUrl()
                .newBuilder()
                .addQueryParameter("track_name", title)

            if (artist.isNotBlank() && artist != "未知歌手") {
                urlBuilder.addQueryParameter("artist_name", artist)
            }

            album?.let {
                urlBuilder.addQueryParameter("album_name", it)
            }

            durationSeconds?.let {
                urlBuilder.addQueryParameter(
                    "duration",
                    String.format(Locale.US, "%.3f", it)
                )
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "LimeMusic/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }

                val body = response.body.string()

                if (body.isBlank()) {
                    return null
                }

                gson.fromJson(
                    body,
                    LyricsCandidate::class.java
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun requestSearch(
        title: String,
        artist: String
    ): List<LyricsCandidate> {
        return try {
            val query = buildString {
                append(title)

                if (artist.isNotBlank() && artist != "未知歌手") {
                    append(" ")
                    append(artist)
                }
            }

            val url = "https://lrclib.net/api/search"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("q", query)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LimeMusic/1.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return emptyList()
                }

                val body = response.body.string()

                if (body.isBlank()) {
                    return emptyList()
                }

                gson.fromJson(
                    body,
                    Array<LyricsCandidate>::class.java
                ).toList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun selectBestCandidate(
        candidates: List<LyricsCandidate>,
        title: String,
        artist: String,
        durationMs: Long
    ): LyricsCandidate? {

        if (candidates.isEmpty()) {
            return null
        }

        val songLanguage = detectLanguage(
            "$title $artist"
        )

        return candidates
            .map { candidate ->
                candidate to scoreCandidate(
                    candidate = candidate,
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    songLanguage = songLanguage
                )
            }
            .filter { it.second >= 72 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun scoreCandidate(
        candidate: LyricsCandidate,
        title: String,
        artist: String,
        durationMs: Long,
        songLanguage: LyricsLanguage
    ): Int {

        var score = 0

        val candidateTitle = candidate.trackName.orEmpty()
        val candidateArtist = candidate.artistName.orEmpty()

        val titleSimilarity = similarity(
            normalize(title),
            normalize(candidateTitle)
        )

        val artistSimilarity = similarity(
            normalize(artist),
            normalize(candidateArtist)
        )

        score += (titleSimilarity * 45.0).toInt()
        score += (artistSimilarity * 30.0).toInt()

        // 同步歌词优先
        if (!candidate.syncedLyrics.isNullOrBlank()) {
            score += 15
        }

        // 时长匹配
        if (durationMs > 0L && candidate.duration != null) {
            val candidateDurationMs =
                (candidate.duration * 1000.0).toLong()

            val diff =
                abs(candidateDurationMs - durationMs)

            score += when {
                diff <= 1000L -> 20
                diff <= 2000L -> 16
                diff <= 5000L -> 10
                diff <= 10000L -> 4
                else -> 0
            }
        }

        // 歌词语言匹配
        val lyricsText = buildString {
            append(candidate.syncedLyrics.orEmpty())
            append("\n")
            append(candidate.plainLyrics.orEmpty())
        }

        val lyricsLanguage = detectLanguage(lyricsText)

        score += when {
            songLanguage == LyricsLanguage.CHINESE &&
                lyricsLanguage == LyricsLanguage.CHINESE -> 15

            songLanguage == LyricsLanguage.ENGLISH &&
                lyricsLanguage == LyricsLanguage.ENGLISH -> 15

            songLanguage == LyricsLanguage.JAPANESE &&
                lyricsLanguage == LyricsLanguage.JAPANESE -> 15

            songLanguage == LyricsLanguage.KOREAN &&
                lyricsLanguage == LyricsLanguage.KOREAN -> 15

            songLanguage == LyricsLanguage.MIXED &&
                lyricsLanguage != LyricsLanguage.UNKNOWN -> 8

            else -> 0
        }

        return score
    }

    private fun convertCandidate(
        candidate: LyricsCandidate
    ): LyricsResult? {

        val synced = candidate.syncedLyrics
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (synced != null) {
            val lines = parseLrc(synced)

            if (lines.isNotEmpty()) {
                return LyricsResult(
                    lines = lines,
                    synced = true,
                    source = "LRCLIB",
                    language = detectLanguage(
                        lines.joinToString("\n") { it.text }
                    )
                )
            }
        }

        // 没有时间轴就只保存普通歌词，
        // 后续 UI 可以明确显示“非同步歌词”，
        // 不允许把它当成同步歌词。
        val plain = candidate.plainLyrics
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (plain != null) {
            val lines = plain
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapIndexed { index, text ->
                    LyricsLine(
                        timeMs = index.toLong(),
                        text = text
                    )
                }

            if (lines.isNotEmpty()) {
                return LyricsResult(
                    lines = lines,
                    synced = false,
                    source = "LRCLIB",
                    language = detectLanguage(plain)
                )
            }
        }

        return null
    }

    private fun parseLrc(lrc: String): List<LyricsLine> {

        val result = mutableListOf<LyricsLine>()

        val regex = Regex(
            """\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\](.*)"""
        )

        lrc.lines().forEach { rawLine ->

            val line = rawLine.trim()

            if (line.isBlank()) {
                return@forEach
            }

            val match = regex.matchEntire(line)
                ?: return@forEach

            val minutes = match.groupValues[1]
                .toLongOrNull()
                ?: return@forEach

            val seconds = match.groupValues[2]
                .toLongOrNull()
                ?: return@forEach

            val fractionText = match.groupValues
                .getOrNull(3)
                .orEmpty()

            val fractionMs = when (fractionText.length) {
                1 -> fractionText.toLongOrNull()?.times(100L) ?: 0L
                2 -> fractionText.toLongOrNull()?.times(10L) ?: 0L
                3 -> fractionText.toLongOrNull() ?: 0L
                else -> 0L
            }

            val text = match.groupValues
                .getOrNull(4)
                ?.trim()
                .orEmpty()

            if (text.isBlank()) {
                return@forEach
            }

            val timeMs =
                minutes * 60_000L +
                seconds * 1_000L +
                fractionMs

            result += LyricsLine(
                timeMs = timeMs,
                text = text
            )
        }

        return result
            .sortedBy { it.timeMs }
            .distinctBy { "${it.timeMs}|${it.text}" }
    }

    private fun normalize(value: String): String {
        return value
            .lowercase(Locale.getDefault())
            .replace(
                Regex("""[\p{P}\p{S}]"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
    }

    private fun similarity(
        a: String,
        b: String
    ): Double {

        if (a.isBlank() || b.isBlank()) {
            return 0.0
        }

        if (a == b) {
            return 1.0
        }

        if (a.contains(b) || b.contains(a)) {
            return 0.9
        }

        val aWords = a.split(" ")
            .filter { it.isNotBlank() }
            .toSet()

        val bWords = b.split(" ")
            .filter { it.isNotBlank() }
            .toSet()

        if (aWords.isEmpty() || bWords.isEmpty()) {
            return 0.0
        }

        val intersection =
            aWords.intersect(bWords).size.toDouble()

        val union =
            aWords.union(bWords).size.toDouble()

        return if (union == 0.0) {
            0.0
        } else {
            intersection / union
        }
    }

    fun detectLanguage(text: String): LyricsLanguage {

        if (text.isBlank()) {
            return LyricsLanguage.UNKNOWN
        }

        var chinese = 0
        var english = 0
        var japanese = 0
        var korean = 0

        text.forEach { char ->

            when {
                char in '\u4E00'..'\u9FFF' -> {
                    chinese++
                }

                char in '\u3040'..'\u30FF' -> {
                    japanese++
                }

                char in '\uAC00'..'\uD7AF' -> {
                    korean++
                }

                char.isLetter() &&
                    char.code < 128 -> {
                    english++
                }
            }
        }

        val total =
            chinese + english + japanese + korean

        if (total == 0) {
            return LyricsLanguage.UNKNOWN
        }

        val values = listOf(
            LyricsLanguage.CHINESE to chinese,
            LyricsLanguage.ENGLISH to english,
            LyricsLanguage.JAPANESE to japanese,
            LyricsLanguage.KOREAN to korean
        )

        val sorted = values.sortedByDescending { it.second }

        val highest = sorted[0]
        val second = sorted[1]

        // 两种语言比例非常接近，认为是混合歌词
        if (second.second > 0 &&
            highest.second.toDouble() / second.second < 1.35
        ) {
            return LyricsLanguage.MIXED
        }

        return highest.first
    }

    private data class LyricsCandidate(
        @SerializedName("id")
        val id: Long? = null,

        @SerializedName("trackName")
        val trackName: String? = null,

        @SerializedName("artistName")
        val artistName: String? = null,

        @SerializedName("albumName")
        val albumName: String? = null,

        @SerializedName("duration")
        val duration: Double? = null,

        @SerializedName("plainLyrics")
        val plainLyrics: String? = null,

        @SerializedName("syncedLyrics")
        val syncedLyrics: String? = null
    )
}
