package com.windupairships.airshippiano.notedetection

object NoteDisplay {
    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun noteName(midi: Int): String {
        val note = midi % 12
        val octave = midi / 12 - 1
        return "${noteNames[note]}$octave"
    }

    fun pitchClass(midi: Int): Int = midi % 12

    data class NoteInfo(val label: String, val detail: String)

    fun describe(notes: Set<Int>, transpose: Int = 0): NoteInfo {
        if (notes.isEmpty()) return NoteInfo("", "Play a note...")

        val transposed = notes.mapNotNull { n ->
            val t = n + transpose
            if (t in 0..127) t else null
        }
        val sorted = transposed.sorted()
        if (sorted.isEmpty()) return NoteInfo("", "")

        if (sorted.size == 1) {
            return NoteInfo(noteName(sorted[0]), "Single note")
        }

        val chord = detectChord(sorted)
        if (chord != null) {
            val noteList = sorted.joinToString(" + ") { noteName(it) }
            return NoteInfo(chord, noteList)
        }

        val noteList = sorted.joinToString(" + ") { noteName(it) }
        return NoteInfo("${sorted.size} Notes", noteList)
    }

    fun detectChord(notes: List<Int>): String? {
        if (notes.size < 2) return null

        val pitchClasses = notes.map { pitchClass(it) }.toSet()
        if (pitchClasses.size < 2) return null

        for (root in pitchClasses.sorted()) {
            val intervals = pitchClasses.map { ((it - root) + 12) % 12 }.toSet()
            val rootName = noteNames[root]

            matchIntervals(intervals, rootName)?.let { return it }
        }

        return null
    }

    private fun matchIntervals(intervals: Set<Int>, root: String): String? {
        // Triads
        if (intervals == setOf(0, 4, 7)) return "$root Major"
        if (intervals == setOf(0, 3, 7)) return "$root Minor"
        if (intervals == setOf(0, 3, 6)) return "$root Dim"
        if (intervals == setOf(0, 4, 8)) return "$root Aug"
        if (intervals == setOf(0, 5, 7)) return "${root}sus4"
        if (intervals == setOf(0, 2, 7)) return "${root}sus2"

        // Seventh chords
        if (intervals == setOf(0, 4, 7, 11)) return "${root}maj7"
        if (intervals == setOf(0, 4, 7, 10)) return "${root}7"
        if (intervals == setOf(0, 3, 7, 10)) return "${root}m7"
        if (intervals == setOf(0, 3, 6, 10)) return "${root}m7b5"
        if (intervals == setOf(0, 3, 6, 9)) return "${root}dim7"
        if (intervals == setOf(0, 3, 7, 11)) return "${root}mMaj7"
        if (intervals == setOf(0, 4, 8, 10)) return "${root}7#5"

        // Extended
        if (intervals == setOf(0, 2, 4, 7, 10)) return "${root}9"
        if (intervals == setOf(0, 2, 4, 7, 11)) return "${root}maj9"

        // Power chord
        if (intervals == setOf(0, 7)) return "${root}5"

        // Intervals (2 notes)
        if (intervals.size == 2) {
            val sorted = intervals.sorted()
            return when (sorted[1]) {
                1 -> "$root + minor 2nd"
                2 -> "$root + major 2nd"
                3 -> "$root + minor 3rd"
                4 -> "$root + major 3rd"
                5 -> "$root + perfect 4th"
                6 -> "$root + tritone"
                7 -> "$root + perfect 5th"
                8 -> "$root + minor 6th"
                9 -> "$root + major 6th"
                10 -> "$root + minor 7th"
                11 -> "$root + major 7th"
                else -> null
            }
        }

        return null
    }
}
