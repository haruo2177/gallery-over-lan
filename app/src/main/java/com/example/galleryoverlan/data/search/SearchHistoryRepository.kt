package com.example.galleryoverlan.data.search

import android.content.Context
import com.example.galleryoverlan.domain.model.SearchMode
import com.example.galleryoverlan.domain.model.SearchOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class SearchHistoryEntry(
    val query: String,
    val options: SearchOptions
)

@Singleton
class SearchHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    }

    private val maxHistory = 20
    private val key = "history_v2"

    fun getHistory(): List<SearchHistoryEntry> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                SearchHistoryEntry(
                    query = obj.getString("query"),
                    options = SearchOptions(
                        mode = SearchMode.valueOf(obj.optString("mode", SearchMode.AND.name)),
                        kanaUnify = obj.optBoolean("kanaUnify", true),
                        caseSensitive = obj.optBoolean("caseSensitive", false),
                        useWildcard = obj.optBoolean("useWildcard", false),
                        excludeQuery = obj.optString("excludeQuery", "")
                    )
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addEntry(query: String, options: SearchOptions) {
        if (query.isBlank() && options.excludeQuery.isBlank()) return
        val current = getHistory().toMutableList()
        current.removeAll { it.query == query.trim() }
        current.add(0, SearchHistoryEntry(query.trim(), options))
        val limited = current.take(maxHistory)
        saveList(limited)
    }

    fun removeEntry(query: String) {
        val current = getHistory().toMutableList()
        current.removeAll { it.query == query }
        saveList(current)
    }

    fun clearHistory() {
        prefs.edit().remove(key).apply()
    }

    private fun saveList(entries: List<SearchHistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("query", entry.query)
                put("mode", entry.options.mode.name)
                put("kanaUnify", entry.options.kanaUnify)
                put("caseSensitive", entry.options.caseSensitive)
                put("useWildcard", entry.options.useWildcard)
                put("excludeQuery", entry.options.excludeQuery)
            }
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
