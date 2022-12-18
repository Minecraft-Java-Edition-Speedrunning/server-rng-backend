package com.mcspeedrun.rng
import io.micronaut.http.*
import org.junit.jupiter.api.Test
import io.micronaut.gcp.function.http.*
import org.junit.jupiter.api.Assertions

class ServerRngBackendFunctionTest {

    @Test
    fun testFunction() {
        HttpFunction().use { function ->
           val response = function.invoke(HttpMethod.GET, "/serverRngBackend")
            Assertions.assertEquals(HttpStatus.OK, response.status)
        }
    }


    @Test
    fun testPost(){
        HttpFunction().use { function ->
            val input = SampleInputMessage("Test Name")
            val request = HttpRequest.POST("/serverRngBackend", input).contentType(MediaType.APPLICATION_JSON_TYPE)
            val response = function.invoke(request)
            Assertions.assertEquals(HttpStatus.OK, response.status)
        }
    }
}
