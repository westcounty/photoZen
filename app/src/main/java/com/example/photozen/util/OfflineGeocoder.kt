package com.example.photozen.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * City data for offline geocoding.
 */
data class CityData(
    val name: String,
    val province: String,
    val lat: Double,
    val lng: Double
)

/**
 * Offline reverse geocoder using bundled city coordinates.
 * Converts GPS coordinates to province/city text without network access.
 */
@Singleton
class OfflineGeocoder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cities: List<CityData> = emptyList()
    private var isLoaded = false
    private val loadMutex = Mutex()
    
    // LRU cache for recent lookups
    private val cache = object : LinkedHashMap<String, String?>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>?): Boolean {
            return size > 500
        }
    }
    
    /**
     * Load city data from assets.
     */
    private suspend fun ensureLoaded() {
        if (isLoaded) return
        
        loadMutex.withLock {
            if (isLoaded) return
            
            withContext(Dispatchers.IO) {
                try {
                    val jsonString = context.assets.open("china_cities.json")
                        .bufferedReader()
                        .use { it.readText() }
                    
                    val jsonArray = JSONArray(jsonString)
                    val cityList = mutableListOf<CityData>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        cityList.add(
                            CityData(
                                name = obj.getString("name"),
                                province = obj.getString("province"),
                                lat = obj.getDouble("lat"),
                                lng = obj.getDouble("lng")
                            )
                        )
                    }
                    
                    cities = cityList
                    isLoaded = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Get location text from latitude and longitude.
     * Returns format: "省份 · 城市" or null if not found.
     * 
     * @param lat Latitude in degrees
     * @param lng Longitude in degrees
     * @return Location text or null if coordinates are outside China or invalid
     */
    suspend fun getLocationText(lat: Double?, lng: Double?): String? {
        if (lat == null || lng == null) return null
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null
        
        // Check cache first
        val cacheKey = "${lat.format(4)},${lng.format(4)}"
        cache[cacheKey]?.let { return it }
        
        ensureLoaded()
        
        if (cities.isEmpty()) return null
        
        return withContext(Dispatchers.Default) {
            var closestCity: CityData? = null
            var minDistance = Double.MAX_VALUE
            
            for (city in cities) {
                val distance = haversineDistance(lat, lng, city.lat, city.lng)
                if (distance < minDistance) {
                    minDistance = distance
                    closestCity = city
                }
            }
            
            // Only return result if within 100km of a city (to filter out ocean/foreign locations)
            val result = if (minDistance < 100.0 && closestCity != null) {
                val province = closestCity.province
                val city = closestCity.name
                // Avoid redundancy like "北京市 · 北京市"
                if (province == city) {
                    city
                } else {
                    "$province · $city"
                }
            } else {
                null
            }
            
            // Cache the result
            synchronized(cache) {
                cache[cacheKey] = result
            }
            
            result
        }
    }
    
    /**
     * Calculate Haversine distance between two points in kilometers.
     */
    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // Earth radius in km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return r * c
    }
    
    /**
     * Format double to specified decimal places.
     */
    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
}
