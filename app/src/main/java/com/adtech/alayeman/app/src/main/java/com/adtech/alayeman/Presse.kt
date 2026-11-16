package com.adtech.alayeman

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Presse(private val activite: Activity) {

    private val prefs = activite.getSharedPreferences("alayeman_presse", Context.MODE_PRIVATE)
    private val separateur = "\u0001"

    // Lit le presse-papiers (Android n'autorise que l'appli au premier plan)
    fun lire(): String? {
        return try {
            val gestionnaire = activite.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (!gestionnaire.hasPrimaryClip()) return null
            val element = gestionnaire.primaryClip?.getItemAt(0) ?: return null
            val texte = element.coerceToText(activite)?.toString()?.trim()
            if (texte.isNullOrEmpty()) null else texte
        } catch (e: Exception) {
            null
        }
    }

    // Repère les contenus qu'on ne doit pas envoyer à l'IA : codes, mots de passe, cartes
    fun sensible(texte: String): Boolean {
        val compact = texte.trim()
        if (Regex("^\\d{4,8}$").matches(compact)) return true
        if (Regex("(?:\\d[ -]?){13,19}").containsMatchIn(compact)) return true
        if (Regex("(?i)(mot de passe|password|passcode|\\bpin\\b|code secret|\\biban\\b)")
                .containsMatchIn(compact)) return true
        return false
    }

    fun apercu(texte: String, max: Int = 1200): String = texte.take(max)

    // Garde le texte en local, sur le téléphone seulement (20 notes maximum)
    fun garder(texte: String) {
        val heure = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
        val notes = notes().toMutableList()
        notes.add("$heure · ${texte.take(500)}")
        prefs.edit().putString("notes", notes.takeLast(20).joinToString(separateur)).apply()
    }

    fun notes(): List<String> =
        (prefs.getString("notes", "") ?: "").split(separateur).filter { it.isNotBlank() }
}
