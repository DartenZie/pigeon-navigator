package cz.miroslavpasek.pigeonnavigator.data.search.mapper

import cz.miroslavpasek.pigeonnavigator.data.search.local.LocalSearchRecord
import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult

/**
 * Maps local search records to domain-facing label strings.
 */
fun List<LocalSearchRecord>.toDomainSearchItems(): List<SearchResult> =
    mapIndexed { index, record ->
        SearchResult.Airport(
            id = "local:$index:${record.label}",
            title = record.label,
            subtitle = "Local result",
            latitude = 0.0,
            longitude = 0.0
        )
    }
