package com.adtech.alayeman

import android.content.Context

class Memoire(context: Context) {

    private val prefs = context.getSharedPreferences("alayeman_memoire", Context.MODE_PRIVATE)

    fun visites(): Int = prefs.getInt("visites", 0)

    fun prenom(): String = prefs.getString("prenom", "Alaye") ?: "Alaye"

    fun definirPrenom(nouveau: String) {
        if (nouveau.isNotBlank()) prefs.edit().putString("prenom", nouveau.trim()).apply()
    }

    fun heuresAbsence(): Long {
        val derniere = prefs.getLong("derniere", 0L)
        if (derniere == 0L) return -1
        return (System.currentTimeMillis() - derniere) / 3600000
    }

    fun marquerVisite() {
        prefs.edit()
            .putLong("derniere", System.currentTimeMillis())
            .putInt("visites", visites() + 1)
            .apply()
    }

    fun humeurAbsence(): Humeur? {
        val h = heuresAbsence()
        return when {
            h >= 48 -> Humeur.TRISTE
            h >= 24 -> Humeur.BOUDEUR
            else -> null
        }
    }

    fun souvenir(): String {
        val h = heuresAbsence()
        val base = when {
            h < 0 -> "C'est la toute première fois que vous vous parlez."
            h < 1 -> "Vous vous êtes parlé il y a moins d'une heure."
            h < 24 -> "${prenom()} t'a laissé seul pendant $h heures."
            else -> "${prenom()} t'a abandonné pendant ${h / 24} jours."
        }
        return "$base Il a ouvert l'appli ${visites()} fois en tout. Il s'appelle ${prenom()}."
    }
}
