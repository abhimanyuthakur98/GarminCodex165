package com.garmincodex.app

/** Never interleave two manufacturers' raw sensor streams within a day. */
internal object SourceSelection {
    fun <T> primary(records: List<T>, source: (T) -> String, coverage: (T) -> Long): List<T> =
        records.groupBy(source).entries.sortedWith(compareByDescending<Map.Entry<String, List<T>>> {
            it.value.sumOf { record -> coverage(record).coerceAtLeast(0) }
        }.thenBy { it.key }).firstOrNull()?.value.orEmpty()

    // Mirrored sessions from different apps count once; separate sessions from one app remain intact.
    fun <T> workouts(records: List<T>, source: (T) -> String, start: (T) -> Long, end: (T) -> Long, type: (T) -> Int): List<T> {
        val kept = mutableListOf<T>()
        records.sortedWith(compareBy<T> { source(it) }.thenBy { start(it) }.thenBy { end(it) }).forEach { record ->
            val duplicate = kept.any { other ->
                val overlap = (minOf(end(record), end(other)) - maxOf(start(record), start(other))).coerceAtLeast(0)
                source(record) != source(other) && type(record) == type(other) &&
                    overlap > 0 && overlap.toDouble() / maxOf(end(record)-start(record), end(other)-start(other), 1L) >= .8
            }
            if (!duplicate) kept += record
        }
        return kept
    }
}
