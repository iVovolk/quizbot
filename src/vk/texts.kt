package club.liefuck.vk

import club.liefuck.data.Question
import club.liefuck.tsAsDateString
import java.text.SimpleDateFormat

const val answerWithNoQuestion = "Вижу ответ, но не помню, чтобы что-то у тебя спрашивал.\n¯\\_(ツ)_/¯"
const val noActiveQuestion = "Команду понял, но спросить-то нечего.\n¯\\_(ツ)_/¯"
const val noActiveQuestionAdmin = "Ты еще не добавил ни одного вопроса. Или все вопросы неактивны."
const val timeIsUp = "Ответ вижу, но время вышло. Детали в /правила"
const val weeklyLimitExceed = "Мы уже играли на этой неделе. Детали в /правила"
const val promocodeIsTaken = "Такой промокод уже был."
const val errorWhileQuestionDeletion =
    "Я понял, что ты хотел удалить вопрос, но какой - не понял. Формат: \"/удалить id\", где id - число."
const val questionPrefix = "Чтобы ответить, пиши \"/ответ твой_ответ\". У тебя 30 секунд, погнали!"
const val questionWasNotFound = "Не нашел вопроса с таким id."
const val questionHasAnswers = "Не могу удалить вопрос, на который есть ответы."
const val questionHasBeenDeleted = "Вопрос успешно удалён."
const val emptyAnswer = "Ответ без ответа - промокод без промокода.\n¯\\_(ツ)_/¯"
const val unusualShit = "У меня в базе какая-то фигня. Написал админу. Извинити("
const val inaccurateAnswer = "Ммммм.. ц... перечитай-ка внимательно свой ответ и давай еще разок."
const val incorrectAnswer = "Неть. Приходи через неделю.\n¯\\_(ツ)_/¯"
const val adminAnswering =
    "Ты правда хочешь промокод? Есть у меня один пожизненный. С ним вообще все везде бесплатно будет: IMNOTGAYBUT$20IS$20"
const val errorWhileAddingQuestion = """
Понял, что надо добавить новый вопрос, но чот не вышло. 
Проверь формат, он должен быть такой: "/добавить вопрос%ответ%промокод". 
Именно в таком порядке и части разделены символом %.
"""
const val errorWhileEditingQuestion = """
Понял, что хочешь поправить вопрос, а дальше, как в тумане.
Проверь формат, он должен быть такой: "/править id%вопрос%ответ". 
Именно в таком порядке и части разделены символом %.
Ты можешь не писать часть, которую не нужно править, просто ничего не пиши в ней. Главное - ставь знак %. 
Например "/править id%%новый ответ".
"""
const val bothQuestionFieldsAreBlank = "Ха-ха. Пустой вопрос - пустой ответ. Умно. Башка у тебя пустая."
const val questionIdWasNotRecognized = "Не понял id вопроса, там число длолжно быть, а не вот это вот."
const val questionHasBeenUpdated = "Гтово, босс, вопрос обновлен."
const val questionsListPrefix = "Список вопросов от новых к старым. Стр."
const val noQuestionsFound = "Страница пуста, как твой загран или как сердце твоей бывшей"
const val winnersListPrefix = "Правильно ответили: "
const val rulesPrefix = "Напоминаю правила.\n"
const val pingPrefix = "Я тут. "

const val adminCommands = """
Список команд, доступных для пользователей сообщества с ролями Создатель или Администратор:
- "/вопрос" - покажу текущий активный вопрос вместе с промокодом и датой добавления;
- "/список 3" - выведу список вопросов с их id, чтобы можно было удалить или редактировать вопрос по id. Выведу по 10 штук. 3 - номер страницы. Если не указать - покажу 10 последних;
- "/удалить 42" - удалю вопрос с id 42. Только нельзя удалить вопрос, если на него уже есть ответы, имей в виду;
- "/добавить Главный вопрос жизни смерти и всего такого?%42%PROMOCD" - добавлю новый вопрос с текстом "Главный вопрос жизни смерти и всего такого?" с ответом "42" и промокодом "PROMOCD";
- "/править 12%вопрос%ответ" - заменю текст вопроса и ответ, на указаннные, в вопросе с id 12. Ты можешь не писать часть, которую не нужно править, просто ничего не пиши. Главное - ставь знак %.;  
- "/угадали 13" - выведу список пользователей, которые правильно ответили на вопрос. Если не указать id, покажу по текущему вопросу, указать - по указанному;
- "/правила" - покажу правила для игроков;
- "/бот" - пнуть меня, чтобы я показал список доступных команд;
- "/помощь" - напомню, как со мной работать.    
"""

const val playerCommands = """
Я понимаю такие команды:
- "/вопрос" - пришлю вопрос, если позволяют правила. Про них ниже;
- "/правила" - напомню правила;
- "/бот" - пнуть меня, чтобы я показал список доступных команд;
- "/помощь" - напомню, как со мной работать.
Чтобы ответить на вопрос, напиши ответ в формате "/ответ _ответ_на_вопрос_".
Например, я задал тебе вопрос, на который ответом будет "колбаса". Значит, мне нужно послать сообщение вида:
/ответ колбаса
"""

const val help = """
Чтобы начать, отправь мне сообщение-команду с текстом /бот. Если у тебя доступны кнопки-команды, они покажутся под полем ввода текста. 
Если нет - команды придется воодить вручную. Их список будет ниже.
Текстовые команды я понимаю всегда. Так что, если тебе проще и удобней, можешь писать мне команды вручную, даже если у тебя доступна клавиатура.
"""

const val playerHelp = "$help\n$playerCommands"

const val adminHelp = "$help\n$adminCommands"

const val rules = """
На ответ тебе дается 30 секунд. Если твой ответ покажется мне неточным (опечатки), я могу переспросить. Так что, печатай аккрутано ;).
Получать от меня вопрос можно не чаще, чем раз в неделю. 
"""

fun correctAnswerMessage(fn: () -> String): String {
    return "Тыдыщ! В точку! Лови промокод на одного человека: ${fn()} "
}

fun questionAddedMessage(fn: () -> String): String {
    return "Вопрос успешно добавлен, его id ${fn()}"
}

fun activeQuestionFormatted(q: Question): String {

    return """
        Текущий активный вопрос имеет id ${q.id} и звучит так:
        ${q.text}
        Ответ: ${q.answer}
        Промокод: ${q.promocode}
        Добавлен: ${SimpleDateFormat("dd.MM.yyyy HH:mm").format(q.addedAt)}
    """.trimIndent()
}

fun questionFormatted(q: Question): String {
    return "${q.id}: ${q.text} - ${q.answer}. Код: ${q.promocode}. Добален: ${tsAsDateString(q.addedAt)}"
}

fun helloPlayer(userName: String?): String {
    return """
        Привет${if (null != userName) ", $userName" else ""}. Я - бот, у которого ты можешь получить промокод на бесплатную игру если правильно ответишь на вопрос.
        $playerHelp
        $rules   
    """.trimIndent()
}


fun helloAdmin(userName: String?): String {
    return """
        Привет${if (null != userName) ", $userName" else ""}. Я - бот, который задает вопросы и раздет промокоды.
        $adminHelp
    """.trimIndent()
}
