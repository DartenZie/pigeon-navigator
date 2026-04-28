package cz.miroslavpasek.pigeonnavigator.data.aviation.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class NavSnapshotParserTest {
    private val parser = NavSnapshotParser()

    @Test
    fun parse_extractsAirportsNavaidsAndAirspaces() {
        val xml = """
            <NavSnapshot>
              <Airports>
                <Airport id="LKAA" d="AD" n="Airport A" lat="50.10" lon="14.30" elevM="200">
                  <Runways>
                    <Runway n="12/30" lenM="900" widM="30" comp="ASPH" prep="GOOD"/>
                  </Runways>
                </Airport>
              </Airports>
              <Navaids>
                <Navaid id="N1" t="VOR" d="ENR" n="Navaid 1" freq="115.9" lat="50.20" lon="14.20"/>
              </Navaids>
              <Airspaces>
                <Airspace id="ASP1" d="ATZ" n="Airspace 1" t="ATZ" lowM="0" lowRef="AGL" upM="3000" upRef="STD">
                  <Poly>
                    <P lat="50.00" lon="14.00"/>
                    <P lat="50.30" lon="14.00"/>
                    <P lat="50.30" lon="14.30"/>
                    <P lat="50.00" lon="14.30"/>
                  </Poly>
                  <BBox minLat="50.00" minLon="14.00" maxLat="50.30" maxLon="14.30"/>
                </Airspace>
              </Airspaces>
            </NavSnapshot>
        """.trimIndent()

        val parsed = parser.parse(xml)

        assertEquals(1, parsed.airports.size)
        assertEquals("LKAA", parsed.airports.first().id)
        assertEquals(1, parsed.navaids.size)
        assertEquals("N1", parsed.navaids.first().id)
        assertEquals("115.9", parsed.navaids.first().frequency)
        assertEquals(1, parsed.airspaces.size)
        assertEquals(4, parsed.airspaces.first().points.size)
    }
}
