package ch.imaginarystudio.keyboardapp.itertools

object Combinations {
    fun <T> Iterable<T>.combinations(length: Int): Sequence<List<T>> =
        sequence {
            val pool = this@combinations as? List<T> ?: toList()
            val n = pool.size
            if (length > n) return@sequence
            val indices = IntArray(length) { it }
            while (true) {
                yield(indices.map { pool[it] })
                var i = length
                do {
                    i--
                    if (i == -1) return@sequence
                } while (indices[i] == i + n - length)
                indices[i]++
                for (j in i + 1 until length) indices[j] = indices[j - 1] + 1
            }
        }
}