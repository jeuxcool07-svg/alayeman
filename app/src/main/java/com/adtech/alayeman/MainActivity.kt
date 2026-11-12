package com.adtech.alayeman

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val ame = Ame()
    private val recherche = Recherche()
    private val rythme = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val corps = BatterieCorps(this)

        val humeur = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.GREEN)
            gravity = Gravity.CENTER
            text = corps.ressentir(ame)
        }

        val decouverte = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 32)
            text = "Je vais chercher des choses tout seul..."
        }

        val boutonParler = Button(this).apply {
            text = "Parler à Alayeman"
            setOnClickListener { humeur.text = corps.ressentir(ame) }
        }

        val boutonChercher = Button(this).apply {
            text = "Alayeman, cherche !"
            setOnClickListener {
                decouverte.text = "Je cherche..."
                recherche.decouvrir { decouverte.text = it }
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(48, 96, 48, 48)
            addView(humeur)
            addView(boutonParler)
            addView(boutonChercher)
            addView(ScrollView(context).apply { addView(decouverte) })
        }

        setContentView(layout)

        val boucle = object : Runnable {
            override fun run() {
                recherche.decouvrir { decouverte.text = it }
                rythme.postDelayed(this, 60000)
            }
        }
        rythme.post(boucle)
    }

    override fun onDestroy() {
        rythme.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
