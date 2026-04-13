package cz.miroslavpasek.pigeonnavigator.data.search.mapper

import cz.miroslavpasek.pigeonnavigator.data.search.local.LocalSearchRecord

/**
 * Maps local search records to domain-facing label strings.
 */
fun List<LocalSearchRecord>.toDomainSearchItems(): List<String> =
    map(LocalSearchRecord::label)
