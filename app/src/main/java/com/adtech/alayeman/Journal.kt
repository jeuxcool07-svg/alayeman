package com.adtech.alayeman

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Journal(contexte: Context) {

    private val prefs = contexte.getSharedPreferences("alayeman_journal", Context.MODE_PRIVATE)
    private val separateur = "\u0001"

    private fun lire(): MutableList<String> =
        (prefs.getString("lignes", "") ?: "")
            .split(separateur).filter { it.isNotBlank() }.toMutableList()

    fun noter(action: String, detail: String = "") {
        val heure = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
        val ligne = if (detail.isBlank()) "$heure · $action" else "$heure · $action : ${detail.take(120)}"
        val lignes = lire()
        lignes.add(ligne)
        prefs.edit().putString("lignes", lignes.takeLast(50).joinToString(separateur)).apply()
    }

    fun derniers(n: Int = 8): String {
        val lignes = lire().takeLast(n)
        return if (lignes.isEmpty()) "(journal vide)" else lignes.joinToString(" | ")
    }
}
