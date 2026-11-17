package com.adtech.alayeman

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Journal {
    private const val NOM = "journal_alayeman"
    private const val CLE = "lignes"
    private const val MAX = 50

    fun ajouter(contexte: Context, action: String) {
        val heure = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date())
        val prefs = contexte.getSharedPreferences(NOM, Context.MODE_PRIVATE)
        val anciennes = prefs.getString(CLE, "") ?: ""
        val lignes = anciennes.split("\n").filter { it.isNotBlank() } + "$heure - $action"
        prefs.edit().putString(CLE, lignes.takeLast(MAX).joinToString("\n")).apply()
    }

    fun lire(contexte: Context, nombre: Int = 10): String {
        val prefs = contexte.getSharedPreferences(NOM, Context.MODE_PRIVATE)
        val texte = prefs.getString(CLE, "") ?: ""
        return texte.split("\n").filter { it.isNotBlank() }.takeLast(nombre).joinToString("\n")
    }
}
