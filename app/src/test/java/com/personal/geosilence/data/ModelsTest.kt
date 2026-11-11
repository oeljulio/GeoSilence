package com.personal.geosilence.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun validCoordinatesAreAccepted() {
        assertTrue(GeofenceZone(latitude = 40.4168, longitude = -3.7038).hasValidLocation())
    }

    @Test
    fun zeroLatitudeIsAccepted() {
        assertTrue(GeofenceZone(latitude = 0.0, longitude = 12.5).hasValidLocation())
    }

    @Test
    fun zeroLongitudeIsAccepted() {
        assertTrue(GeofenceZone(latitude = 41.0, longitude = 0.0).hasValidLocation())
    }

    @Test
    fun defaultCoordinatesAreRejected() {
        assertFalse(GeofenceZone().hasValidLocation())
    }

    @Test
    fun geographicBoundariesAreAccepted() {
        assertTrue(GeofenceZone(latitude = 90.0, longitude = 180.0).hasValidLocation())
        assertTrue(GeofenceZone(latitude = -90.0, longitude = -180.0).hasValidLocation())
    }

    @Test
    fun coordinatesOutsideGeographicBoundsAreRejected() {
        assertFalse(GeofenceZone(latitude = 90.1, longitude = 10.0).hasValidLocation())
        assertFalse(GeofenceZone(latitude = 10.0, longitude = 180.1).hasValidLocation())
    }

    @Test
    fun nonFiniteCoordinatesAreRejected() {
        assertFalse(GeofenceZone(latitude = Double.NaN, longitude = 10.0).hasValidLocation())
        assertFalse(GeofenceZone(latitude = 10.0, longitude = Double.POSITIVE_INFINITY).hasValidLocation())
    }
}
