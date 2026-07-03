package org.olgakhamzina.scientificlibrarythesis.server

import org.olgakhamzina.scientificlibrarythesis.shared.Publication
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * PublicationService – сервис для получения подробной информации о конкретной публикации.
 */
@Service
class PublicationService(private val publicationRepository: PublicationRepository) {

    /**
     * Ищет публикацию по ее идентификатору.
     * @param paperId идентификатор (строковый)
     * @return Publication, если найден, или выбрасывает исключение 404, если не найден.
     */
    fun getPublicationById(paperId: String): Publication {
        val pub = publicationRepository.findPublicationById(paperId)
        return pub ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Publication not found")
    }
}