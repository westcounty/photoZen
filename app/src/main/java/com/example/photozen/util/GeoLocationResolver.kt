package com.example.photozen.util

import android.content.Context
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PhotoEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves geo location for a photo with DB caching.
 *
 * Flow:
 * 1. If photo already has lat/lng in DB → resolve directly
 * 2. If not yet GPS-scanned → read EXIF, write result back to DB, then resolve
 * 3. If already scanned but no GPS → return null (skip EXIF IO)
 */
@Singleton
class GeoLocationResolver @Inject constructor(
    private val offlineGeocoder: OfflineGeocoder,
    private val photoDao: PhotoDao
) {
    /**
     * Resolve location text ("省份 · 城市") for a photo, caching GPS to DB.
     */
    suspend fun resolveText(photo: PhotoEntity): String? {
        val (lat, lng) = resolveGps(photo) ?: return null
        return offlineGeocoder.getLocationText(lat, lng)
    }

    /**
     * Resolve LocationDetails (province, city) for a photo, caching GPS to DB.
     */
    suspend fun resolveDetails(photo: PhotoEntity): LocationDetails? {
        val (lat, lng) = resolveGps(photo) ?: return null
        return offlineGeocoder.getLocationDetails(lat, lng)
    }

    /**
     * Core: get GPS coordinates, reading EXIF if needed and persisting to DB.
     */
    private suspend fun resolveGps(photo: PhotoEntity): Pair<Double, Double>? {
        // Already have coordinates in DB
        if (photo.latitude != null && photo.longitude != null) {
            return Pair(photo.latitude, photo.longitude)
        }

        // Already scanned EXIF before and found nothing
        if (photo.gpsScanned) return null

        // Read EXIF
        val gps = offlineGeocoder.extractGpsFromUri(photo.systemUri)
        if (gps != null) {
            photoDao.updateGpsLocation(photo.id, gps.first, gps.second)
        } else {
            photoDao.markGpsScanned(photo.id)
        }
        return gps
    }

    companion object {
        /**
         * Obtain an instance from any Android Context (for Composables / non-DI code).
         */
        fun from(context: Context): GeoLocationResolver {
            return EntryPointAccessors.fromApplication(
                context.applicationContext,
                GeoLocationResolverEntryPoint::class.java
            ).geoLocationResolver()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeoLocationResolverEntryPoint {
    fun geoLocationResolver(): GeoLocationResolver
}
