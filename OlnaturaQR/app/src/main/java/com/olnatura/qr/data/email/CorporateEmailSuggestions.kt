package com.olnatura.qr.data.email


object CorporateEmailSuggestions {
    val ALL: List<String> = listOf(
        "Virginia.Amaro@olnatura.com",
        "ac.supervision@olnatura.com",
        "supervisor.inspeccion@olnatura.com",
        "inspeccion.materiales@olnatura.com",
    )

    fun available(usedLowercase: Set<String>, query: String): List<String> {
        val q = query.trim().lowercase()
        return ALL.filter { email ->
            val lower = email.lowercase()
            lower !in usedLowercase && (q.isEmpty() || lower.contains(q))
        }
    }
}
