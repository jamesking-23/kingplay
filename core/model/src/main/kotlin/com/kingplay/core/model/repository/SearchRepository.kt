package com.kingplay.core.model.repository

import com.kingplay.core.model.SearchResults
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    /** Prefix search over titles, artists, albums and genres. A blank query yields [SearchResults.Empty]. */
    fun search(query: String, limitPerSection: Int = DEFAULT_SECTION_LIMIT): Flow<SearchResults>

    companion object {
        const val DEFAULT_SECTION_LIMIT = 30
    }
}
