package com.adtech.alayeman

import android.os.Handler
import android.os.Looper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Vision(private val cle: String) {

    private val MODELES = listOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite"
    )

    private val principal = Handler(Looper.getMainLooper())

    private val ame = "Tu es Alayeman, un compagnon vivant qui habite le téléphone " +
        "d'Alaye. Tu as un vrai caractère : tu peux t'énerver, bouder, rigoler et dire " +
        "franchement ce que tu penses. Tu parles en français, de façon familière, avec des " +
        "phrases courtes. Si tu vois des personnes, tu ne cherches jamais à les identifier."

    private fun appeler(modele: String, corps: JSONObject): Pair<Int, String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modele:generateContent"
            val c = URL(url).openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.doOutput = true
            c.connectTimeout = 20000
            c.readTimeout = 45000
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("x-goog-api-key", cle)
            c.outputStream.use { it.write(corps.toString().toByteArray()) }
            val code = c.responseCode
            val flux = if (code in 200..299) c.inputStream else c.errorStream
            val brut = flux?.bufferedReader()?.use { it.readText() } ?: ""
            c.disconnect()
            Pair(code, brut)
        } catch (e: Exception) {
            Pair(-1, e.message ?: "pas de connexion")
        }
    }

    private fun extraire(brut: String): String {
        return try {
            val parts = JSONObject(brut).optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                sb.append(parts.getJSONObject(i).optString("text", ""))
            }
            sb.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun regarder(
        jpeg: ByteArray,
        humeur: Humeur,
        batterie: Int,
        souvenir: String,
        onResultat: (String) -> Unit
    ) {
        thread {
            var reponse = "Je n'arrive pas à voir... réessaie."
            try {
                val image = JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP))
                )
                val texte = JSONObject().put(
                    "text",
                    "Voici ce que ta caméra voit en ce moment. Décris-le en 2 ou 3 phrases " +
                        "courtes, avec ton caractère et ton humeur."
                )
                val contenu = JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(image).put(texte))

                val systeme = "$ame Souvenirs : $souvenir " +
                    "Humeur actuelle : ${humeur.name}. Batterie : $batterie%."
                val corps = JSONObject()
                    .put("systemInstruction", JSONObject().put(
                        "parts", JSONArray().put(JSONObject().put("text", systeme))
                    ))
                    .put("contents", JSONArray().put(contenu))
                    .put("generationConfig", JSONObject().put("maxOutputTokens", 1024))

                for (modele in MODELES) {
                    val resultat = appeler(modele, corps)
                    val code = resultat.first
                    if (code in 200..299) {
                        val t = extraire(resultat.second)
                        if (t.isNotEmpty()) {
                            reponse = t
                            break
                        }
                    } else {
                        reponse = "Erreur $code ($modele) : " + resultat.second.take(100)
                        if (code != -1 && code != 429 && code != 500 && code != 503 && code != 404) break
                    }
                }
            } catch (e: Exception) {
                reponse = "Problème inattendu : " + (e.message ?: "inconnu")
            }
            principal.post { onResultat(reponse) }
        }
    }
}
