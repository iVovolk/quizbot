package club.liefuck.vk

import com.google.gson.annotations.SerializedName

data class Keyboard(
    val buttons: List<List<Button>>,
    @SerializedName("one_time") val oneTime: Boolean = true,
    val inline: Boolean = false
) {
    companion object {
        private val commonList by lazy {
            listOf(
                Button(Action(ActionType.TEXT.type, "Вопрос", Payload("/вопрос")), Color.POSITIVE.type),
                Button(Action(ActionType.TEXT.type, "Правила", Payload("/правила")), Color.PRIMARY.type),
                Button(Action(ActionType.TEXT.type, "Помощь", Payload("/помощь")), Color.PRIMARY.type)
            )
        }

        fun player(): Keyboard {
            val buttons = listOf(commonList)
            return Keyboard(buttons)
        }

        fun admin(): Keyboard {
            val buttons = listOf(
                commonList + listOf(
                    Button(
                        Action(ActionType.TEXT.type, "Угадали", Payload("/угадали")),
                        Color.POSITIVE.type
                    )
                )
            )
            return Keyboard(buttons)
        }
    }
}

data class Button(val action: Action, val color: String)
data class Action(val type: String, val label: String, val payload: Payload)
data class Payload(val command: String)

enum class Color(val type: String) {
    PRIMARY("primary"),
    SECONDARY("secondary"),
    NEGATIVE("negative"),
    POSITIVE("positive")
}

enum class ActionType(val type: String) {
    TEXT("text"),
    OPEN_LINK("open_link"),
    LOCATION("location"),
    VK_PAY("vkpay"),
    VK_APPS("open_app"),
}