## Этот репозиторий создан для знакомства с [Kotlin](https://kotlinlang.org/) и [Ktor](https://ktor.io/). 

Попутно, насколько это возможно, была решена задача написания чат-бота для сообщества в vk. 

Функционал чат-бота:
- приветствовать, объяснять правила, задавать вопрос
- проверять ответ и, если он верный, выдавать промокод
- получать вопрос можно не чаще, чем раз в неделю (7 дней с момента последней игры)
- время на ответ - 30 секунд
- добавлять/удалять/редактировать вопросы
- смотреть статистику по правильным ответам

В качестве бонуса - обработка ответов с опечатками. Точность ответа определяется с помощью [расстояния Дамерау-Левенштейна](https://ru.wikipedia.org/wiki/%D0%A0%D0%B0%D1%81%D1%81%D1%82%D0%BE%D1%8F%D0%BD%D0%B8%D0%B5_%D0%94%D0%B0%D0%BC%D0%B5%D1%80%D0%B0%D1%83_%E2%80%94_%D0%9B%D0%B5%D0%B2%D0%B5%D0%BD%D1%88%D1%82%D0%B5%D0%B9%D0%BD%D0%B0).

Список команд бота разделен на 2 группы:
- команды для игроков (обычных пользователей)
- команды для пользователей сообщества с ролями "создатель" и "администратор"

Роль пользователя определяется вызовом API метода VK.

Потрогать бота можно [вот в этой группе](https://vk.com/public192628443)