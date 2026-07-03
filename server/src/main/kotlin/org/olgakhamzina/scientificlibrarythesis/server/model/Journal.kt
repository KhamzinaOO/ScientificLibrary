package org.olgakhamzina.scientificlibrarythesis.server.model

import jakarta.persistence.*

@Entity
@Table(name = "journals")
data class Journal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "name")
    val name: String = ""
)