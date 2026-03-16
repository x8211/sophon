package sophon.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform