package club.liefuck

import java.text.SimpleDateFormat

const val utf8Collate = "utf8mb4_unicode_520_ci"

const val weekInMillis = (7 * 24 * 60 * 60 * 1000).toLong()
const val thirtySecInMillis = (30 * 1000).toLong()
const val twoMinutesInMillis = (2 * 60 * 1000).toLong()

fun tsAsDateString(ts: Long): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm").format(ts)
}


//stolen from https://github.com/jdereg/java-util
fun damerauLevenshteinDistance(source: CharSequence?, target: CharSequence?): Int {
    if (source == null || "" == source) {
        return if (target == null || "" == target) 0 else target.length
    } else if (target == null || "" == target) {
        return source.length
    }
    val srcLen = source.length
    val targetLen = target.length
    val distanceMatrix = Array(srcLen + 1) { IntArray(targetLen + 1) }

    for (srcIndex in 0..srcLen) {
        distanceMatrix[srcIndex][0] = srcIndex
    }
    for (targetIndex in 0..targetLen) {
        distanceMatrix[0][targetIndex] = targetIndex
    }

    for (srcIndex in 1..srcLen) {
        for (targetIndex in 1..targetLen) {
            val cost = if (source[srcIndex - 1] == target[targetIndex - 1]) 0 else 1
            distanceMatrix[srcIndex][targetIndex] =
                minimum(
                    (distanceMatrix[srcIndex - 1][targetIndex] + 1).toLong(),
                    (distanceMatrix[srcIndex][targetIndex - 1] + 1).toLong(),
                    (distanceMatrix[srcIndex - 1][targetIndex - 1] + cost).toLong()
                ).toInt()
            if (srcIndex == 1 || targetIndex == 1) {
                continue
            }
            if (source[srcIndex - 1] == target[targetIndex - 2] && source[srcIndex - 2] == target[targetIndex - 1]) {
                distanceMatrix[srcIndex][targetIndex] = minimum(
                    distanceMatrix[srcIndex][targetIndex].toLong(),
                    (distanceMatrix[srcIndex - 2][targetIndex - 2] + cost).toLong()
                ).toInt()
            }
        }
    }
    return distanceMatrix[srcLen][targetLen]
}

//stolen from https://github.com/jdereg/java-util
fun minimum(vararg values: Long): Long {
    val len = values.size
    var current = values[0]
    for (i in 1 until len) {
        current = values[i].coerceAtMost(current)
    }
    return current
}
