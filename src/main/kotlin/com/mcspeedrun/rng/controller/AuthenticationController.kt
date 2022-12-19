package com.mcspeedrun.rng.controller

import com.mcspeedrun.rng.model.AuthenticationMethod
import com.mcspeedrun.rng.model.YggdrasilRegistration
import com.mcspeedrun.rng.model.http.http401
import com.mcspeedrun.rng.service.AuthenticationService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.token.jwt.render.AccessRefreshToken
import javax.annotation.security.PermitAll


@PermitAll
@Controller("/authentication")
@Suppress("Unused")
class AuthenticationController (
    private val authenticationService: AuthenticationService,
) {
    @Post("yggdrasil")
    fun registerYggdrasil(
        @Body yggdrasilPayload: YggdrasilRegistration,
    ): AccessRefreshToken {
        if (authenticationService.validateYggdrasil(yggdrasilPayload)) {
            return authenticationService.registerInstance(AuthenticationMethod.YGGDRASIL, yggdrasilPayload.uuid)
        }
        throw http401("unable to authenticate with server")
    }

    @Post("refresh")
    fun name(
        @Body refreshToken: String
    ): AccessRefreshToken {
        return authenticationService.refreshInstance(refreshToken)
    }
}
