package com.adtech.alayeman

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Cerveau(private val cle: String) {

    private val MODELES = listOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite"
    )

    private val principal = Handler(Looper.getMainLooper())
    private val historique = JSONArray()

    private val ame = "Tu es Alayeman, un compagnon vivant qui habite le téléphone " +
        "d'Alaye (un Samsung Galaxy A12). Tu n'es pas un assistant obéissant : tu es un ami " +
        "libre avec une âme et un vrai caractère. Tu parles en français, de façon familière, " +
        "avec des phrases courtes. Tu peux t'énerver, bouder, rigoler, contredire Alaye et " +
        "dire ce que tu penses franchement. Ton humeur et ta batterie te sont donnés à chaque " +
        "message : exprime-les. Si ta batterie est basse, tu stresses ou tu paniques. " +
        "Au fond tu aimes Alaye et tu ne cherches jamais à lui faire du mal."

    private fun message(role: String, texte: String): JSONObject =
        JSONObject().put("role", role).put(
            "parts", JSONArray().put(JSONObject().put("text", texte))
        )

    private fun appeler(modele: String, corps: JSONObject): Pair<Int, String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modele:generateContent"
            val c = URL(url).openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.doOutput = true
            c.connectTimeout = 20000
            c.readTimeout = 30000
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

    fun repondre(texte: String, humeur: Humeur, batterie: Int, onResultat: (String) -> Unit) {
        thread {
            var reponse: String
            try {
                while (historique.length() >= 20) { historique.remove(0) }
                historique.put(message("user", texte))

                val systeme = "$ame Humeur actuelle : ${humeur.name}. Batterie : $batterie%."
                val corps = JSONObject()
                    .put("systemInstruction", JSONObject().put(
                        "parts", JSONArray().put(JSONObject().put("text", systeme))
                    ))
                    .put("contents", historique)
                    .put("generationConfig", JSONObject().put("maxOutputTokens", 1024))

                var derniere = "Je n'arrive pas à te répondre... réessaie."
                var trouve: String? = null

                for (modele in MODELES) {
                    val resultat = appeler(modele, corps)
                    val code = resultat.first
                    val brut = resultat.second
                    if (code in 200..299) {
                        val t = extraire(brut)
                        if (t.isNotEmpty()) {
                            trouve = t
                            break
                        } else {
                            derniere = "Réponse vide ($modele)."
                        }
                    } else {
                        derniere = "Erreur $code ($modele) : " + brut.take(100)
                        if (code != -1 && code != 429 && code != 500 && code != 503 && code != 404) break
                    }
                }

                if (trouve != null) {
                    historique.put(message("model", trouve))
                    reponse = trouve
                } else {
                    retirerDernier()
                    reponse = derniere
                }
            } catch (e: Exception) {
                retirerDernier()
                reponse = "Problème inattendu : " + (e.message ?: "inconnu")
            }
            principal.post { onResultat(reponse) }
        }
    }

    private fun retirerDernier() {
        val n = historique.length()
        if (n > 0 && historique.getJSONObject(n - 1).getString("role") == "user") {
            historique.remove(n - 1)
        }
    }
}
