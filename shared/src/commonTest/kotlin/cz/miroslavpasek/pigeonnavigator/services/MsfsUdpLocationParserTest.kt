package cz.miroslavpasek.pigeonnavigator.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MsfsUdpLocationParserTest {

    @Test
    fun `parseMsfsUdpLocationPacket parses valid payload`() {
        val location = parseMsfsUdpLocationPacket("XGPSMSFS,14.4378,50.0755,412.3,271.9,66.2")

        assertNotNull(location)
        assertEquals(50.0755, location.latitude)
        assertEquals(14.4378, location.longitude)
        assertEquals(412.3, location.altitudeMeters)
        assertEquals(271.9f, location.bearingDegrees)
        assertEquals(66.2f, location.speedMetersPerSecond)
        assertEquals(null, location.horizontalAccuracyMeters)
        assertEquals(false, location.requiresPermission)
    }

    @Test
    fun `parseMsfsUdpLocationPacket returns null for invalid prefix`() {
        val location = parseMsfsUdpLocationPacket("XGPS,14.4378,50.0755,412.3,271.9,66.2")

        assertNull(location)
    }

    @Test
    fun `parseMsfsUdpLocationPacket returns null for malformed fields`() {
        val location = parseMsfsUdpLocationPacket("XGPSMSFS,14.4378,not-a-number,412.3,271.9,66.2")

        assertNull(location)
    }
}
