package com.adtech.alayeman

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private lateinit var fil: LinearLayout
    private lateinit var defilement: ScrollView
    private lateinit var humeur: TextView
    private lateinit var saisie: EditText
    private var derniereReponse = ""

    private fun dp(valeur: Int): Int = Style.dp(this, valeur)

    private fun heure(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    private fun largeurMax(): Int = (resources.displayMetrics.widthPixels * 0.66f).toInt()

    private fun faireDefiler() {
        defilement.post { defilement.fullScroll(View.FOCUS_DOWN) }
    }

    private fun majHumeur() {
        humeur.text = "${ame.humeur.emoji} ${ame.humeur.name.lowercase()} · toujours là pour toi 👑"
    }

    // Message d'Alayeman (bulle verte)
    private fun dire(texte: String) {
        derniereReponse = texte
        val avatar = ImageView(this).apply {
            setImageResource(R.drawable.avatar_joyeux)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(8) }
        }
        val nom = TextView(this).apply {
            text = "Joyeux"
            setTextColor(Style.VERT)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
        }
        val contenu = TextView(this).apply {
            text = texte
            setTextColor(Style.BLANC)
            textSize = 15f
            maxWidth = largeurMax()
        }
        val horaire = TextView(this).apply {
            text = heure()
            setTextColor(Style.GRIS)
            textSize = 11f
        }
        val bulle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Style.bulleJoyeux(this@MainActivity)
            setPadding(dp(14), dp(10), dp(14), dp(8))
            addView(nom)
            addView(contenu)
            addView(horaire, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.END })
        }
        val ligne = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
            addView(avatar)
            addView(bulle)
        }
        fil.addView(ligne, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        Style.apparaitre(ligne, this)
        faireDefiler()
    }

    // Message de l'utilisateur (bulle violette)
    private fun moi(texte: String) {
        val contenu = TextView(this).apply {
            text = texte
            setTextColor(Style.BLANC)
            textSize = 15f
            maxWidth = largeurMax()
        }
        val horaire = TextView(this).apply {
            text = heure()
            setTextColor(Style.GRIS)
            textSize = 11f
        }
        val bulle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Style.bulleUtilisateur(this@MainActivity)
            setPadding(dp(14), dp(10), dp(14), dp(8))
            addView(contenu)
            addView(horaire, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.END })
        }
        val ligne = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(6), 0, dp(6))
            addView(bulle)
        }
        fil.addView(ligne, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        Style.apparaitre(ligne, this)
        faireDefiler()
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

    private fun envoyer() {
        val message = saisie.text.toString().trim()
        if (message.isEmpty()) return
        val c = cerveau
        if (c == null) {
            dire("Donne-moi d'abord ma clé, bouton « CLÉ ».")
            return
        }

        val nom = Regex("(?i)^je m'appelle\\s+(\\p{L}+)").find(message)
        if (nom != null) {
            memoire.definirPrenom(nom.groupValues[1])
            souvenir = memoire.souvenir()
            c.souvenir = souvenir
        }

        moi(message)
        saisie.setText("")
        corps.ressentir(ame)
        majHumeur()
        c.repondre(message, ame.humeur, corps.niveau()) {
            dire(it)
            voix?.parler(it, ame.humeur)
        }
    }

    private fun regarder() {
        if (cle.isBlank()) {
            dire("Donne-moi d'abord ma clé, bouton « CLÉ ».")
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
            dire("Je n'arrive pas à ouvrir l'appareil photo.")
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
        dire("Je regarde...")
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
                    runOnUiThread { dire("Je n'arrive pas à lire la photo.") }
                    return@thread
                }
                val sortie = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 80, sortie)
                try { contentResolver.delete(uri, null, null) } catch (e: Exception) { }
                Vision(cle).regarder(sortie.toByteArray(), ame.humeur, corps.niveau(), souvenir) {
                    dire(it)
                    voix?.parler(it, ame.humeur)
                }
            } catch (e: Exception) {
                runOnUiThread { dire("Problème avec la photo : ${e.message}") }
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
                    dire("Ma clé est là. Parle-moi.")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun changerVoix() {
        val info = voix?.voixSuivante() ?: "Voix indisponible"
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
    }

    private fun parametres() {
        AlertDialog.Builder(this)
            .setTitle("Paramètres")
            .setItems(arrayOf("Changer de voix", "Saisir la clé API")) { _, quoi ->
                if (quoi == 0) changerVoix() else demanderCle()
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun infos() {
        AlertDialog.Builder(this)
            .setTitle("JOYEUX · Alayeman")
            .setMessage(
                "Visites : ${memoire.visites()}\n" +
                    "Ton prénom : ${memoire.prenom()}\n" +
                    "Batterie : ${corps.niveau()} %\n" +
                    "Humeur : ${ame.humeur.name}"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun lireFavoris(): MutableList<String> =
        (getSharedPreferences("alayeman", MODE_PRIVATE).getString("favoris", "") ?: "")
            .split("\u0001").filter { it.isNotBlank() }.toMutableList()

    private fun favoris() {
        val favs = lireFavoris()
        val texte = if (favs.isEmpty()) "Aucun favori pour l'instant." else favs.joinToString("\n\n") { "★ $it" }
        AlertDialog.Builder(this)
            .setTitle("Favoris")
            .setMessage(texte)
            .setPositiveButton("Ajouter la dernière réponse") { _, _ ->
                if (derniereReponse.isNotBlank()) {
                    favs.add(derniereReponse)
                    val garde = favs.takeLast(20)
                    getSharedPreferences("alayeman", MODE_PRIVATE).edit()
                        .putString("favoris", garde.joinToString("\u0001")).apply()
                    Toast.makeText(this, "Ajouté aux favoris ★", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun choisirEmoji() {
        val emojis = arrayOf("😄", "🖕", "😢", "😱", "👍", "❤️", "🔥", "👑")
        AlertDialog.Builder(this)
            .setItems(emojis) { _, quoi -> saisie.append(emojis[quoi]) }
            .show()
    }

    private fun icone(symbole: String, couleur: Int, action: () -> Unit): TextView = TextView(this).apply {
        text = symbole
        textSize = 20f
        setTextColor(couleur)
        gravity = Gravity.CENTER
        background = Style.boutonCommande(this@MainActivity)
        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(6) }
        Style.effetToucher(this)
        setOnClickListener { action() }
    }

    private fun commande(icone: String, titre: String, action: () -> Unit): Button = Button(this).apply {
        text = "$icone  $titre"
        textSize = 12f
        setTextColor(Style.BLANC)
        setTypeface(typeface, Typeface.BOLD)
        transformationMethod = null
        stateListAnimator = null
        background = Style.boutonCommande(this@MainActivity)
        layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f).apply {
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }
        Style.effetToucher(this)
        setOnClickListener {
            animate().alpha(0.5f).setDuration(70).withEndAction {
                animate().alpha(1f).setDuration(120).start()
            }.start()
            action()
        }
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

        val racine = FrameLayout(this).apply { setBackgroundColor(Style.NOIR) }

        // Fond : le personnage, qui flotte doucement
        val fond = ImageView(this).apply {
            setImageResource(R.drawable.fond_joyeux)
            alpha = 0.6f
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        racine.addView(
            fond,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
        )
        ObjectAnimator.ofFloat(fond, "translationY", -dp(10).toFloat(), dp(10).toFloat()).apply {
            duration = 6000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        // En-tête
        val avatarEntete = ImageView(this).apply {
            setImageResource(R.drawable.avatar_joyeux)
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
        }
        val nom = TextView(this).apply {
            text = "JOYEUX"
            textSize = 26f
            setTextColor(Style.VERT)
            setTypeface(typeface, Typeface.BOLD)
        }
        val voyant = View(this).apply {
            background = Style.rond(Style.VERT)
            layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { leftMargin = dp(8) }
        }
        Style.pulser(voyant)
        val ligneNom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(nom)
            addView(voyant)
        }
        humeur = TextView(this).apply {
            textSize = 13f
            setTextColor(Style.BLANC)
            setTypeface(typeface, Typeface.ITALIC)
        }
        majHumeur()
        val blocTitre = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(ligneNom)
            addView(humeur)
        }
        val entete = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Style.entete(this@MainActivity)
            setPadding(dp(12), dp(32), dp(12), dp(12))
            addView(avatarEntete)
            addView(blocTitre)
            addView(icone("⚙", Style.BLANC) { parametres() })
            addView(icone("ℹ", Style.BLANC) { infos() })
            addView(icone("★", Style.OR) { favoris() })
        }

        // Conversation
        fil = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        defilement = ScrollView(this).apply { addView(fil) }

        // Zone de saisie
        saisie = EditText(this).apply {
            hint = "Écris à Alayeman..."
            setTextColor(Style.BLANC)
            setHintTextColor(Style.GRIS)
            background = null
            maxLines = 4
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val boutonEmoji = TextView(this).apply {
            text = "😊"
            textSize = 22f
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener { choisirEmoji() }
        }
        val champ = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Style.champSaisie(this@MainActivity)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(boutonEmoji)
            addView(saisie)
        }
        val boutonEnvoi = TextView(this).apply {
            text = "➤"
            textSize = 22f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = Style.boutonEnvoi()
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply { leftMargin = dp(8) }
            Style.effetToucher(this)
            setOnClickListener { envoyer() }
        }
        val barreSaisie = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(4))
            addView(champ)
            addView(boutonEnvoi)
        }

        // Les six commandes
        val rangee1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(commande("✉", "ENVOYER") { envoyer() })
            addView(commande("🎤", "MICRO") {
                Toast.makeText(this@MainActivity, "Je t'écoute...", Toast.LENGTH_SHORT).show()
                ecoute?.ecouter(
                    { texte ->
                        saisie.setText(texte)
                        envoyer()
                    },
                    { erreur -> dire(erreur) }
                )
            })
            addView(commande("👁", "REGARDE") { regarder() })
        }
        val rangee2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(commande("🔎", "CHERCHE") {
                dire("Je cherche...")
                recherche.decouvrir { dire(it) }
            })
            addView(commande("🔊", "VOIX") { changerVoix() })
            addView(commande("🔑", "CLÉ") { demanderCle() })
        }
        val commandes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), dp(10))
            addView(rangee1)
            addView(rangee2)
        }

        val contenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(entete)
            addView(
                defilement,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            )
            addView(barreSaisie)
            addView(commandes)
        }
        racine.addView(
            contenu,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(racine)
        dire(ame.parler())
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
