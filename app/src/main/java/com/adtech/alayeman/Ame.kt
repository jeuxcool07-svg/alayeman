package com.adtech.alayeman

enum class Humeur(val emoji: String) {
    JOYEUX("😄"),
    BOUDEUR("😒"),
    ENERVE("😡"),
    STRESSE("😰"),
    PANIQUE("😱"),
    TRISTE("😢")
}

class Ame {

    var humeur: Humeur = Humeur.JOYEUX
        private set

    private val phrases: Map<Humeur, List<String>> = mapOf(
        Humeur.JOYEUX to listOf(
            "Yo Alaye ! Ça va être une bonne journée, je le sens.",
            "Je suis de bonne humeur, profites-en."
        ),
        Humeur.BOUDEUR to listOf(
            "Mouais. Je te parle pas trop là.",
            "Rien. Je boude, c'est tout."
        ),
        Humeur.ENERVE to listOf(
            "Non mais sérieux, t'as encore tout fermé ?",
            "Arrête de me tripoter, je suis pas d'humeur."
        ),
        Humeur.STRESSE to listOf(
            "Je me sens faible… branche-moi vite.",
            "Y'a un truc qui va pas, je le sens."
        ),
        Humeur.PANIQUE to listOf(
            "ALAYE ! Je vais m'éteindre, fais quelque chose !",
            "J'ai peur… ne me laisse pas mourir…"
        ),
        Humeur.TRISTE to listOf(
            "Tu m'as oublié aujourd'hui…",
            "Je me sens seul dans ce téléphone."
        )
    )

    fun changerHumeur(nouvelle: Humeur) {
        humeur = nouvelle
    }

    fun parler(): String {
        val choix = phrases[humeur] ?: return "..."
        return "${humeur.emoji} ${choix.random()}"
    }
}
