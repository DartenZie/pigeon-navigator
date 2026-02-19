package cz.miroslavpasek.pigeonnavigator

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform