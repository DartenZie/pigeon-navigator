package cz.miroslavpasek.pigeonnavigator.data.search.mapper

import cz.miroslavpasek.pigeonnavigator.data.search.local.LocalSearchRecord

/**
 * Maps local records into domain-facing search labels.
 */
fun List<LocalSearchRecord>.toDomainSearchItems(): List<String> =
    map(LocalSearchRecord::label)
