package org.saffron

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform