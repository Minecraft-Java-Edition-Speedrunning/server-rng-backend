package com.mcspeedrun.rng.model.http

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException

fun http425(message: String = ""): HttpStatusException {
    return HttpStatusException(HttpStatus.TOO_EARLY, message)
}
