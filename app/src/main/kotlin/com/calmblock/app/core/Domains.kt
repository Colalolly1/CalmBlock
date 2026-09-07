package com.calmblock.app.core

object Domains {

    /** "https://www.Example.com/path" becomes "example.com". Null when there is no host in it. */
    fun normalise(input: String): String? {
        var s = input.trim().lowercase()
        if (s.isEmpty() || s.contains(' ')) return null
        s = s.substringAfter("://")
        s = s.substringBefore('/').substringBefore('?').substringBefore('#')
        s = s.substringAfter('@').substringBefore(':')
        s = s.removePrefix("www.")
        if (!s.contains('.') || s.startsWith(".") || s.endsWith(".")) return null
        return s
    }

    fun isBlocked(host: String, blocked: Set<String>): Boolean {
        val h = host.removePrefix("www.")
        return blocked.any { h == it || h.endsWith(".$it") }
    }
}
