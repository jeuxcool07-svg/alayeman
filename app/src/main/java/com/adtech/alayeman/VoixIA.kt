package com.adtech.alayeman

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class VoixIA(
    private val contexte: Context,
    private val cle: String,
    private val telephone: Voix
) {

    private val MODELE = "gemini-3.8-flash-lite-tts"
    private val VOIX = listOf("Puck", "Fenrir", "Achird", "Sadachbia", "Orus", "Charon")
    private val STYLE = "clear, natural and warm, a young man"

    private val prefs = contexte.getSharedPreferences("alayeman", Context.MODE_PRIVATE)
    // index = VOIX.size signifie « voix du téléphone »
    private var index = prefs.getInt("voix_ia", 0).coerceIn(0, VOIX.size)
    private val principal = Handler(Looper.getMainLooper())
    private var lecteur: MediaPlayer? = null
    var active = true

    private fun nomChoix(): String =
        if (index == VOIX.size) "Voix du téléphone" else "Voix Google ${VOIX[index]}"

    fun voixSuivante(): String {
        index = (index + 1) % (VOIX.size + 1)
        prefs.edit().putInt("voix_ia", index).apply()
        parler("Salut, c'est moi, Alayeman.", Humeur.JOYEUX)
        return "${nomChoix()} (${index + 1}/${VOIX.size + 1})"
    }

    fun parler(texte: String, humeur: Humeur) {
        if (!active) return
        val propre = texte.replace(Regex("[^\\p{L}\\p{N}\\p{P}\\s]"), "").trim().take(400)
        if (propre.isEmpty()) return

        // Voix du téléphone : instantanée, toujours le même ton
        if (index == VOIX.size) {
            telephone.parler(propre, Humeur.JOYEUX)
            return
        }

        val voixChoisie = VOIX[index]
        thread {
            val octets = try {
                demander(propre, voixChoisie)
            } catch (e: Exception) {
                null
            }
            principal.post {
                if (octets == null) {
                    Toast.makeText(contexte, "Voix Google indisponible", Toast.LENGTH_SHORT).show()
                } else {
                    jouer(octets)
                }
            }
        }
    }

    private fun demander(texte: String, voix: String): ByteArray? {
        val annotation = JSONObject()
            .put("type", "speech_metadata")
            .put("style", STYLE)
        val contenu = JSONObject()
            .put("type", "text")
            .put("text", texte)
            .put("annotations", JSONArray().put(annotation))
        val entree = JSONObject()
            .put("type", "user_input")
            .put("content", JSONArray().put(contenu))
        val corps = JSONObject()
            .put("model", MODELE)
            .put("input", JSONArray().put(entree))
            .put("response_format", JSONObject().put("type", "audio"))
            .put(
                "generation_config",
                JSONObject().put(
                    "speech_config",
                    JSONArray().put(JSONObject().put("voice", voix))
                )
            )

        val c = URL("https://generativelanguage.googleapis.com/v1beta/interactions")
            .openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("x-goog-api-key", cle)
        c.outputStream.use { it.write(corps.toString().toByteArray()) }

        val code = c.responseCode
        if (code !in 200..299) {
            c.disconnect()
            return null
        }
        val brut = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()

        val etapes = JSONObject(brut).optJSONArray("steps") ?: return null
        var donnees: String? = null
        for (i in 0 until etapes.length()) {
            val etape = etapes.getJSONObject(i)
            if (etape.optString("type") != "model_output") continue
            val contenus = etape.optJSONArray("content") ?: continue
            for (j in 0 until contenus.length()) {
                val elem = contenus.getJSONObject(j)
                if (elem.optString("type") == "audio") donnees = elem.optString("data")
            }
        }
        if (donnees.isNullOrEmpty()) return null
        return Base64.decode(donnees, Base64.DEFAULT)
    }

    private fun jouer(octets: ByteArray) {
        try {
            arreterLecture()
            val fichier = File(contexte.cacheDir, "alayeman_voix.wav")
            fichier.writeBytes(octets)
            val mp = MediaPlayer()
            mp.setDataSource(fichier.absolutePath)
            mp.setOnCompletionListener { fini ->
                fini.release()
                if (lecteur === fini) lecteur = null
            }
            mp.prepare()
            mp.start()
            lecteur = mp
        } catch (e: Exception) {
            Toast.makeText(contexte, "Voix Google indisponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun arreterLecture() {
        lecteur?.release()
        lecteur = null
    }

    fun arreter() {
        arreterLecture()
        telephone.arreter()
    }
}
