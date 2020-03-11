package club.liefuck.data

import club.liefuck.utf8Collate
import org.jetbrains.exposed.dao.id.IntIdTable

object Questions : IntIdTable() {
    val text = text("text", utf8Collate)
    val answer = text("answer", utf8Collate)
    val code = varchar("promocode", 300, utf8Collate).uniqueIndex()
    val addedAt = long("added_at")
    val isActive = bool("is_active")
}

object Answers : IntIdTable() {
    val vkUserId = long("vk_user_id").index()
    val askedAt = long("asked_at").index()
    val questionId = integer("question_id").references(Questions.id)
    val answer = text("answer", utf8Collate).nullable()
    val answeredAt = long("answered_at").nullable()
    var isCorrect = bool("is_correct").nullable()
}