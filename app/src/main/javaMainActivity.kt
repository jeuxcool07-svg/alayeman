package com.adtech.alayeman

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val ame = Ame()
    private val recherche = Recherche()
    private var cerveau: Cerveau? = null
    private lateinit var corps: BatterieCorps
    private lateinit var conversation: TextView
    private lateinit var defilement: ScrollView
    private lateinit var humeur: TextView

    private fun ajouter(texte: String) {
        conversation.append(texte + "\n\n")
        defilement.post { defilement.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun envoyer(saisie: EditText) {
        val message = saisie.text.toString().trim()
        if (message.isEmpty()) return
        val c = cerveau
        if (c == null) {
            ajouter("Alayeman : Donne-moi d'abord ma clé, bouton « Clé ».")
            return
        }
        ajouter("Toi : $message")
        saisie.setText("")
        corps.ressentir(ame)
        humeur.text = "${ame.humeur.emoji} ${ame.humeur.name}"
        c.repondre(message, ame.humeur, corps.niveau()) { ajouter("Alayeman : $it") }
    }

    private fun demanderCle() {
        val champ = EditText(this).apply {
            hint = "Colle ta clé API ici"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Clé API")
            .setView(champ)
            .setPositiveButton("Enregistrer") { _, _ ->
                val cle = champ.text.toString().trim()
                if (cle.isNotEmpty()) {
                    getSharedPreferences("alayeman", MODE_PRIVATE).edit().putString("cle", cle).apply()
                    cerveau = Cerveau(cle)
                    ajouter("Alayeman : Ma clé est là. Parle-moi.")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        corps = BatterieCorps(this)
        val cleSauvee = getSharedPreferences("alayeman", MODE_PRIVATE).getString("cle", "") ?: ""
        if (cleSauvee.isNotBlank()) cerveau = Cerveau(cleSauvee)

        corps.ressentir(ame)
        humeur = TextView(this).apply {
            textSize = 20f
            setTextColor(Color.GREEN)
            gravity = Gravity.CENTER
            text = "${ame.humeur.emoji} ${ame.humeur.name}"
        }

        conversation = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(8, 16, 8, 16)
            text = "Alayeman : ${ame.parler()}\n\n"
        }

        defilement = ScrollView(this).apply { addView(conversation) }

        val saisie = EditText(this).apply {
            hint = "Écris à Alayeman..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        val poids = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        val boutonEnvoyer = Button(this).apply {
            text = "Envoyer"
            layoutParams = poids
            setOnClickListener { envoyer(saisie) }
        }
        val boutonChercher = Button(this).apply {
            text = "Cherche"
            layoutParams = poids
            setOnClickListener {
                ajouter("Alayeman : Je cherche...")
                recherche.decouvrir { ajouter("Alayeman : $it") }
            }
        }
        val boutonCle = Button(this).apply {
            text = "Clé"
            layoutParams = poids
            setOnClickListener { demanderCle() }
        }

        val rangee = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(boutonEnvoyer)
            addView(boutonChercher)
            addView(boutonCle)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(32, 64, 32, 32)
            addView(humeur)
            addView(defilement, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(saisie)
            addView(rangee)
        }

        setContentView(layout)
    }
}
