package com.ivangames.wavebox

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StationRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wavebox_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getFavorites(): List<Station> {
        val json = prefs.getString("favorites", null) ?: return emptyList()
        val type = object : TypeToken<List<Station>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addFavorite(station: Station) {
        val favorites = getFavorites().toMutableList()
        if (favorites.none { it.url == station.url }) {
            favorites.add(station)
            saveFavorites(favorites)
        }
    }

    fun removeFavorite(station: Station) {
        val favorites = getFavorites().toMutableList()
        favorites.removeAll { it.url == station.url }
        saveFavorites(favorites)
    }

    fun isFavorite(station: Station): Boolean {
        return getFavorites().any { it.url == station.url }
    }

    private fun saveFavorites(favorites: List<Station>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString("favorites", json).apply()
    }
}
