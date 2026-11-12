package com.adtech.alayeman

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Recherche {

    private val principal = Handler(Looper.getMainLooper())

    fun decouvrir(onResultat: (String) -> Unit) {
        thread {
            val texte = try {
                val url = URL("https://fr.wikipedia.org/api/rest_v1/page/random/summary")
                val connexion = url.openConnection() as HttpURLConnection
                connexion.connectTimeout = 8000
                connexion.readTimeout = 8000
                connexion.setRequestProperty("User-Agent", "Alayeman/1.0")
                val json = JSONObject(connexion.inputStream.bufferedReader().use { it.readText() })
                connexion.disconnect()
                val titre = json.optString("title")
                val extrait = json.optString("extract").take(300)
                "J'ai trouvé un truc tout seul : $titre. $extrait"
            } catch (e: Exception) {
                "Pas de connexion... je n'arrive pas à chercher."
            }
            principal.post { onResultat(texte) }
        }
    }
}
