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

    private val PREFERES = listOf(
        "llama-3.3-70b-versatile",
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "llama-3.1-8b-instant",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "qwen/qwen3-32b"
    )
    private val EXCLUS = listOf(
        "whisper", "guard", "tts", "playai", "orpheus", "embed", "safeguard", "prompt"
    )

    private var modelesChoisis: List<String>? = null

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

    private fun lister(): List<String> {
        return try {
            val c = URL("https://api.groq.com/openai/v1/models").openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 15000
            c.setRequestProperty("Authorization", "Bearer $cle")
            val code = c.responseCode
            if (code !in 200..299) {
                c.disconnect()
                return emptyList()
            }
            val brut = c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            val donnees = JSONObject(brut).optJSONArray("data") ?: return emptyList()
            val ids = ArrayList<String>()
            for (i in 0 until donnees.length()) {
                val id = donnees.getJSONObject(i).optString("id", "")
                if (id.isNotBlank() && EXCLUS.none { id.lowercase().contains(it) }) ids.add(id)
            }
            val choisis = ArrayList<String>()
            for (p in PREFERES) if (ids.contains(p)) choisis.add(p)
            for (id in ids) if (!choisis.contains(id) && choisis.size < 4) choisis.add(id)
            choisis.take(4)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun modeles(): List<String> {
        val deja = modelesChoisis
        if (deja != null && deja.isNotEmpty()) return deja
        val liste = lister()
        val resultat = if (liste.isNotEmpty()) liste else listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant")
        if (liste.isNotEmpty()) modelesChoisis = liste
        return resultat
    }

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
            val texte = JSONObject(brut).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")
                ?.optString("content", "") ?: ""
            texte.replace(Regex("(?s)<think>.*?</think>"), "").trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun repondre(texte: String, humeur: Humeur, batterie: Int, onResultat: (String) -> Unit) {
        thread {
            var reponse: String
            try {
                if (cle.isBlank()) {
                    principal.post {
                        onResultat("Ma clé n'est pas dans cet APK (secret GROQ_KEY manquant ou mal transmis).")
                    }
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
                    .put("max_tokens", 600)
                    .put("temperature", 0.9)

                var derniere = "Je n'arrive pas à te répondre... réessaie."
                var trouve: String? = null

                for (modele in modeles()) {
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
                        derniere = "Ma clé est refusée (erreur 401). Vérifie le secret GROQ_KEY."
                        break
                    } else if (code == -1) {
                        derniere = "Pas de connexion... je n'arrive pas à te répondre."
                    } else {
                        derniere = "Erreur $code ($modele) : " + resultat.second.take(200)
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
