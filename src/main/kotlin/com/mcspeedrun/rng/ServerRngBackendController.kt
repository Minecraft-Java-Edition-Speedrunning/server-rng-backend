package com.mcspeedrun.rng

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.security.Principal

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/api/v2")
@Suppress("Unused")
class ServerRngBackendController {
    @Post
    fun test(principal: Principal) {
        println(principal.name)
    }
}
