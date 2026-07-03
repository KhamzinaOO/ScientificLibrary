package org.olgakhamzina.scientificlibrarythesis.server.chaos

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.olgakhamzina.scientificlibrarythesis.shared.Publication
import org.olgakhamzina.scientificlibrarythesis.server.SearchController
import org.olgakhamzina.scientificlibrarythesis.server.SearchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SearchController::class)
class SearchControllerChaosTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var searchService: SearchService

    @BeforeEach
    fun setUp() {
        // stub once for *any* filters / page / size
        whenever(searchService.searchPublications(any(), any(), any()))
            .thenReturn(emptyList<Publication>())
    }

    @Test
    fun `when page negative then return 200 with empty list`() {
        mockMvc.get("/search?page=-1") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { json("[]") }
        }
    }

    @Test
    fun `when random string as query param then return 200 with empty list`() {
        mockMvc.get("/search?query=%%%") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { json("[]") }
        }
    }
}