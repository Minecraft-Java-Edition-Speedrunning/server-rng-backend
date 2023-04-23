package com.mcspeedrun.rng.model

enum class Access (
    val id: Int,
) {
    RUN(0);

    companion object {
        // turn roles into a string
        fun serialize(roles: List<Access>): String {
            return roles.joinToString(separator = " ") { it.name }
        }
    }
}
