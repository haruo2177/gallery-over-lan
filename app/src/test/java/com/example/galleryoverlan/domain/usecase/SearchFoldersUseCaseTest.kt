package com.example.galleryoverlan.domain.usecase

import com.example.galleryoverlan.domain.model.FolderItem
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchFoldersUseCaseTest {

    private lateinit var useCase: SearchFoldersUseCase
    private val folders = listOf(
        FolderItem("Family Photos", "Family Photos"),
        FolderItem("Travel 2025", "Travel 2025"),
        FolderItem("Birthday Party", "Birthday Party"),
        FolderItem("family-videos", "family-videos")
    )

    @Before
    fun setup() {
        useCase = SearchFoldersUseCase()
    }

    @Test
    fun `blank query returns all folders`() {
        val result = useCase(folders, "")
        assertEquals(folders, result)
    }

    @Test
    fun `whitespace query returns all folders`() {
        val result = useCase(folders, "   ")
        assertEquals(folders, result)
    }

    @Test
    fun `case-insensitive search`() {
        val result = useCase(folders, "family")
        assertEquals(2, result.size)
        assertEquals("Family Photos", result[0].name)
        assertEquals("family-videos", result[1].name)
    }

    @Test
    fun `partial match`() {
        val result = useCase(folders, "travel")
        assertEquals(1, result.size)
        assertEquals("Travel 2025", result[0].name)
    }

    @Test
    fun `no match returns empty`() {
        val result = useCase(folders, "xyz")
        assertEquals(0, result.size)
    }

    @Test
    fun `number search`() {
        val result = useCase(folders, "2025")
        assertEquals(1, result.size)
    }
}
