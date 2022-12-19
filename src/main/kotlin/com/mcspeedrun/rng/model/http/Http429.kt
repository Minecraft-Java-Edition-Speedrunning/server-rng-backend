package com.mcspeedrun.rng.model.http

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException

fun http429(message: String = ""): HttpStatusException {
    return HttpStatusException(HttpStatus.TOO_MANY_REQUESTS, message)
}
