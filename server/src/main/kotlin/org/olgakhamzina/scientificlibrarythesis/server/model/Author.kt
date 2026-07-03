package org.olgakhamzina.scientificlibrarythesis.server.model

import jakarta.persistence.*

/**
 * Author – модель автора. Соответствует записи в таблице `authors`.
 * Используется для отображения связей (Many-to-Many между авторами и публикациями через `author_publication`).
 */
@Entity
@Table(name = "authors")
data class Author(
    @Id
    @Column(name = "authorId")
    val authorId: String,    // Уникальный идентификатор автора

    @Column(name = "name")
    val name: String,        // Имя автора

    @Column(name = "hIndex")
    val hIndex: Int          // h-индекс автора
)