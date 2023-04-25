package com.mcspeedrun.rng.controller

import com.mcspeedrun.rng.model.AuthenticationMethod
import com.mcspeedrun.rng.model.auth.YggdrasilRegistration
import com.mcspeedrun.rng.model.auth.AccessRefreshToken
import com.mcspeedrun.rng.model.http.http401
import com.mcspeedrun.rng.service.AuthenticationService
import com.mcspeedrun.rng.service.YggdrasilService
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import javax.annotation.security.PermitAll


@PermitAll
@Controller("/authentication")
@Suppress("Unused")
class AuthenticationController (
    private val authenticationService: AuthenticationService,
    private val yggdrasilService: YggdrasilService,
) {
    @Post("yggdrasil")
    fun registerYggdrasil(
        @Body yggdrasilPayload: YggdrasilRegistration,
    ): AccessRefreshToken {
        if (yggdrasilService.validate(yggdrasilPayload)) {
            return authenticationService.registerInstance(AuthenticationMethod.YGGDRASIL, yggdrasilPayload.uuid)
        }
        throw http401("unable to authenticate with server")
    }

    @Post("refresh")
    fun refreshToken(
        @Body refreshToken: String
    ): AccessRefreshToken {
        println(refreshToken)
        return authenticationService.refreshInstance(refreshToken)
    }
}
