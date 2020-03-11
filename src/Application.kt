package club.liefuck

import club.liefuck.data.Storage
import club.liefuck.vk.VKClient
import club.liefuck.vk.callback
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.error

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
fun Application.module() {
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
    install(StatusPages) {
        exception<Throwable> {
            call.respond(HttpStatusCode.InternalServerError, it.localizedMessage)
            log.error(it)
        }
    }
    install(Locations)

    val vkAccessToken = environment.config.property("vk.access_token").getString()
    val vkSecret = environment.config.property("vk.secret").getString()
    val communityId = environment.config.property("vk.community_id").getString()
    val vkClient = VKClient(vkAccessToken, communityId)

    val dbConfig = environment.config.config("db")
    val ds = HikariDataSource()
    ds.jdbcUrl = dbConfig.property("jdbcUrl").getString()
    ds.username = dbConfig.property("user").getString()
    ds.password = dbConfig.property("password").getString()
    val storage by lazy {
        Storage(ds)
    }

    routing {
        callback(vkSecret, vkClient, storage)
    }
}

@KtorExperimentalAPI
val Application.envKind
    get() = environment.config.property("ktor.environment").getString()
val Application.isDev get() = envKind == "dev"
val Application.isProd get() = envKind != "dev"
