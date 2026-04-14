package cz.miroslavpasek.pigeonnavigator.data.aviation.internal

import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Navaid

/**
 * Lightweight parser for OFPKG navsnapshot XML format.
 */
internal class NavSnapshotParser {
    fun parse(xml: String): ParsedNavSnapshot {
        val airports = parseAirports(xml)
        val navaids = parseNavaids(xml)
        val airspaces = parseAirspaces(xml)
        return ParsedNavSnapshot(
            airports = airports,
            navaids = navaids,
            airspaces = airspaces
        )
    }

    private fun parseAirports(xml: String): List<Airport> {
        val section = section(xml, "Airports") ?: return emptyList()
        val matches = AIRPORT_BLOCK.findAll(section)
        return matches.mapNotNull { match ->
            val attrs = parseAttributes(match.groupValues[1])
            val lat = attrs["lat"]?.toDoubleOrNull() ?: return@mapNotNull null
            val lon = attrs["lon"]?.toDoubleOrNull() ?: return@mapNotNull null
            Airport(
                id = attrs["id"].orEmpty(),
                name = attrs["n"].orEmpty(),
                kind = attrs["d"].orEmpty(),
                latitude = lat,
                longitude = lon,
                elevationMeters = attrs["elevM"]?.toIntOrNull()
            )
        }.toList()
    }

    private fun parseNavaids(xml: String): List<Navaid> {
        val section = section(xml, "Navaids") ?: return emptyList()
        return NAVAID_TAG.findAll(section).mapNotNull { match ->
            val attrs = parseAttributes(match.groupValues[1])
            val lat = attrs["lat"]?.toDoubleOrNull() ?: return@mapNotNull null
            val lon = attrs["lon"]?.toDoubleOrNull() ?: return@mapNotNull null

            Navaid(
                id = attrs["id"].orEmpty(),
                name = attrs["n"].orEmpty(),
                kind = attrs["t"].orEmpty(),
                detail = attrs["d"].orEmpty(),
                latitude = lat,
                longitude = lon
            )
        }.toList()
    }

    private fun parseAirspaces(xml: String): List<Airspace> {
        val section = section(xml, "Airspaces") ?: return emptyList()
        return AIRSPACE_BLOCK.findAll(section).mapNotNull { match ->
            val attrs = parseAttributes(match.groupValues[1])
            val body = match.groupValues[2]

            val points = POINT_TAG.findAll(body).mapNotNull { pointMatch ->
                val pointAttrs = parseAttributes(pointMatch.groupValues[1])
                val lat = pointAttrs["lat"]?.toDoubleOrNull() ?: return@mapNotNull null
                val lon = pointAttrs["lon"]?.toDoubleOrNull() ?: return@mapNotNull null
                GeoPoint(latitude = lat, longitude = lon)
            }.toList()

            if (points.size < 3) {
                return@mapNotNull null
            }

            Airspace(
                id = attrs["id"].orEmpty(),
                name = attrs["n"].orEmpty(),
                kind = attrs["t"].orEmpty().ifEmpty { attrs["d"].orEmpty() },
                lowerLimitMeters = attrs["lowM"]?.toIntOrNull(),
                lowerLimitReference = attrs["lowRef"],
                upperLimitMeters = attrs["upM"]?.toIntOrNull(),
                upperLimitReference = attrs["upRef"],
                points = points
            )
        }.toList()
    }

    private fun section(xml: String, tag: String): String? {
        val startTag = "<$tag>"
        val endTag = "</$tag>"
        val start = xml.indexOf(startTag)
        if (start < 0) return null
        val contentStart = start + startTag.length
        val end = xml.indexOf(endTag, startIndex = contentStart)
        if (end < 0) return null
        return xml.substring(contentStart, end)
    }

    private fun parseAttributes(rawAttributes: String): Map<String, String> {
        return ATTRIBUTE_PATTERN.findAll(rawAttributes).associate { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
            key to value
        }
    }

    private companion object {
        val AIRPORT_BLOCK = Regex("<Airport\\s+([^>]+)>([\\s\\S]*?)</Airport>")
        val NAVAID_TAG = Regex("<Navaid\\s+([^>]+)/?>")
        val AIRSPACE_BLOCK = Regex("<Airspace\\s+([^>]+)>([\\s\\S]*?)</Airspace>")
        val POINT_TAG = Regex("<P\\s+([^>]+)/?>")
        val ATTRIBUTE_PATTERN = Regex("(\\w+)=\"([^\"]*)\"")
    }
}

internal data class ParsedNavSnapshot(
    val airports: List<Airport>,
    val navaids: List<Navaid>,
    val airspaces: List<Airspace>
)
