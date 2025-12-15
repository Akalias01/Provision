package com.example.rezon.data

data class ChapterMarker(
    val title: String,
    val startMs: Long
)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val filePath: String,
    val synopsis: String,
    val seriesInfo: String,
    val chapterMarkers: List<ChapterMarker>
)
