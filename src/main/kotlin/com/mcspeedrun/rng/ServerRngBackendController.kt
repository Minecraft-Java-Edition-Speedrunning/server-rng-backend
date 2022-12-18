package com.mcspeedrun.rng

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Produces
import io.micronaut.http.MediaType
import io.micronaut.core.annotation.Introspected

@Controller("/serverRngBackend")
class ServerRngBackendController {

    @Produces(MediaType.TEXT_PLAIN)
    @Get
    fun index(): String {
        return "Example Response"
    }

    @Post
    fun post(@Body inputMessage: SampleInputMessage): SampleReturnMessage {
        return SampleReturnMessage("Hello ${inputMessage.name}, thank you for sending the message")
    }
}

@Introspected
data class SampleInputMessage(val name: String)

@Introspected
data class SampleReturnMessage(val returnMessage: String)