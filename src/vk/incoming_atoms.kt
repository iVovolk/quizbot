package club.liefuck.vk

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class VkChatEvent(
    val type: String,
    @SerializedName("group_id") val groupId: Long,
    @SerializedName("event_id") val eventId: String,
    @SerializedName("object") val body: Body,
    val secret: String
)

data class Body(val message: Message, @SerializedName("client_info") val clientInfo: ClientInfo)

data class Message(
    val id: Long,
    val date: Long,
    @SerializedName("from_id") val fromIid: Long,
    val out: Int,
    @SerializedName("peer_id") val peerId: Long,
    val text: String,
    @SerializedName("conversation_message_id") val conversationMessageId: Long,
    val important: Boolean,
    @SerializedName("random_id") val randomId: Long,
    val payload: String?,
    @SerializedName("is_hidden") val isHidden: Boolean
) {
    //this is needed because VK sends payload as a string not as an object thus it could't be deserialized automatically
    val parsedPayload: Payload?
        get() {
            if (payload == null) {
                return null
            }
            return Gson().fromJson(payload, Payload::class.java)
        }
}

data class ClientInfo(val keyboard: Boolean)
