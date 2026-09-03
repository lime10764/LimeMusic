package com.limemusic.app.parser

object MusicNameParser {

    data class ParsedMusic(
        val title: String,
        val artist: String,
        val extension: String
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "ogg", "m4a", "aac",
        "opus", "wma", "aiff", "ape", "wv", "caf", "webm"
    )

    fun parse(fileName: String): ParsedMusic {
        var name = fileName.trim()
        val extension = getExtension(name)

        if (extension.isNotEmpty()) {
            name = name.dropLast(extension.length + 1)
        }

        name = removeUselessSuffix(name.trim())

        val separator = findTitleArtistSeparator(name)

        if (separator != -1) {
            var title = name.substring(0, separator).trim()
            var artist = name.substring(separator + 1).trim()

            artist = removeArtistNumericSuffix(artist)
            if (title.isBlank()) title = name

            return ParsedMusic(
                title = cleanTitle(title),
                artist = normalizeArtist(artist),
                extension = extension
            )
        }

        return ParsedMusic(
            title = cleanTitle(name),
            artist = "",
            extension = extension
        )
    }

    private fun getExtension(fileName: String): String {
        val cleanName = fileName.substringBefore('?')
        val index = cleanName.lastIndexOf('.')
        if (index < 0 || index >= cleanName.length - 1) return ""

        val extension = cleanName.substring(index + 1).lowercase()
        return if (extension in AUDIO_EXTENSIONS) extension else ""
    }

    private fun removeUselessSuffix(input: String): String {
        var value = input.trim()

        repeat(4) {
            val match = Regex("""^(.*?)-(\d{2,12})$""").matchEntire(value)
                ?: return@repeat

            val before = match.groupValues[1].trim()
            val number = match.groupValues[2]

            if (before.isBlank()) return@repeat
            if (isUselessNumericSuffix(number)) {
                value = before
            } else {
                return@repeat
            }
        }

        return value.trim()
    }

    private fun isUselessNumericSuffix(number: String): Boolean {
        return number == "64" ||
            number == "96" ||
            number == "128" ||
            number == "160" ||
            number == "192" ||
            number == "256" ||
            number == "320" ||
            number == "512" ||
            number == "1000" ||
            number == "2000" ||
            number.length >= 6
    }

    private fun findTitleArtistSeparator(value: String): Int {
        val index = value.indexOf('-')

        if (index > 0) {
            val left = value.substring(0, index).trim()
            val right = value.substring(index + 1).trim()
            if (left.isNotBlank() && right.isNotBlank()) return index
        }

        val chineseIndex = value.indexOf('—')

        if (chineseIndex > 0) {
            val left = value.substring(0, chineseIndex).trim()
            val right = value.substring(chineseIndex + 1).trim()
            if (left.isNotBlank() && right.isNotBlank()) return chineseIndex
        }

        return -1
    }

    private fun removeArtistNumericSuffix(input: String): String {
        var value = input.trim()

        repeat(2) {
            val match = Regex("""^(.*?)[-_](\d{2,12})$""").matchEntire(value)
                ?: return@repeat

            val before = match.groupValues[1].trim()
            val number = match.groupValues[2]

            if (before.isBlank()) return@repeat
            if (isUselessNumericSuffix(number)) {
                value = before
            } else {
                return@repeat
            }
        }

        return value.trim()
    }

    private fun normalizeArtist(input: String): String {
        if (input.isBlank()) return ""

        return input
            .replace(Regex("""\s*[_＋+]\s*"""), " / ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    private fun cleanTitle(input: String): String =
        input.trim()
            .trim(' ', '-', '_', '—', '–')
            .replace(Regex("""\s{2,}"""), " ")
}
