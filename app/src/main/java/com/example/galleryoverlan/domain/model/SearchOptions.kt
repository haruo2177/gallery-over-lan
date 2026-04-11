package com.example.galleryoverlan.domain.model

data class SearchOptions(
    val mode: SearchMode = SearchMode.AND,
    val kanaUnify: Boolean = true,
    val caseSensitive: Boolean = false,
    val useWildcard: Boolean = false,
    val excludeQuery: String = ""
)

enum class SearchMode(val label: String) {
    AND("AND（すべて含む）"),
    OR("OR（いずれか含む）")
}
