package club.liefuck.data

import club.liefuck.startOfWeekInMillis
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class Storage(dataSource: DataSource) {
    init {
        Database.connect(dataSource)

        transaction {
            addLogger(StdOutSqlLogger)

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
        val q = Questions.slice(Questions.text, Questions.id).select { Questions.isActive eq true }.firstOrNull()
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

    fun promocodeIsTaken(code: String): Boolean = transaction {
        null !== Questions.slice(Questions.id).select { Questions.code eq code }.firstOrNull()
    }

    fun playerIsAbleToGetQuestion(playerId: Long): Boolean = transaction {
        val startOfWeekTS = startOfWeekInMillis()
        Answers.select { Answers.askedAt greater startOfWeekTS and (Answers.vkUserId eq playerId) }.count() == 0
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
        val startOfWeekTS = startOfWeekInMillis()
        val a = Answers.slice(Answers.id, Answers.questionId, Answers.askedAt)
            .select { Answers.vkUserId eq playerId and (Answers.answeredAt.isNull()) and (Answers.askedAt greater startOfWeekTS) }
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

