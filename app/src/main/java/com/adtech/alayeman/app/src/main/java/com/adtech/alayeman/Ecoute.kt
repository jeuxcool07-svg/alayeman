package com.adtech.alayeman

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class Ecoute(private val activite: Activity) {

    private var reconnaisseur: SpeechRecognizer? = null

    fun disponible(): Boolean = SpeechRecognizer.isRecognitionAvailable(activite)

    private fun autorise(): Boolean =
        activite.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun ecouter(onTexte: (String) -> Unit, onErreur: (String) -> Unit) {
        if (!disponible()) {
            onErreur("La reconnaissance vocale n'est pas disponible sur ce téléphone.")
            return
        }
        if (!autorise()) {
            activite.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 2)
            onErreur("Autorise le micro, puis réappuie sur Micro.")
            return
        }

        arreter()
        reconnaisseur = SpeechRecognizer.createSpeechRecognizer(activite).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onError(error: Int) {
                    onErreur("Je n'ai pas compris (code $error). Réessaie.")
                }

                override fun onResults(results: Bundle?) {
                    val liste = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val texte = liste?.firstOrNull()
                    if (texte.isNullOrBlank()) {
                        onErreur("Je n'ai rien entendu. Réessaie.")
                    } else {
                        onTexte(texte)
                    }
                }
            })
        }

        val intention = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
        }
        reconnaisseur?.startListening(intention)
    }

    fun arreter() {
        reconnaisseur?.destroy()
        reconnaisseur = null
    }
}
