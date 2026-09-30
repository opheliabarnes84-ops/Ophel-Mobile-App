package com.example.data.model

enum class ProcessCategory(val displayName: String) {
    SOCIAL("Social & Network"),
    MEDIA("Streaming & Media"),
    UTILITY("Tool & Utility"),
    BACKGROUND_SERVICE("Background Daemon"),
    SYSTEM("System Essential")
}

data class ProcessInfo(
    val packageName: String,
    val appName: String,
    val pid: Int,
    val memoryUsageMb: Long,
    val importance: String,
    val category: ProcessCategory,
    val isProtected: Boolean = false,
    val isSelected: Boolean = true,
    val isKilled: Boolean = false
)
