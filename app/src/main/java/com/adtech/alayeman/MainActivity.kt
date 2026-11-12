package com.adtech.alayeman

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val ame = Ame()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val corps = BatterieCorps(this)

        val texte = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.GREEN)
            gravity = Gravity.CENTER
            text = corps.ressentir(ame)
        }

        val bouton = Button(this).apply {
            text = "Parler à Alayeman"
            setOnClickListener { texte.text = corps.ressentir(ame) }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(48, 48, 48, 48)
            addView(texte)
            addView(bouton)
        }

        setContentView(layout)
    }
}
