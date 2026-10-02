package com.atharok.barcodescanner.domain.library.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoTorchControllerTest {

    private var clock = 0L
    private fun controller(hasFlash: Boolean = true) = AutoTorchController(hasFlash, now = { clock })

    @Test fun `sustained darkness turns the torch on after the debounce`() {
        val c = controller()
        assertNull(c.onLuma(10))
        clock += 1000
        assertNull(c.onLuma(10))
        clock += 500
        assertEquals(true, c.onLuma(10))
    }

    @Test fun `sustained brightness turns the torch off after the debounce`() {
        val c = controller()
        c.onLuma(150)
        clock += 1500
        assertEquals(false, c.onLuma(150))
    }

    @Test fun `a mid-range frame restarts the debounce, so readings near a threshold don't flicker`() {
        val c = controller()
        c.onLuma(10)
        clock += 1000
        c.onLuma(75)
        clock += 1000
        assertNull(c.onLuma(10))
        clock += 1500
        assertEquals(true, c.onLuma(10))
    }

    @Test fun `manual toggle disables auto behavior for the session`() {
        val c = controller()
        c.onManualTorchToggle()
        c.onLuma(10)
        clock += 5000
        assertNull(c.onLuma(10))
    }

    @Test fun `device without a flash never requests a change`() {
        val c = controller(hasFlash = false)
        c.onLuma(1)
        clock += 5000
        assertNull(c.onLuma(1))
    }
}
