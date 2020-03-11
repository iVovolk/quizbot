package club.liefuck.vk

import club.liefuck.Callback
import club.liefuck.damerauLevenshteinDistance
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
            eventHandler(event, vkClient, storage)
        }
    }
}

lateinit var adminUsers: List<Long>

@KtorExperimentalAPI
private suspend fun eventHandler(event: VkChatEvent, vkClient: VKClient, storage: Storage) {
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

    when {
        command.startsWith(Commands.START.command) || command.startsWith(Commands.BEGIN.command) -> {
            val username = vkClient.firstNameByUserId(userId)
            val message = if (isAdmin) helloAdmin(username) else helloPlayer(username)
            vkClient.sendMessage(userId, message)
        }
        command.startsWith(Commands.PING.command) -> {
            val message = "Я тут. " + if (isAdmin) adminCommands else playerCommands
            vkClient.sendMessage(userId, message, keyboard)
        }
        command.startsWith(Commands.HELP.command) -> {
            val help = if (isAdmin) adminHelp else playerHelp
            vkClient.sendMessage(userId, help, keyboard)
        }
        command.startsWith(Commands.RULES.command) -> vkClient.sendMessage(userId, "Напоминаю правила.\n$rules")
        command.startsWith(Commands.QUESTION.command) -> {
            val message = if (isAdmin) {
                val q = storage.findActiveQuestionForAdmin()
                if (null == q) {
                    noActiveQuestionAdmin
                } else {
                    activeQuestionFormatted(q)
                }
            } else {
                if (!storage.playerIsAbleToGetQuestion(userId)) {
                    weeklyLimitExceed
                } else {
                    val q = storage.findActiveQuestionForPlayer()
                    if (null == q) {
                        noActiveQuestion
                    } else {
                        storage.startGame(userId, q.id)
                        "$questionPrefix\n${q.text}"
                    }
                }
            }
            vkClient.sendMessage(userId, message)
        }
        command.startsWith(Commands.ADD.command) -> {
            if (!isAdmin) return
            val parts = text
                .removePrefix(command)
                .split('%')
                .map { it.trim() }
                .filter { it != "" }
            if (parts.count() != 3) {
                vkClient.sendMessage(userId, errorWhileAddingQuestion)
                return
            }
            val (question, answer, promocode) = parts
            val message = if (storage.promocodeIsTaken(promocode)) {
                promocodeIsTaken
            } else {
                val qId = storage.addQuestion(question, answer, promocode)
                questionAddedMessage { qId.toString() }
            }
            vkClient.sendMessage(userId, message)
        }
        command.startsWith(Commands.DELETE.command) -> {
            if (!isAdmin) return
            val message = try {
                val qId = text.removePrefix(command).trim().toInt()
                if (!storage.questionExists(qId)) {
                    vkClient.sendMessage(userId, questionWasNotFound)
                    return
                }
                if (storage.questionHasAnswers(qId)) {
                    vkClient.sendMessage(userId, questionHasAnswers)
                    return

                }
                storage.deleteQuestion(qId)
                questionHasBeenDeleted
            } catch (e: NumberFormatException) {
                questionIdWasNotRecognized
            }
            vkClient.sendMessage(userId, message)
        }
        command.startsWith(Commands.ANSWER.command) -> {
            if (isAdmin) {
                vkClient.sendMessage(userId, adminAnswering)
                return
            }
            val answer = text.removePrefix(command).trim()
            if (answer.isBlank()) {
                vkClient.sendMessage(userId, emptyAnswer)
                return
            }
            val da = storage.findDraftAnswer(userId)
            if (null == da) {
                vkClient.sendMessage(userId, answerWithNoQuestion)
                return
            }
            if (da.askedAt + 30000 < System.currentTimeMillis()) {
                storage.stopGame(da.id, answer)
                vkClient.sendMessage(userId, timeIsUp)
                return
            }
            val q = storage.findQuestionById(da.questionId)
            if (null == q) {
                storage.stopGame(da.id, answer)
                vkClient.sendMessage(userId, unusualShit)
                return
            }
            val message = when {
                answer == q.answer -> {
                    storage.stopGame(da.id, answer, true)
                    correctAnswerMessage { q.promocode }
                }
                damerauLevenshteinDistance(answer, q.answer) <= 2 -> inaccurateAnswer
                else -> {
                    storage.stopGame(da.id, answer)
                    incorrectAnswer
                }
            }
            vkClient.sendMessage(userId, message)
        }
    }
}

private fun extractCommand(message: Message, keyboardAvailable: Boolean): String {
    val command = message.parsedPayload?.command
    if (keyboardAvailable && null != command) {
        return command
    }
    return message.text.trim().substringBefore(" ").toLowerCase()
}

enum class Commands(val command: String) {
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
    ADD("/добавить")
}
