package club.liefuck.vk

import club.liefuck.damerauLevenshteinDistance
import club.liefuck.thirtySecInMillis
import club.liefuck.tsAsDateString
import io.ktor.util.KtorExperimentalAPI
import kotlin.math.absoluteValue

@KtorExperimentalAPI
internal suspend fun handleQuestion(arguments: CommandHandlerArguments) {
    val message = if (arguments.isAdmin) {
        val q = arguments.storage.findActiveQuestionForAdmin()
        if (null == q) {
            noActiveQuestionAdmin
        } else {
            activeQuestionFormatted(q)
        }
    } else {
        if (!arguments.storage.playerIsAbleToGetQuestion(arguments.userId)) {
            weeklyLimitExceed
        } else {
            val q = arguments.storage.findActiveQuestionForPlayer()
            if (null == q) {
                noActiveQuestion
            } else {
                arguments.storage.startGame(arguments.userId, q.id)
                "$questionPrefix\n${q.text}"
            }
        }
    }
    arguments.vkClient.sendMessage(arguments.userId, message)
}

@KtorExperimentalAPI
internal suspend fun handleQuestionAddition(arguments: CommandHandlerArguments) {
    if (!arguments.isAdmin) return
    val parts = arguments.text
        .removePrefix(arguments.command)
        .split('%')
        .map { it.trim() }
        .filter { it != "" }
    if (parts.count() != 3) {
        arguments.vkClient.sendMessage(arguments.userId, errorWhileAddingQuestion)
        return
    }
    val (question, answer, promocode) = parts
    val message = if (arguments.storage.promocodeIsTaken(promocode)) {
        promocodeIsTaken
    } else {
        val qId = arguments.storage.addQuestion(question, answer, promocode)
        questionAddedMessage { qId.toString() }
    }
    arguments.vkClient.sendMessage(arguments.userId, message)
}

@KtorExperimentalAPI
internal suspend fun handleQuestionDeletion(arguments: CommandHandlerArguments) {
    if (!arguments.isAdmin) return
    val message = try {
        val qId = arguments.text.removePrefix(arguments.command).trim().toInt()
        if (!arguments.storage.questionExists(qId)) {
            arguments.vkClient.sendMessage(arguments.userId, questionWasNotFound)
            return
        }
        if (arguments.storage.questionHasAnswers(qId)) {
            arguments.vkClient.sendMessage(arguments.userId, questionHasAnswers)
            return

        }
        arguments.storage.deleteQuestion(qId)
        questionHasBeenDeleted
    } catch (e: NumberFormatException) {
        errorWhileQuestionDeletion
    }
    arguments.vkClient.sendMessage(arguments.userId, message)
}

@KtorExperimentalAPI
internal suspend fun handleAnswer(arguments: CommandHandlerArguments) {
    if (arguments.isAdmin) {
        arguments.vkClient.sendMessage(arguments.userId, adminAnswering)
        return
    }
    val answer = arguments.text.removePrefix(arguments.command).trim()
    if (answer.isBlank()) {
        arguments.vkClient.sendMessage(arguments.userId, emptyAnswer)
        return
    }
    val da = arguments.storage.findDraftAnswer(arguments.userId)
    if (null == da) {
        arguments.vkClient.sendMessage(arguments.userId, answerWithNoQuestion)
        return
    }
    if (da.askedAt + thirtySecInMillis < System.currentTimeMillis()) {
        arguments.storage.stopGame(da.id, answer)
        arguments.vkClient.sendMessage(arguments.userId, timeIsUp)
        return
    }
    val q = arguments.storage.findQuestionById(da.questionId)
    if (null == q) {
        arguments.storage.stopGame(da.id, answer)
        arguments.vkClient.sendMessage(arguments.userId, unusualShit)
        return
    }
    val message = when {
        answer == q.answer -> {
            arguments.storage.stopGame(da.id, answer, true)
            correctAnswerMessage { q.promocode }
        }
        damerauLevenshteinDistance(answer, q.answer) <= 2 -> inaccurateAnswer
        else -> {
            arguments.storage.stopGame(da.id, answer)
            incorrectAnswer
        }
    }
    arguments.vkClient.sendMessage(arguments.userId, message)
}

@KtorExperimentalAPI
internal suspend fun handleEditing(arguments: CommandHandlerArguments) {
    if (!arguments.isAdmin) return
    val parts = arguments.text
        .removePrefix(arguments.command)
        .split('%')
        .map { it.trim() }
    if (parts.count() != 3) {
        arguments.vkClient.sendMessage(arguments.userId, errorWhileEditingQuestion)
        return
    }
    val (qIdString, text, answer) = parts
    if (text.isBlank() && answer.isBlank()) {
        arguments.vkClient.sendMessage(arguments.userId, bothQuestionFieldsAreBlank)
        return
    }
    val qId = try {
        qIdString.toInt()
    } catch (e: NumberFormatException) {
        arguments.vkClient.sendMessage(arguments.userId, questionIdWasNotRecognized)
        return
    }
    if (!arguments.storage.questionExists(qId)) {
        arguments.vkClient.sendMessage(arguments.userId, questionWasNotFound)
        return
    }
    arguments.storage.updateQuestion(qId, text, answer)
    arguments.vkClient.sendMessage(arguments.userId, questionHasBeenUpdated)
}

@KtorExperimentalAPI
internal suspend fun handleWinners(arguments: CommandHandlerArguments) {
    if (!arguments.isAdmin) return
    val stringQId = arguments.text.removePrefix(arguments.command).trim()
    val winners = if (stringQId.isBlank()) {
        arguments.storage.getWinnersForActiveQuestion()
    } else {
        val qId = try {
            stringQId.toInt()
        } catch (e: NumberFormatException) {
            arguments.vkClient.sendMessage(arguments.userId, questionIdWasNotRecognized)
            return
        }
        if (!arguments.storage.questionExists(qId)) {
            arguments.vkClient.sendMessage(arguments.userId, questionWasNotFound)
            return
        }
        arguments.storage.getWinnersByQuestionId(qId)
    }
    val fullNames = arguments.vkClient.getFullNamesByIds(winners.keys)
    val message = fullNames.map {
        //null check is useless because of the query condition but required because
        //i found no way to force null column to became a non-null map value
        //@see club.liefuck.data.Storage.getWinnersByQuestionId
        it.value + winners[it.key]?.let { ts -> tsAsDateString(ts) }
    }.joinToString(", ", winnersListPrefix)
    arguments.vkClient.sendMessage(arguments.userId, message)
}

@KtorExperimentalAPI
internal suspend fun handleList(arguments: CommandHandlerArguments) {
    if (!arguments.isAdmin) return
    val stringOffset = arguments.text.removePrefix(arguments.command).trim()
    val limit = 10;
    val page = try {
        val page = stringOffset.toInt().absoluteValue
        if (page == 0) 1 else page
    } catch (e: NumberFormatException) {
        1
    }
    val offset = if (page == 1) 0 else page * limit
    val questions = arguments.storage.getQuestions(limit, offset)
    val message = if (questions.isEmpty()) {
        noQuestionsFound
    } else {
        questions.joinToString("\n", "$questionsListPrefix$page:\n") {
            questionFormatted(it)
        }
    }
    arguments.vkClient.sendMessage(arguments.userId, message)
}