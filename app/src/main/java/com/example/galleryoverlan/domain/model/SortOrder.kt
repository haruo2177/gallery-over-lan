package com.example.galleryoverlan.domain.model

enum class SortOrder(val label: String) {
    NAME_ASC("名前 (A→Z)"),
    NAME_DESC("名前 (Z→A)"),
    DATE_ASC("日付 (古い順)"),
    DATE_DESC("日付 (新しい順)"),
    SIZE_ASC("サイズ (小さい順)"),
    SIZE_DESC("サイズ (大きい順)")
}
