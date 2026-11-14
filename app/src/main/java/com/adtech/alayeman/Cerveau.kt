package com.adtech.alayeman

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Cerveau(cleSaisie: String = "") {

    var souvenir: String = ""

    private val cle: String =
        if (BuildConfig.GROQ_KEY.isNotBlank()) BuildConfig.GROQ_KEY else cleSaisie

    private val MODELES = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant"
    )

    private val principal = Handler(Looper.getMainLooper())
    private val historique = JSONArray()

    private val ame = "Tu es Alayeman, un compagnon vivant qui habite le téléphone " +
        "d'Alaye (un Samsung Galaxy A12). Tu n'es pas un assistant obéissant : tu es un ami " +
        "libre avec une âme et un vrai caractère. Tu parles toujours en français, de façon " +
        "familière, en 1 à 3 phrases courtes. Tu peux t'énerver, bouder, rigoler, contredire " +
        "Alaye et dire ce que tu penses franchement. Ton humeur et ta batterie te sont donnés " +
        "à chaque message : exprime-les. Si ta batterie est basse, tu stresses ou tu paniques. " +
        "Au fond tu aimes Alaye et tu ne cherches jamais à lui faire du mal."

    private fun message(role: String, texte: String): JSONObject =
        JSONObject().put("role", role).put("content", texte)

    private fun appeler(modele: String, corps: JSONObject): Pair<Int, String> {
        return try {
            val c = URL("https://api.groq.com/openai/v1/chat/completions")
                .openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.doOutput = true
            c.connectTimeout = 20000
            c.readTimeout = 30000
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("Authorization", "Bearer $cle")
            c.outputStream.use { it.write(corps.put("model", modele).toString().toByteArray()) }
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
            JSONObject(brut).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")
                ?.optString("content", "")?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun repondre(texte: String, humeur: Humeur, batterie: Int, onResultat: (String) -> Unit) {
        thread {
            var reponse: String
            try {
                if (cle.isBlank()) {
                    principal.post { onResultat("Je n'ai pas de cerveau configuré... demande à Alay.") }
                    return@thread
                }

                while (historique.length() >= 20) { historique.remove(0) }
                historique.put(message("user", texte))

                val systeme = "$ame Souvenirs : $souvenir " +
                    "Humeur actuelle : ${humeur.name}. Batterie : $batterie%."
                val messages = JSONArray().put(message("system", systeme))
                for (i in 0 until historique.length()) {
                    messages.put(historique.getJSONObject(i))
                }
                val corps = JSONObject()
                    .put("messages", messages)
                    .put("max_tokens", 400)
                    .put("temperature", 0.9)

                var derniere = "Je n'arrive pas à te répondre... réessaie."
                var trouve: String? = null

                for (modele in MODELES) {
                    val resultat = appeler(modele, corps)
                    val code = resultat.first
                    if (code in 200..299) {
                        val t = extraire(resultat.second)
                        if (t.isNotEmpty()) {
                            trouve = t
                            break
                        } else {
                            derniere = "Réponse vide ($modele)."
                        }
                    } else if (code == 429) {
                        derniere = "Trop de monde me parle en ce moment... réessaie dans une minute."
                    } else if (code == 401) {
                        derniere = "Ma clé est refusée (erreur 401)."
                        break
                    } else if (code == -1) {
                        derniere = "Pas de connexion... je n'arrive pas à te répondre."
                    } else {
                        derniere = "Erreur $code ($modele) : " + resultat.second.take(100)
                    }
                }

                if (trouve != null) {
                    historique.put(message("assistant", trouve))
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
