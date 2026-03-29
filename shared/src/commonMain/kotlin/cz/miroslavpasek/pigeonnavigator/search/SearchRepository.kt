package cz.miroslavpasek.pigeonnavigator.search

interface SearchRepository {
    fun search(query: String): List<String>
}