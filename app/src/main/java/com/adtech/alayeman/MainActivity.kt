package com.adtech.alayeman

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val ame = Ame()
    private val recherche = Recherche()
    private var cerveau: Cerveau? = null
    private var voix: Voix? = null
    private var ecoute: Ecoute? = null
    private var cle = ""
    private var uriPhoto: Uri? = null
    private lateinit var memoire: Memoire
    private var souvenir = ""
    private lateinit var corps: BatterieCorps
    private lateinit var conversation: TextView
    private lateinit var defilement: ScrollView
    private lateinit var humeur: TextView

    private fun poids() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun bouton(titre: String, action: () -> Unit): Button = Button(this).apply {
        text = titre
        textSize = 12f
        layoutParams = poids()
        setOnClickListener { action() }
    }

    private fun ajouter(texte: String) {
        conversation.append(texte + "\n\n")
        defilement.post { defilement.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun lancerVeille() {
        try {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            val i = Intent(this, Veilleur::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        } catch (e: Exception) {
            Log.e("Alayeman", "Veille impossible", e)
        }
    }

    private fun envoyer(saisie: EditText) {
        val message = saisie.text.toString().trim()
        if (message.isEmpty()) return
        val c = cerveau
        if (c == null) {
            ajouter("Alayeman : Donne-moi d'abord ma clé, bouton « Clé ».")
            return
        }

        val nom = Regex("(?i)^je m'appelle\\s+(\\p{L}+)").find(message)
        if (nom != null) {
            memoire.definirPrenom(nom.groupValues[1])
            souvenir = memoire.souvenir()
            c.souvenir = souvenir
        }

        ajouter("Toi : $message")
        saisie.setText("")
        corps.ressentir(ame)
        humeur.text = "${ame.humeur.emoji} ${ame.humeur.name}"
        c.repondre(message, ame.humeur, corps.niveau()) {
            ajouter("Alayeman : $it")
            voix?.parler(it, ame.humeur)
        }
    }

    private fun regarder() {
        if (cle.isBlank()) {
            ajouter("Alayeman : Donne-moi d'abord ma clé, bouton « Clé ».")
            return
        }
        try {
            val valeurs = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "alayeman_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            uriPhoto = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valeurs)
            val intention = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intention.putExtra(MediaStore.EXTRA_OUTPUT, uriPhoto)
            intention.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivityForResult(intention, 3)
        } catch (e: Exception) {
            ajouter("Alayeman : Je n'arrive pas à ouvrir l'appareil photo.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 3) return
        val uri = uriPhoto ?: return
        if (resultCode != RESULT_OK) {
            try { contentResolver.delete(uri, null, null) } catch (e: Exception) { }
            return
        }
        ajouter("Alayeman : Je regarde...")
        thread {
            try {
                val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bornes) }
                var echelle = 1
                while (maxOf(bornes.outWidth, bornes.outHeight) / echelle > 1280) echelle *= 2
                val reglages = BitmapFactory.Options().apply { inSampleSize = echelle }
                val image: Bitmap? = contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, reglages)
                }
                if (image == null) {
                    runOnUiThread { ajouter("Alayeman : Je n'arrive pas à lire la photo.") }
                    return@thread
                }
                val sortie = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 80, sortie)
                try { contentResolver.delete(uri, null, null) } catch (e: Exception) { }
                Vision(cle).regarder(sortie.toByteArray(), ame.humeur, corps.niveau(), souvenir) {
                    ajouter("Alayeman : $it")
                    voix?.parler(it, ame.humeur)
                }
            } catch (e: Exception) {
                runOnUiThread { ajouter("Alayeman : Problème avec la photo : ${e.message}") }
            }
        }
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
                val nouvelle = champ.text.toString().trim()
                if (nouvelle.isNotEmpty()) {
                    getSharedPreferences("alayeman", MODE_PRIVATE).edit().putString("cle", nouvelle).apply()
                    cle = nouvelle
                    cerveau = Cerveau(nouvelle).also { it.souvenir = souvenir }
                    ajouter("Alayeman : Ma clé est là. Parle-moi.")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun construire() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        corps = BatterieCorps(this)
        voix = Voix(this)
        ecoute = Ecoute(this)
        memoire = Memoire(this)

        souvenir = memoire.souvenir()
        val humeurAbsence = memoire.humeurAbsence()
        memoire.marquerVisite()

        cle = getSharedPreferences("alayeman", MODE_PRIVATE).getString("cle", "") ?: ""
        if (cle.isNotBlank()) cerveau = Cerveau(cle).also { it.souvenir = souvenir }

        corps.ressentir(ame)
        if (humeurAbsence != null) ame.changerHumeur(humeurAbsence)

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

        val rangee1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(bouton("Envoyer") { envoyer(saisie) })
            addView(bouton("Micro") {
                Toast.makeText(this@MainActivity, "Je t'écoute...", Toast.LENGTH_SHORT).show()
                ecoute?.ecouter(
                    { texte ->
                        saisie.setText(texte)
                        envoyer(saisie)
                    },
                    { erreur -> ajouter("Alayeman : $erreur") }
                )
            })
            addView(bouton("Regarde") { regarder() })
        }

        val rangee2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(bouton("Cherche") {
                ajouter("Alayeman : Je cherche...")
                recherche.decouvrir { ajouter("Alayeman : $it") }
            })
            addView(bouton("Voix") {
                val info = voix?.voixSuivante() ?: "Voix indisponible"
                Toast.makeText(this@MainActivity, info, Toast.LENGTH_SHORT).show()
            })
            addView(bouton("Clé") { demanderCle() })
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(32, 64, 32, 32)
            addView(humeur)
            addView(defilement, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(saisie)
            addView(rangee1)
            addView(rangee2)
        }

        setContentView(layout)
        lancerVeille()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            construire()
        } catch (e: Throwable) {
            val erreur = TextView(this).apply {
                setTextColor(Color.RED)
                setBackgroundColor(Color.BLACK)
                setPadding(32, 64, 32, 32)
                text = "ERREUR : " + Log.getStackTraceString(e).take(1500)
            }
            setContentView(ScrollView(this).apply { addView(erreur) })
        }
    }

    override fun onDestroy() {
        ecoute?.arreter()
        voix?.arreter()
        super.onDestroy()
    }
}
