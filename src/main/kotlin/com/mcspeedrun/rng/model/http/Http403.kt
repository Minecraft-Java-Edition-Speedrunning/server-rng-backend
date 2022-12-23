package com.mcspeedrun.rng.model.http

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException

fun http403(message: String = ""): HttpStatusException {
    return HttpStatusException(HttpStatus.FORBIDDEN, message)
}
