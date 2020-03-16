package club.liefuck.data

import club.liefuck.twoMinutesInMillis
import club.liefuck.weekInMillis
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class Storage(dataSource: DataSource, isProd: Boolean = false) {
    init {
        Database.connect(dataSource)

        transaction {
            if (!isProd) {
                addLogger(StdOutSqlLogger)
            }
            SchemaUtils.create(Questions, Answers)
        }
    }

    fun findQuestionById(questionId: Int): Question? = transaction {
        val q = Questions.select { Questions.id eq questionId }.firstOrNull()
        if (null == q) {
            null
        } else {
            Question(
                q[Questions.id].value,
                q[Questions.text],
                q[Questions.answer],
                q[Questions.code],
                q[Questions.addedAt]
            )
        }
    }

    fun findActiveQuestionForAdmin(): Question? = transaction {
        val q = Questions.select { Questions.isActive eq true }.firstOrNull()
        if (null == q) {
            null
        } else {
            Question(
                q[Questions.id].value,
                q[Questions.text],
                q[Questions.answer],
                q[Questions.code],
                q[Questions.addedAt]
            )
        }
    }

    fun findActiveQuestionForPlayer(): PlayerQuestion? = transaction {
        val q = Questions.leftJoin(Answers)
            .slice(Questions.text, Questions.id)
            .select { Questions.isActive eq true and (Answers.questionId.isNull()) }
            .firstOrNull()
        if (null == q) {
            null
        } else {
            PlayerQuestion(
                q[Questions.id].value,
                q[Questions.text]
            )
        }
    }

    fun addQuestion(text: String, answer: String, code: String): Int = transaction {
        val questionId = Questions.insertAndGetId {
            it[Questions.text] = text
            it[Questions.answer] = answer
            it[Questions.code] = code
            it[addedAt] = System.currentTimeMillis()
            it[isActive] = true
        }

        Questions.update({ (Questions.isActive eq true) and (Questions.id neq questionId) }) {
            it[isActive] = false
        }

        questionId.value
    }

    fun deleteQuestion(questionId: Int) = transaction {
        Questions.deleteWhere { Questions.id eq questionId }
    }

    fun questionExists(questionId: Int): Boolean = transaction {
        Questions.slice(Questions.id).select { Questions.id eq questionId }.count() == 1
    }

    fun questionHasAnswers(questionId: Int): Boolean = transaction {
        Answers.slice(Answers.id).select { Answers.questionId eq questionId }.count() > 0
    }

    fun updateQuestion(questionId: Int, newText: String, newAnswer: String) = transaction {
        Questions.update({ Questions.id eq questionId }) {
            when {
                newText.isBlank() -> it[answer] = newAnswer
                newAnswer.isBlank() -> it[text] = newText
                else -> {
                    it[answer] = newAnswer
                    it[text] = newText
                }
            }
        }
    }

    fun getQuestions(limit: Int = 10, offset: Int): List<Question> = transaction {
        Questions.selectAll().limit(limit, offset).sortedByDescending { Questions.id }.map {
            Question(
                it[Questions.id].value,
                it[Questions.text],
                it[Questions.answer],
                it[Questions.code],
                it[Questions.addedAt]
            )
        }
    }

    //i was unable to make it a Map<Long, Long> even using filter{it.answeredAt != null}
    fun getWinnersForActiveQuestion(): Map<Long, Long?> = transaction {
        Answers.join(Questions, JoinType.INNER, additionalConstraint = { Questions.isActive eq true })
            .slice(Answers.vkUserId, Answers.answeredAt)
            .select { Answers.isCorrect eq true and (Answers.answeredAt.isNotNull()) }
            .map { it[Answers.vkUserId] to it[Answers.answeredAt] }
            .toMap()
    }

    //i was unable to make it a Map<Long, Long> even using filter{it.answeredAt != null}
    fun getWinnersByQuestionId(questionId: Int): Map<Long, Long?> = transaction {
        Answers.slice(Answers.vkUserId, Answers.answeredAt)
            .select { Answers.isCorrect eq true and (Answers.answeredAt.isNotNull()) and (Answers.questionId eq questionId) }
            .map { it[Answers.vkUserId] to it[Answers.answeredAt] }
            .toMap()
    }

    fun promocodeIsTaken(code: String): Boolean = transaction {
        null !== Questions.slice(Questions.id).select { Questions.code eq code }.firstOrNull()
    }

    fun playerIsAbleToGetQuestion(playerId: Long): Boolean = transaction {
        val nowInMillis = System.currentTimeMillis()
        Answers.select {
            Answers.vkUserId eq playerId and ((Answers.askedAt + weekInMillis) greaterEq nowInMillis)
        }.count() == 0
    }

    fun startGame(playerId: Long, questionId: Int) = transaction {
        Answers.insert {
            it[Answers.questionId] = questionId
            it[vkUserId] = playerId
            it[askedAt] = System.currentTimeMillis()
        }
    }

    fun stopGame(answerId: Int, answer: String, isCorrect: Boolean = false) = transaction {
        Answers.update({ Answers.id eq answerId }) {
            it[answeredAt] = System.currentTimeMillis()
            it[Answers.answer] = answer
            it[Answers.isCorrect] = isCorrect
        }
    }

    fun findDraftAnswer(playerId: Long): DraftAnswer? = transaction {
        val nowInMillis = System.currentTimeMillis()
        val a = Answers.slice(Answers.id, Answers.questionId, Answers.askedAt)
            .select {
                Answers.vkUserId eq playerId and
                        (Answers.answeredAt.isNull()) and
                        ((Answers.askedAt + twoMinutesInMillis) greaterEq nowInMillis)
            }
            .limit(1)
            .sortedByDescending { Answers.id }
            .singleOrNull()
        if (null == a) {
            null
        } else {
            DraftAnswer(a[Answers.id].value, a[Answers.questionId], a[Answers.askedAt])
        }
    }
}

