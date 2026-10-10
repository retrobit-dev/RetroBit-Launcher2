package com.retrobit.launcher.ui

/** Small dependency-free fuzzy scorer: exact/prefix matches rank above subsequences. */
internal object FuzzySearch {
    fun score(query: String, candidate: String): Int? {
        val q = normalize(query)
        val text = normalize(candidate)
        if (q.isEmpty()) return 0
        if (text.isEmpty()) return null

        val exactIndex = text.indexOf(q)
        if (exactIndex >= 0) {
            return when {
                exactIndex == 0 -> 1_000 - (text.length - q.length)
                else -> 800 - exactIndex * 5 - (text.length - q.length)
            }
        }

        var queryIndex = 0
        var firstMatch = -1
        var previousMatch = -2
        var gaps = 0
        var consecutive = 0
        var bestRun = 0

        text.forEachIndexed { index, char ->
            if (queryIndex < q.length && char == q[queryIndex]) {
                if (firstMatch < 0) firstMatch = index
                if (index == previousMatch + 1) {
                    consecutive++
                } else {
                    gaps += if (previousMatch >= 0) index - previousMatch - 1 else index
                    consecutive = 1
                }
                bestRun = maxOf(bestRun, consecutive)
                previousMatch = index
                queryIndex++
            }
        }

        if (queryIndex != q.length) return null
        val trailing = text.length - previousMatch - 1
        return 400 + bestRun * 12 - gaps * 8 - firstMatch * 3 - trailing.coerceAtMost(30)
    }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .filterNot { it.isWhitespace() }
}
