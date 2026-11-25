package dev.still.scriptures

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class ScriptureRepository(context: Context) {
    private val assets = context.applicationContext.assets
    @Volatile private var cachedBook: Pair<String, JSONArray>? = null

    fun cachedCatalog(): List<Book>? = processCatalog

    suspend fun catalog(): List<Book> = processCatalog ?: withContext(Dispatchers.IO) {
        processCatalog ?: run {
            val array = JSONArray(assets.open("scriptures/catalog.json").bufferedReader().use { it.readText() })
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                val counts = item.getJSONArray("verseCounts")
                Book(item.getString("id"), item.getString("name"), item.getString("volume"),
                    List(counts.length()) { counts.getInt(it) })
            }.also { processCatalog = it }
        }
    }

    suspend fun chapter(book: Book, number: Int): Chapter = withContext(Dispatchers.IO) {
        val cached = cachedBook
        val chapters = if (cached != null && cached.first == book.id) cached.second else {
            JSONArray(assets.open("scriptures/${book.id}.json").bufferedReader().use { it.readText() })
                .also { cachedBook = book.id to it }
        }
        val verses = chapters.getJSONArray(number - 1)
        Chapter(book, number, List(verses.length()) { verses.getString(it) })
    }

    companion object {
        @Volatile private var processCatalog: List<Book>? = null
    }
}
