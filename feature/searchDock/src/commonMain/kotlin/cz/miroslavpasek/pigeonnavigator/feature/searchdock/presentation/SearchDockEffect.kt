package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

sealed interface SearchDockEffect {
    data class ShowMessage(val message: String) : SearchDockEffect
}
