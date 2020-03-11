package club.liefuck.data

data class Question(val id: Int, val text: String, val answer: String, val promocode: String, val addedAt: Long)
data class PlayerQuestion(val id: Int, val text: String)
data class DraftAnswer(val id: Int,  val questionId: Int, val askedAt: Long)