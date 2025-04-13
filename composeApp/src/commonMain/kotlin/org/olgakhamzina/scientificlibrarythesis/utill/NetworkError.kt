package org.olgakhamzina.scientificlibrarythesis.utill

enum class NetworkError(val message: String) : Error {
    REQUEST_TIMEOUT("Превышен лимит ожидания ответа"),
    FORBIDDEN("Ошибка доступа. Выполните повторный вход в приложение"),
    BAD_REQUEST("Некорректный ввод"),
    CONFLICT("Неразрешимый конфликт"),
    TOO_MANY_REQUESTS("Превышено количество запросов к серверу"),
    NO_INTERNET("Отсутствует подключение к интернету"),
    PAYLOAD_TOO_LARGE("Превышена загрузка"),
    SERVER_ERROR("Внутренняя ошибка сервера"),
    SERIALIZATION("Ошибка преобразования данных"),
    CANCELLATION(""),
    UNKNOWN("Неизвестная ошибка");
}