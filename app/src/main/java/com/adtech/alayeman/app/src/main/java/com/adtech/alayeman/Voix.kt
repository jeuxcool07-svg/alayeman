package com.adtech.alayeman

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

class Voix(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private val prefs = context.getSharedPreferences("alayeman", Context.MODE_PRIVATE)
    private var pret = false
    private var voix: List<Voice> = emptyList()
    private var index = 0
    var active = true

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val r = tts?.setLanguage(Locale.FRENCH)
        pret = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        voix = (tts?.voices ?: emptySet<Voice>())
            .filter { it.locale.language == "fr" }
            .sortedBy { it.name }
        if (voix.isNotEmpty()) {
            index = prefs.getInt("voix", 0).coerceIn(0, voix.size - 1)
            tts?.voice = voix[index]
        }
    }

    fun voixSuivante(): String {
        if (!pret || voix.isEmpty()) return "Aucune voix française trouvée"
        index = (index + 1) % voix.size
        prefs.edit().putInt("voix", index).apply()
        tts?.voice = voix[index]
        tts?.setPitch(0.9f)
        tts?.setSpeechRate(1.0f)
        tts?.speak("Salut, c'est moi, Alayeman.", TextToSpeech.QUEUE_FLUSH, null, "essai")
        return "Voix ${index + 1} sur ${voix.size}"
    }

    fun parler(texte: String, humeur: Humeur) {
        if (!pret || !active) return
        val propre = texte.replace(Regex("[^\\p{L}\\p{N}\\p{P}\\s]"), "").trim()
        if (propre.isEmpty()) return

        val (facteur, vitesse) = when (humeur) {
            Humeur.PANIQUE -> Pair(1.25f, 1.3f)
            Humeur.STRESSE -> Pair(1.1f, 1.15f)
            Humeur.ENERVE -> Pair(0.85f, 1.2f)
            Humeur.TRISTE -> Pair(0.85f, 0.8f)
            Humeur.BOUDEUR -> Pair(0.9f, 0.85f)
            Humeur.JOYEUX -> Pair(1.05f, 1.05f)
        }
        tts?.setPitch(0.9f * facteur)
        tts?.setSpeechRate(vitesse)
        tts?.speak(propre, TextToSpeech.QUEUE_FLUSH, null, "alayeman")
    }

    fun arreter() {
        tts?.stop()
        tts?.shutdown()
    }
}
