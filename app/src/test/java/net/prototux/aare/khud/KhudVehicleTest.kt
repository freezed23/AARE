package net.prototux.aare.khud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KhudVehicleTest {
    @Test
    fun parsesTheKhudCarlinkPayload() {
        val v = KhudVehicle.parse("""{"schema":"khud.vehicle","v":1,"link":true,"gear":"R",
            "hvac":{"off":false,"driverTemp10":225,"passengerTemp10":null,"auto":true,"ac":true,"recirc":false,"defrost":false,"fan":4,"mode":3},
            "hvacPopup":true,"parking":{"front":[0,1,2],"rear":[3,0,1]},"blinkLeft":null,"blinkRight":null,"wiper":null}""")!!
        assertTrue(v.link)
        assertEquals("R", v.gear)
        assertEquals(225, v.hvac!!.driverTemp10)
        assertNull(v.hvac!!.passengerTemp10)
        assertEquals(4, v.hvac!!.fan)
        assertTrue(v.hvacPopup)
        assertEquals(listOf(3, 0, 1), v.parking!!.rear)
        assertTrue(v.parking!!.any)
        assertNull(v.blinkLeft)
        assertNull(v.wiper)
    }

    @Test
    fun linkDownAndForeignPayloads() {
        val down = KhudVehicle.parse("""{"schema":"khud.vehicle","v":1,"link":false,"gear":null,"hvac":null,"hvacPopup":false,"parking":null}""")!!
        assertFalse(down.link)
        assertNull(down.gear)
        assertNull(KhudVehicle.parse("""{"schema":"other","v":1}"""))
        assertNull(KhudVehicle.parse("""{"schema":"khud.vehicle","v":2}"""))
        assertNull(KhudVehicle.parse("not json"))
    }
}
