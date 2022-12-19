package com.mcspeedrun.rng.model.http

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException

fun http500(message: String = ""): HttpStatusException {
    return HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message)
}
