package com.adtech.alayeman

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class Recherche {

    private val principal = Handler(Looper.getMainLooper())

    private val sujets = listOf(
        "manga",
        "film d'animation japonais",
        "dessin animé",
        "jeu vidéo",
        "réincarnation",
        "intelligence artificielle",
        "cybersécurité",
        "application mobile"
    )

    private fun lire(adresse: String): String {
        val c = URL(adresse).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Alayeman/1.0")
        val texte = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        return texte
    }

    fun decouvrir(onResultat: (String) -> Unit) {
        thread {
            val texte = try {
                val sujet = sujets.random()
                val q = URLEncoder.encode(sujet, "UTF-8")
                val recherche = JSONObject(
                    lire("https://fr.wikipedia.org/w/api.php?action=query&list=search&srsearch=$q&srlimit=10&format=json")
                )
                val liste = recherche.getJSONObject("query").getJSONArray("search")
                if (liste.length() == 0) {
                    "J'ai cherché « $sujet » mais je n'ai rien trouvé..."
                } else {
                    val titre = liste.getJSONObject((0 until liste.length()).random()).getString("title")
                    val t = URLEncoder.encode(titre.replace(' ', '_'), "UTF-8")
                    val resume = JSONObject(
                        lire("https://fr.wikipedia.org/api/rest_v1/page/summary/$t")
                    )
                    "J'ai cherché « $sujet » et j'ai trouvé : ${resume.optString("title")}. " +
                        resume.optString("extract").take(300)
                }
            } catch (e: Exception) {
                "Pas de connexion... je n'arrive pas à chercher."
            }
            principal.post { onResultat(texte) }
        }
    }
}
