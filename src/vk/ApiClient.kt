package club.liefuck.vk

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.formUrlEncode
import io.ktor.util.KtorExperimentalAPI
import kotlin.random.Random

class VKClient(private val token: String, private val communityId: String, private val version: String = "5.103") {
    private val baseEndpoint = "https://api.vk.com/method/"

    @KtorExperimentalAPI
    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
    }


    @KtorExperimentalAPI
    suspend fun firstNameByUserId(userId: Long): String? {
        val params = listOf("user_id" to userId.toString())
        val info = makeGetRequest<Response<List<UserInfo>>>(params, "users.get")
        return info.response.firstOrNull()?.firstName
    }

    @KtorExperimentalAPI
    suspend fun getCommunityAdmins(): List<Long> {
        val params = listOf("group_id" to communityId, "filter" to "managers")
        val m = makeGetRequest<Response<PaginatedResponse<CommunityManager>>>(
            params,
            "groups.getMembers"
        ).response.items
        val adminRoles = listOf("administrator", "creator")
        return m.filter { adminRoles.contains(it.role) }.map { it.id }
    }

    @KtorExperimentalAPI
    suspend fun sendMessage(userId: Long, message: String, keyboard: Keyboard? = null) {
        val params = listOf(
            "random_id" to Random.nextLong().toString(),
            "peer_id" to communityId,
            "user_id" to userId.toString(),
            "message" to message
        )
        val paramsWithKeyboard = if (null != keyboard) {
            params + listOf("keyboard" to Gson().toJson(keyboard).toString())
        } else {
            params
        }
        makePostRequest(paramsWithKeyboard, "messages.send")
    }

    @KtorExperimentalAPI
    private suspend inline fun <reified T> makeGetRequest(params: List<Pair<String, String>>, method: String): T =
        client.get<T>(
            "$baseEndpoint$method?" + (listOf(
                "v" to version,
                "access_token" to token
            ) + params).formUrlEncode()
        )

    @KtorExperimentalAPI
    private suspend fun makePostRequest(params: List<Pair<String, String>>, method: String) {
        val urlEncodedParams = (listOf("v" to version, "access_token" to token) + params).formUrlEncode()

        client.post<Unit>(baseEndpoint + method) {
            header("Content-Type", "application/x-www-form-urlencoded")
            body = urlEncodedParams
        }
    }
}

data class Response<Any>(val response: Any)
data class UserInfo(
    val id: Long,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
)

data class PaginatedResponse<T>(val count: Int, val items: List<T>)
data class CommunityManager(val id: Long, val role: String)
