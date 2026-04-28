package cz.miroslavpasek.pigeonnavigator.data.aviation.internal

import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airport
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Airspace
import cz.miroslavpasek.pigeonnavigator.domain.aviation.GeoPoint
import cz.miroslavpasek.pigeonnavigator.domain.aviation.Navaid

/**
 * Single-pass parser for OFPKG navsnapshot XML format.
 *
 * Scans the raw XML bytes once, collecting airports, navaids, and airspaces
 * without regex backtracking or repeated full-string passes.
 *
 * Attribute parsing and entity unescaping are done in a single character sweep.
 */
internal class NavSnapshotParser {

    fun parse(xml: String): ParsedNavSnapshot {
        val airports = mutableListOf<Airport>()
        val navaids = mutableListOf<Navaid>()
        val airspaces = mutableListOf<Airspace>()

        var i = 0
        val len = xml.length

        parseLoop@ while (i < len) {
            val lt = xml.indexOf('<', i)
            if (lt < 0) break
            i = lt + 1

            // Skip closing tags and comments and processing instructions
            if (i >= len) break
            val c = xml[i]
            if (c == '/' || c == '!' || c == '?') {
                i = xml.indexOf('>', i).let { if (it < 0) len else it + 1 }
                continue
            }

            // Read tag name
            val nameEnd = run {
                var j = i
                while (j < len && xml[j] != ' ' && xml[j] != '/' && xml[j] != '>') j++
                j
            }
            val tagName = xml.substring(i, nameEnd)
            i = nameEnd

            when (tagName) {
                "Airport" -> {
                    val (attrs, end) = parseTagAttrsAndEnd(xml, i) ?: run { i = len; continue@parseLoop }
                    i = end
                    val lat = attrs["lat"]?.toDoubleOrNull() ?: continue@parseLoop
                    val lon = attrs["lon"]?.toDoubleOrNull() ?: continue@parseLoop
                    airports += Airport(
                        id = attrs["id"].orEmpty(),
                        name = attrs["n"].orEmpty(),
                        kind = attrs["d"].orEmpty(),
                        latitude = lat,
                        longitude = lon,
                        elevationMeters = attrs["elevM"]?.toIntOrNull()
                    )
                    // skip to </Airport>
                    val close = xml.indexOf("</Airport>", i)
                    i = if (close < 0) len else close + "</Airport>".length
                }
                "Navaid" -> {
                    val (attrs, end) = parseTagAttrsAndEnd(xml, i) ?: run { i = len; continue@parseLoop }
                    i = end
                    val lat = attrs["lat"]?.toDoubleOrNull() ?: continue@parseLoop
                    val lon = attrs["lon"]?.toDoubleOrNull() ?: continue@parseLoop
                    navaids += Navaid(
                        id = attrs["id"].orEmpty(),
                        name = attrs["n"].orEmpty(),
                        kind = attrs["t"].orEmpty(),
                        detail = attrs["d"].orEmpty(),
                        latitude = lat,
                        longitude = lon
                    )
                }
                "Airspace" -> {
                    val (attrs, end) = parseTagAttrsAndEnd(xml, i) ?: run { i = len; continue@parseLoop }
                    i = end
                    val closeTag = "</Airspace>"
                    val closeIdx = xml.indexOf(closeTag, i)
                    val bodyEnd = if (closeIdx < 0) len else closeIdx
                    val points = parsePoints(xml, i, bodyEnd)
                    i = if (closeIdx < 0) len else closeIdx + closeTag.length
                    if (points.size < 3) continue@parseLoop
                    airspaces += Airspace(
                        id = attrs["id"].orEmpty(),
                        name = attrs["n"].orEmpty(),
                        kind = attrs["t"].orEmpty().ifEmpty { attrs["d"].orEmpty() },
                        lowerLimitMeters = attrs["lowM"]?.toIntOrNull(),
                        lowerLimitReference = attrs["lowRef"],
                        upperLimitMeters = attrs["upM"]?.toIntOrNull(),
                        upperLimitReference = attrs["upRef"],
                        points = points
                    )
                }
                else -> {
                    // skip to end of this tag (self-closing or open)
                    i = xml.indexOf('>', i).let { if (it < 0) len else it + 1 }
                }
            }
        }

        return ParsedNavSnapshot(airports = airports, navaids = navaids, airspaces = airspaces)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Starting from [from] (just after the tag name), parse all attributes
     * up to and including the closing `>` or `/>`.
     * Returns (attrMap, indexAfterClose) or null on malformed input.
     */
    private fun parseTagAttrsAndEnd(xml: String, from: Int): Pair<Map<String, String>, Int>? {
        val len = xml.length
        val attrs = mutableMapOf<String, String>()
        var i = from

        while (i < len) {
            // skip whitespace
            while (i < len && xml[i].isWhitespace()) i++
            if (i >= len) return null

            val c = xml[i]
            if (c == '>') return attrs to (i + 1)
            if (c == '/') {
                // self-closing
                val gt = xml.indexOf('>', i)
                return attrs to (if (gt < 0) len else gt + 1)
            }

            // read attribute name
            val keyStart = i
            while (i < len && xml[i] != '=' && xml[i] != '>' && !xml[i].isWhitespace()) i++
            val key = xml.substring(keyStart, i)

            while (i < len && xml[i].isWhitespace()) i++
            if (i >= len || xml[i] != '=') continue
            i++ // skip '='
            while (i < len && xml[i].isWhitespace()) i++
            if (i >= len) return null

            val quote = xml[i]
            if (quote != '"' && quote != '\'') continue
            i++ // skip opening quote
            val valueStart = i
            while (i < len && xml[i] != quote) i++
            val rawValue = xml.substring(valueStart, i)
            if (i < len) i++ // skip closing quote

            attrs[key] = unescapeXml(rawValue)
        }
        return null
    }

    private fun parsePoints(xml: String, from: Int, to: Int): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var i = from
        while (i < to) {
            val lt = xml.indexOf('<', i)
            if (lt < 0 || lt >= to) break
            i = lt + 1
            if (i >= to) break
            if (xml[i] == '/') { i = xml.indexOf('>', i).let { if (it < 0) to else it + 1 }; continue }

            // check tag name
            val nameEnd = run {
                var j = i; while (j < to && xml[j] != ' ' && xml[j] != '/' && xml[j] != '>') j++; j
            }
            if (xml.substring(i, nameEnd) != "P") {
                i = xml.indexOf('>', nameEnd).let { if (it < 0) to else it + 1 }
                continue
            }
            i = nameEnd
            val (attrs, end) = parseTagAttrsAndEnd(xml, i) ?: break
            i = end
            val lat = attrs["lat"]?.toDoubleOrNull() ?: continue
            val lon = attrs["lon"]?.toDoubleOrNull() ?: continue
            points += GeoPoint(latitude = lat, longitude = lon)
        }
        return points
    }

    /** Single-pass XML entity unescaping. */
    private fun unescapeXml(s: String): String {
        val amp = s.indexOf('&')
        if (amp < 0) return s // fast path: nothing to unescape

        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c != '&') { sb.append(c); i++; continue }
            val semi = s.indexOf(';', i + 1)
            if (semi < 0) { sb.append(c); i++; continue }
            when (s.substring(i, semi + 1)) {
                "&amp;"  -> sb.append('&')
                "&lt;"   -> sb.append('<')
                "&gt;"   -> sb.append('>')
                "&quot;" -> sb.append('"')
                "&apos;" -> sb.append('\'')
                else     -> sb.append(s, i, semi + 1)
            }
            i = semi + 1
        }
        return sb.toString()
    }
}

internal data class ParsedNavSnapshot(
    val airports: List<Airport>,
    val navaids: List<Navaid>,
    val airspaces: List<Airspace>
)
