package cz.miroslavpasek.pigeonnavigator.feature.searchdock.presentation

import cz.miroslavpasek.pigeonnavigator.domain.search.SearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchDockReducerTest {

    private val reducer = SearchDockReducer(routeResolver = SearchDockRouteResolver())

    @Test
    fun routePlanningWinsOverOtherRoutes() {
        val state = SearchDockState(
            hasMapSelection = true,
            searchQuery = "prg"
        )

        val next = reducer.reduce(state, SearchDockIntent.RoutePlanningChanged(planning = true))

        assertEquals(SearchDockRoute.RoutePlanner, next.activeRoute)
    }

    @Test
    fun mapTapRouteAlwaysAvailable() {
        val next = reducer.reduce(SearchDockState(), SearchDockIntent.MapSelectionChanged(hasSelection = true))

        assertTrue(next.availableRoutes.contains(SearchDockRoute.MapTap))
    }

    @Test
    fun mapSelectionDoesNotOverrideDefaultRoute() {
        val next = reducer.reduce(SearchDockState(), SearchDockIntent.MapSelectionChanged(hasSelection = true))

        assertEquals(SearchDockRoute.Nearby, next.activeRoute)
    }

    @Test
    fun searchQuerySelectsSearchRoute() {
        val next = reducer.reduce(SearchDockState(), SearchDockIntent.SearchQueryChanged("prg"))

        assertEquals(SearchDockRoute.Search, next.activeRoute)
    }

    @Test
    fun expandingWithoutSpecialStateDefaultsToNearby() {
        val state = SearchDockState(
            isExpanded = false,
            activeRoute = SearchDockRoute.Search,
            selectedRouteOverride = SearchDockRoute.Search
        )

        val next = reducer.reduce(state, SearchDockIntent.ExpandedChanged(expanded = true))

        assertEquals(SearchDockRoute.Nearby, next.activeRoute)
        assertEquals(null, next.selectedRouteOverride)
    }

    @Test
    fun mapTapLookupCompletedWithResultsAutoExpandsAndSelectsMapTap() {
        val next = reducer.reduce(
            SearchDockState(),
            SearchDockIntent.MapTapLookupChanged(
                cursor = 1L,
                isLoading = false,
                hasResults = true,
                hasSelection = true
            )
        )

        assertTrue(next.isExpanded)
        assertEquals(SearchDockRoute.MapTap, next.activeRoute)
        assertEquals(SearchDockRoute.MapTap, next.selectedRouteOverride)
        assertEquals(1L, next.lastAutoExpandedMapTapCursor)
        assertTrue(next.hasMapSelection)
    }

    @Test
    fun mapTapLookupWithZeroResultsKeepsDockCollapsedAndSilent() {
        val next = reducer.reduce(
            SearchDockState(),
            SearchDockIntent.MapTapLookupChanged(
                cursor = 1L,
                isLoading = false,
                hasResults = false,
                hasSelection = true
            )
        )

        assertFalse(next.isExpanded)
        assertEquals(SearchDockRoute.Nearby, next.activeRoute)
        assertNull(next.selectedRouteOverride)
        assertNull(next.lastAutoExpandedMapTapCursor)
        assertTrue(next.hasMapSelection)
    }

    @Test
    fun mapTapLookupLoadingDoesNotAutoExpand() {
        val next = reducer.reduce(
            SearchDockState(),
            SearchDockIntent.MapTapLookupChanged(
                cursor = 1L,
                isLoading = true,
                hasResults = false,
                hasSelection = true
            )
        )

        assertFalse(next.isExpanded)
        assertNull(next.selectedRouteOverride)
        assertNull(next.lastAutoExpandedMapTapCursor)
    }

    @Test
    fun mapTapLookupSameCursorDoesNotReExpandAfterUserCollapse() {
        val firstResults = SearchDockIntent.MapTapLookupChanged(
            cursor = 1L,
            isLoading = false,
            hasResults = true,
            hasSelection = true
        )
        val expanded = reducer.reduce(SearchDockState(), firstResults)
        assertTrue(expanded.isExpanded)

        // User manually collapses the dock.
        val collapsed = reducer.reduce(expanded, SearchDockIntent.ExpandedChanged(expanded = false))
        assertFalse(collapsed.isExpanded)
        // lastAutoExpandedMapTapCursor must stick across collapse to gate the rule.
        assertEquals(1L, collapsed.lastAutoExpandedMapTapCursor)

        // A re-emission of the same lookup (e.g. UI re-subscription) must NOT
        // re-expand the dock.
        val rebroadcast = reducer.reduce(collapsed, firstResults)
        assertFalse(rebroadcast.isExpanded)
    }

    @Test
    fun mapTapLookupNewCursorReExpandsEvenIfPreviousAutoExpandWasDismissed() {
        val firstResults = SearchDockIntent.MapTapLookupChanged(
            cursor = 1L,
            isLoading = false,
            hasResults = true,
            hasSelection = true
        )
        val expanded = reducer.reduce(SearchDockState(), firstResults)
        val collapsed = reducer.reduce(expanded, SearchDockIntent.ExpandedChanged(expanded = false))

        val secondResults = SearchDockIntent.MapTapLookupChanged(
            cursor = 2L,
            isLoading = false,
            hasResults = true,
            hasSelection = true
        )
        val reExpanded = reducer.reduce(collapsed, secondResults)

        assertTrue(reExpanded.isExpanded)
        assertEquals(SearchDockRoute.MapTap, reExpanded.activeRoute)
        assertEquals(2L, reExpanded.lastAutoExpandedMapTapCursor)
    }

    @Test
    fun openMapTapDetailStoresKey() {
        val next = reducer.reduce(
            SearchDockState(),
            SearchDockIntent.OpenMapTapDetail(key = "airport:LKAA")
        )

        assertEquals("airport:LKAA", next.selectedMapTapDetailKey)
    }

    @Test
    fun closeMapTapDetailClearsKey() {
        val opened = reducer.reduce(
            SearchDockState(),
            SearchDockIntent.OpenMapTapDetail(key = "airport:LKAA")
        )

        val closed = reducer.reduce(opened, SearchDockIntent.CloseMapTapDetail)

        assertNull(closed.selectedMapTapDetailKey)
    }

    @Test
    fun collapsingDockClearsOpenedDetailKey() {
        val opened = reducer
            .reduce(
                SearchDockState(),
                SearchDockIntent.MapTapLookupChanged(
                    cursor = 1L,
                    isLoading = false,
                    hasResults = true,
                    hasSelection = true
                )
            )
            .let { reducer.reduce(it, SearchDockIntent.OpenMapTapDetail(key = "navaid:PRG")) }

        assertEquals("navaid:PRG", opened.selectedMapTapDetailKey)

        val collapsed = reducer.reduce(opened, SearchDockIntent.ExpandedChanged(expanded = false))

        assertNull(collapsed.selectedMapTapDetailKey)
    }

    @Test
    fun freshLookupClearsPreviouslyOpenedDetailKey() {
        val withDetail = reducer
            .reduce(
                SearchDockState(),
                SearchDockIntent.MapTapLookupChanged(
                    cursor = 1L,
                    isLoading = false,
                    hasResults = true,
                    hasSelection = true
                )
            )
            .let { reducer.reduce(it, SearchDockIntent.OpenMapTapDetail(key = "airport:LKAA")) }

        val nextLookup = reducer.reduce(
            withDetail,
            SearchDockIntent.MapTapLookupChanged(
                cursor = 2L,
                isLoading = false,
                hasResults = true,
                hasSelection = true
            )
        )

        assertNull(nextLookup.selectedMapTapDetailKey)
        // And the new lookup auto-expands per the existing rule.
        assertTrue(nextLookup.isExpanded)
    }

    @Test
    fun loadingPhaseDoesNotClearOpenedDetailKey() {
        val withDetail = reducer
            .reduce(
                SearchDockState(),
                SearchDockIntent.MapTapLookupChanged(
                    cursor = 1L,
                    isLoading = false,
                    hasResults = true,
                    hasSelection = true
                )
            )
            .let { reducer.reduce(it, SearchDockIntent.OpenMapTapDetail(key = "airport:LKAA")) }

        // A loading update for a future cursor must not preemptively clear the
        // currently-open detail (results haven't replaced anything yet).
        val midFlight = reducer.reduce(
            withDetail,
            SearchDockIntent.MapTapLookupChanged(
                cursor = 2L,
                isLoading = true,
                hasResults = false,
                hasSelection = true
            )
        )

        assertEquals("airport:LKAA", midFlight.selectedMapTapDetailKey)
    }

    @Test
    fun mapTapLookupClearsHasMapSelectionWhenSelectionDropped() {
        val withSelection = reducer.reduce(
            SearchDockState(),
            SearchDockIntent.MapTapLookupChanged(
                cursor = 1L,
                isLoading = false,
                hasResults = true,
                hasSelection = true
            )
        )
        assertTrue(withSelection.hasMapSelection)

        val cleared = reducer.reduce(
            withSelection,
            SearchDockIntent.MapTapLookupChanged(
                cursor = 1L,
                isLoading = false,
                hasResults = false,
                hasSelection = false
            )
        )

        assertFalse(cleared.hasMapSelection)
    }

    @Test
    fun collapsingClearsSearchInputAndResults() {
        val state = SearchDockState(
            isExpanded = true,
            searchQuery = "lkpr",
            isSearching = true,
            searchResults = listOf(
                SearchResult.Airport(
                    id = "airport:LKPR",
                    title = "LKPR",
                    subtitle = "Prague",
                    latitude = 50.1008,
                    longitude = 14.26
                )
            ),
            searchErrorMessage = "Error"
        )

        val next = reducer.reduce(state, SearchDockIntent.ExpandedChanged(expanded = false))

        assertEquals("", next.searchQuery)
        assertEquals(false, next.isSearching)
        assertEquals(emptyList(), next.searchResults)
        assertEquals(null, next.searchErrorMessage)
    }
}
