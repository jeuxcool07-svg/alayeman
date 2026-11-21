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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
    private var voixIA: VoixIA? = null
    private var ecoute: Ecoute? = null
    private var ussd: Ussd? = null
    private var presse: Presse? = null
    private var uriPhoto: Uri? = null
    private lateinit var memoire: Memoire
    private lateinit var journal: Journal
    private var souvenir = ""
    private lateinit var corps: BatterieCorps
    private lateinit var fil: LinearLayout
    private lateinit var defilement: ScrollView
    private lateinit var humeur: TextView
    private lateinit var saisie: EditText
    private var derniereReponse = ""

    // Mots qui déclenchent les outils
    private val motsCredit = Regex("(?i)(cr[ée]dit|forfait|solde|\\bdata\\b|mo restant|go restant)")
    private val motsPresse = Regex("(?i)(presse[- ]?papiers?|copi[ée]|clipboard)")
    private val motsGarder = Regex("(?i)(garde|stocke|note)")
    private val motsAchatData = Regex(
        "(?i)((transform|convert|ach[eè]t|prend|\\bmets?\\b|mettre|passe).*(data|internet|connexion)" +
            "|(data|internet|connexion).*(transform|convert|ach[eè]t|prend|\\bmets?\\b|mettre|passe))"
    )
    private val montantData = Regex("\\b(100|200|250|500|1000)\\b")

    private fun dp(valeur: Int): Int = Style.dp(this, valeur)

    private fun heure(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    // Largeur disponible pour les bulles : presque tout l'écran
    private fun largeurMax(): Int = resources.displayMetrics.widthPixels - dp(110)

    private fun largeurTexte(vararg vues: TextView): Int {
        var plus = 0f
        for (v in vues) {
            plus = maxOf(plus, v.paint.measureText(v.text.toString()))
        }
        return minOf(plus.toInt() + dp(6), largeurMax())
    }

    private fun faireDefiler() {
        defilement.post { defilement.fullScroll(View.FOCUS_DOWN) }
    }

    private fun majHumeur() {
        humeur.text = "${ame.humeur.emoji} ${ame.humeur.name.lowercase()} · toujours là pour toi 👑"
    }

    private fun nouveauCerveau() {
        cerveau = Cerveau().also {
            it.souvenir = souvenir
            it.journal = journal.derniers()
        }
    }

    // Parle avec la voix du téléphone uniquement
    private fun parler(texte: String) {
        voix?.parler(texte, ame.humeur)
    }

    // Message d'Alayeman (bulle violette)
    private fun dire(texte: String) {
        derniereReponse = texte
        val avatar = ImageView(this).apply {
            setImageResource(R.drawable.avatar_joyeux)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(8) }
        }
        val nom = TextView(this).apply {
            text = "Joyeux"
            setTextColor(Style.OR)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
        }
        val contenu = TextView(this).apply {
            text = texte
            setTextColor(Style.BLANC)
            textSize = 15f
        }
        val horaire = TextView(this).apply {
            text = heure()
            setTextColor(Style.GRIS)
            textSize = 11f
        }
        contenu.layoutParams = LinearLayout.LayoutParams(
            largeurTexte(nom, contenu, horaire), ViewGroup.LayoutParams.WRAP_CONTENT
        )
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

    // Message de l'utilisateur (bulle dorée)
    private fun moi(texte: String) {
        val contenu = TextView(this).apply {
            text = texte
            setTextColor(Style.BLANC)
            textSize = 15f
        }
        val horaire = TextView(this).apply {
            text = heure()
            setTextColor(Style.GRIS)
            textSize = 11f
        }
        contenu.layoutParams = LinearLayout.LayoutParams(
            largeurTexte(contenu, horaire), ViewGroup.LayoutParams.WRAP_CONTENT
        )
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

    // Envoie une consigne au cerveau avec le journal à jour
    private fun demanderAuCerveau(consigne: String) {
        val c = cerveau ?: return
        c.souvenir = souvenir
        c.journal = journal.derniers()
        corps.ressentir(ame)
        majHumeur()
        c.repondre(consigne, ame.humeur, corps.niveau()) {
            dire(it)
            parler(it)
        }
    }

    // OUTIL 1 : crédit et forfait par USSD (*101#)
    private fun verifierCredit() {
        val u = ussd ?: return
        journal.noter("CREDIT", "code lancé")
        dire("Je lance ton code de crédit, une seconde...")
        u.verifierCredit(
            { reponse ->
                val brut = reponse.trim()
                cerveau?.credit = brut.take(600)
                journal.noter("CREDIT", "réponse reçue")
                // La réponse exacte de Moov, pour que tu puisses la vérifier toi-même
                dire("📩 Réponse de Moov :\n$brut")
                demanderAuCerveau(
                    "Voici la réponse de mon code crédit. Dis-moi combien j'ai de crédit et de data, " +
                        "et ce qui expire bientôt."
                )
            },
            { erreur ->
                journal.noter("CREDIT_ECHEC", erreur)
                dire(erreur)
            }
        )
    }

    // OUTIL 2 : transformer du crédit en data (confirmation obligatoire dans Ussd)
    private fun convertirEnData(montant: Int) {
        val u = ussd ?: return
        journal.noter("DATA", "achat de $montant F demandé")
        u.convertirEnData(
            montant,
            { reponse ->
                val brut = reponse.trim()
                journal.noter("DATA", "réponse reçue pour $montant F")
                dire("📩 Réponse de Moov :\n$brut")
                demanderAuCerveau(
                    "Voici la réponse de Moov à mon achat de data de $montant F de crédit. " +
                        "Dis en une phrase si ça a marché."
                )
            },
            { erreur ->
                journal.noter("DATA_ECHEC", erreur)
                dire(erreur)
            }
        )
    }

    // Liste des montants quand aucun n'est donné
    private fun choisirMontantData() {
        val u = ussd ?: return
        val montants = u.montantsDisponibles()
        AlertDialog.Builder(this)
            .setTitle("Combien de crédit transformer en data ?")
            .setItems(montants.map { "$it F" }.toTypedArray()) { _, quoi ->
                val m = montants[quoi]
                moi("Transforme $m F de crédit en data.")
                convertirEnData(m)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // OUTIL 3 : presse-papiers (uniquement sur demande)
    private fun analyserPresse(demande: String) {
        val p = presse ?: return
        val texte = p.lire()
        if (texte == null) {
            journal.noter("PRESSE", "vide ou illisible")
            dire("Ton presse-papiers est vide, ou Android ne me laisse pas le lire. Copie un texte, reviens ici, puis réessaie.")
            return
        }
        if (p.sensible(texte)) {
            journal.noter("PRESSE", "contenu sensible ignoré")
            dire("Ce que tu as copié ressemble à un code, un mot de passe ou un numéro de carte. Je ne le lis pas et je ne l'envoie nulle part.")
            return
        }
        journal.noter("PRESSE", "${texte.length} caractères lus")
        cerveau?.presse = p.apercu(texte)
        if (motsGarder.containsMatchIn(demande)) {
            p.garder(texte)
            journal.noter("PRESSE", "texte gardé en note")
        }
        demanderAuCerveau("Voici ce que je viens de copier. Résume-le et commente-le.")
    }

    private fun envoyer() {
        val message = saisie.text.toString().trim()
        if (message.isEmpty()) return
        val c = cerveau ?: return

        val nom = Regex("(?i)^je m'appelle\\s+(\\p{L}+)").find(message)
        if (nom != null) {
            memoire.definirPrenom(nom.groupValues[1])
            souvenir = memoire.souvenir()
            c.souvenir = souvenir
        }

        moi(message)
        saisie.setText("")

        // Le routage est local et déterministe : l'IA ne déclenche aucune action seule
        if (motsPresse.containsMatchIn(message)) {
            analyserPresse(message)
            return
        }
        if (motsAchatData.containsMatchIn(message)) {
            val montant = montantData.find(message)?.value?.toIntOrNull()
            if (montant != null) convertirEnData(montant) else choisirMontantData()
            return
        }
        if (motsCredit.containsMatchIn(message)) {
            verifierCredit()
            return
        }

        journal.noter("MESSAGE", message)
        demanderAuCerveau(message)
    }

    // Micro : on appuie, on parle, le message part tout seul à la fin
    private fun ecouterVoix() {
        Toast.makeText(this, "Je t'écoute...", Toast.LENGTH_SHORT).show()
        ecoute?.ecouter(
            { texte ->
                saisie.setText(texte)
                envoyer()
            },
            { erreur -> dire(erreur) }
        )
    }

    // Prendre une photo avec l'appareil photo
    private fun regarder() {
        if (BuildConfig.GEMINI_KEY.isBlank()) {
            dire("Je n'ai pas encore d'yeux... demande à Alay de me les activer.")
            return
        }
        journal.noter("REGARDE", "photo demandée")
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

    // Choisir une photo déjà dans la galerie
    private fun choisirPhoto() {
        if (BuildConfig.GEMINI_KEY.isBlank()) {
            dire("Je n'ai pas encore d'yeux... demande à Alay de me les activer.")
            return
        }
        journal.noter("REGARDE", "photo choisie dans la galerie")
        try {
            val intention = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intention, "Choisir une photo"), 4)
        } catch (e: Exception) {
            dire("Je n'arrive pas à ouvrir la galerie.")
        }
    }

    // Menu du bouton plus : photos, crédit, data, presse-papiers
    private fun menuPlus() {
        AlertDialog.Builder(this)
            .setItems(
                arrayOf(
                    "📷 Prendre une photo",
                    "🖼 Choisir une photo",
                    "💳 Voir mon crédit",
                    "💾 Convertir du crédit en data",
                    "📋 Analyser le presse-papiers"
                )
            ) { _, quoi ->
                when (quoi) {
                    0 -> regarder()
                    1 -> choisirPhoto()
                    2 -> {
                        moi("Mon crédit et mes forfaits ?")
                        verifierCredit()
                    }
                    3 -> choisirMontantData()
                    else -> {
                        moi("Analyse ce que j'ai copié.")
                        analyserPresse("")
                    }
                }
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3) {
            val uri = uriPhoto ?: return
            if (resultCode != RESULT_OK) {
                try { contentResolver.delete(uri, null, null) } catch (e: Exception) { }
                return
            }
            analyserImage(uri, true)
        } else if (requestCode == 4) {
            if (resultCode != RESULT_OK) return
            val uri = data?.data ?: return
            analyserImage(uri, false)
        }
    }

    // Envoie une photo à Gemini (via Vision) et fait réagir Joyeux
    private fun analyserImage(uri: Uri, supprimer: Boolean) {
        moi("📷 Photo envoyée")
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
                if (supprimer) {
                    try { contentResolver.delete(uri, null, null) } catch (e: Exception) { }
                }
                Vision(BuildConfig.GEMINI_KEY)
                    .regarder(sortie.toByteArray(), ame.humeur, corps.niveau(), souvenir) {
                        dire(it)
                        parler(it)
                    }
            } catch (e: Exception) {
                runOnUiThread { dire("Problème avec la photo : ${e.message}") }
            }
        }
    }

    private fun changerVoix() {
        val info = voix?.voixSuivante() ?: "Voix indisponible"
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
    }

    private fun effacer() {
        fil.removeAllViews()
        nouveauCerveau()
        dire(ame.parler())
    }

    private fun decouvrir() {
        journal.noter("CHERCHE", "recherche lancée")
        dire("Je cherche...")
        recherche.decouvrir { dire(it) }
    }

    private fun afficherJournal() {
        AlertDialog.Builder(this)
            .setTitle("Journal de bord")
            .setMessage(journal.derniers(30).replace(" | ", "\n"))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun afficherNotes() {
        val notes = presse?.notes() ?: emptyList()
        AlertDialog.Builder(this)
            .setTitle("Notes du presse-papiers")
            .setMessage(if (notes.isEmpty()) "Aucune note pour l'instant." else notes.joinToString("\n\n"))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun parametres() {
        AlertDialog.Builder(this)
            .setTitle("Paramètres")
            .setItems(
                arrayOf(
                    "Changer de voix",
                    "Journal de bord",
                    "Notes du presse-papiers",
                    "Regarder (appareil photo)",
                    "Chercher (découverte)",
                    "Effacer la conversation"
                )
            ) { _, quoi ->
                when (quoi) {
                    0 -> changerVoix()
                    1 -> afficherJournal()
                    2 -> afficherNotes()
                    3 -> regarder()
                    4 -> decouvrir()
                    else -> effacer()
                }
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
        val emojis = arrayOf("😄", "😡", "😢", "😱", "👍", "❤️", "🔥", "👑")
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

    private fun construire() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        corps = BatterieCorps(this)
        // La voix Gemini est désactivée : seule la voix du téléphone est utilisée
        voix = Voix(this)
        ecoute = Ecoute(this)
        ussd = Ussd(this)
        presse = Presse(this)
        memoire = Memoire(this)
        journal = Journal(this)

        souvenir = memoire.souvenir()
        val humeurAbsence = memoire.humeurAbsence()
        memoire.marquerVisite()
        journal.noter("OUVERTURE", "appli ouverte")
        nouveauCerveau()

        corps.ressentir(ame)
        if (humeurAbsence != null) ame.changerHumeur(humeurAbsence)

        val racine = FrameLayout(this).apply { setBackgroundColor(Style.NOIR) }

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

        val avatarEntete = ImageView(this).apply {
            setImageResource(R.drawable.avatar_joyeux)
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
        }
        val nom = TextView(this).apply {
            text = "JOYEUX"
            textSize = 26f
            setTextColor(Style.OR)
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

        fil = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        defilement = ScrollView(this).apply { addView(fil) }

        saisie = EditText(this).apply {
            hint = "Écris à Alayeman..."
            setTextColor(Style.BLANC)
            setHintTextColor(Style.GRIS)
            textSize = 16f
            background = null
            minLines = 1
            maxLines = 6
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
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
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(boutonEmoji)
            addView(saisie)
        }

        // Bouton plus (à gauche) : photos, crédit, data, presse-papiers
        val boutonPlus = TextView(this).apply {
            text = "＋"
            textSize = 26f
            setTextColor(Style.OR)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = Style.boutonCommande(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).apply { rightMargin = dp(8) }
            Style.effetToucher(this)
            setOnClickListener { menuPlus() }
        }

        // Bouton de droite, comme WhatsApp : micro quand le champ est vide, flèche quand il y a du texte
        val boutonAction = TextView(this).apply {
            text = "🎤"
            textSize = 22f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = Style.boutonEnvoi()
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply { leftMargin = dp(8) }
            setOnTouchListener { vue, evenement ->
                when (evenement.action) {
                    MotionEvent.ACTION_DOWN -> {
                        vue.alpha = 0.6f
                        if (saisie.text.isBlank()) ecouterVoix()
                    }
                    MotionEvent.ACTION_UP -> {
                        vue.alpha = 1f
                        if (saisie.text.isNotBlank()) envoyer()
                    }
                    MotionEvent.ACTION_CANCEL -> vue.alpha = 1f
                }
                true
            }
        }
        saisie.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                boutonAction.text = if (s.isNullOrBlank()) "🎤" else "➤"
            }
        })

        val barreSaisie = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(8))
            addView(boutonPlus)
            addView(champ)
            addView(boutonAction)
        }

        // Toute la largeur pour la discussion, puis la barre de saisie en bas
        val colonneChat = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(defilement, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(barreSaisie)
        }

        val contenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(entete)
            addView(
                colonneChat,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            )
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
