package club.liefuck.vk

import club.liefuck.Callback
import club.liefuck.data.Storage
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Route.callback(secret: String, vkClient: VKClient, storage: Storage) {
    post<Callback> {
        val event = call.receive<VkChatEvent>()
        if (event.secret != secret) {
            throw BadRequestException("")
        }
        call.respondText("ok")
        if (event.type == "message_new") {
            proxyHandler(event, vkClient, storage)
        }
    }
}

lateinit var adminUsers: List<Long>

@KtorExperimentalAPI
private suspend fun proxyHandler(event: VkChatEvent, vkClient: VKClient, storage: Storage) {
    if (!::adminUsers.isInitialized) {
        adminUsers = vkClient.getCommunityAdmins()
    }

    val isAdmin = adminUsers.contains(event.body.message.fromIid)

    val userId = event.body.message.fromIid
    val text = event.body.message.text

    val keyboardAvailable = event.body.clientInfo.keyboard
    val keyboard: Keyboard? by lazy {
        if (keyboardAvailable) {
            if (isAdmin) Keyboard.admin() else Keyboard.player()
        } else {
            null
        }
    }

    val command = extractCommand(event.body.message, keyboardAvailable)
    val arguments = CommandHandlerArguments(
        storage,
        vkClient,
        text,
        command,
        userId,
        isAdmin
    )

    when {
        command.startsWith(Commands.START.command) || command.startsWith(Commands.BEGIN.command) -> {
            val username = vkClient.getFirstNameByUserId(userId)
            val message = if (isAdmin) helloAdmin(username) else helloPlayer(username)
            vkClient.sendMessage(userId, message)
        }
        command.startsWith(Commands.PING.command) -> {
            val message = pingPrefix + if (isAdmin) adminCommands else playerCommands
            vkClient.sendMessage(userId, message, keyboard)
        }
        command.startsWith(Commands.HELP.command) -> {
            val help = if (isAdmin) adminHelp else playerHelp
            vkClient.sendMessage(userId, help, keyboard)
        }
        command.startsWith(Commands.RULES.command) -> vkClient.sendMessage(userId, "$rulesPrefix$rules")
        command.startsWith(Commands.QUESTION.command) -> handleQuestion(arguments)
        command.startsWith(Commands.ADD.command) -> handleQuestionAddition(arguments)
        command.startsWith(Commands.DELETE.command) -> handleQuestionDeletion(arguments)
        command.startsWith(Commands.ANSWER.command) -> handleAnswer(arguments)
        command.startsWith(Commands.EDIT.command) -> handleEditing(arguments)
        command.startsWith(Commands.WINNERS.command) -> handleWinners(arguments)
        command.startsWith(Commands.LIST.command) -> handleList(arguments)
    }
}

internal data class CommandHandlerArguments(
    val storage: Storage,
    val vkClient: VKClient,
    val text: String,
    val command: String,
    val userId: Long,
    val isAdmin: Boolean
)

private fun extractCommand(message: Message, keyboardAvailable: Boolean): String {
    val command = message.parsedPayload?.command
    if (keyboardAvailable && null != command) {
        return command
    }
    return message.text.trim().substringBefore(" ").toLowerCase()
}

private enum class Commands(val command: String) {
    START("start"),
    BEGIN("/начать"),
    PING("/бот"),
    QUESTION("/вопрос"),
    RULES("/правила"),
    HELP("/помощь"),
    ANSWER("/ответ"),
    WINNERS("/угадали"),
    LIST("/список"),
    DELETE("/удалить"),
    ADD("/добавить"),
    EDIT("/править")
}
