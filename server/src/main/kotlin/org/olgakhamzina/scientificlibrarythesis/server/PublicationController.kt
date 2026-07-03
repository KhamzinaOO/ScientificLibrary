package org.olgakhamzina.scientificlibrarythesis.server

import org.olgakhamzina.scientificlibrarythesis.shared.Publication
import org.springframework.web.bind.annotation.*

/**
 * PublicationController – контроллер для получения подробной информации о публикации по ID.
 */
@RestController
@RequestMapping("/publication")
class PublicationController(private val publicationService: PublicationService) {

    /**
     * GET /publication/{id} – получение подробной информации о публикации по ее идентификатору.
     * В случае, если публикация не найдена, возвращается 404 статус.
     */
    @GetMapping("/{id}")
    fun getPublicationById(@PathVariable id: String): Publication {
        return publicationService.getPublicationById(id)
    }
}