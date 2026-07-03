package org.olgakhamzina.scientificlibrarythesis.server

import org.olgakhamzina.scientificlibrarythesis.shared.Publication
import org.olgakhamzina.scientificlibrarythesis.shared.SearchFilters
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class PublicationRepository(private val jdbcTemplate: JdbcTemplate) {

    private val baseSelectQuery: String = """
        SELECT
            p.paperId,
            p.title,
            p.abstract,
            CASE WHEN p.venue = '' THEN IFNULL(pv.name, '') ELSE p.venue END AS venue,
            p.year,
            p.publicationDate,
            p.citationCount,
            p.influentialCitationCount,
            IFNULL(j.name, '') AS journalName,
            p.isOpenAccess,
            IFNULL(GROUP_CONCAT(DISTINCT a.name SEPARATOR ', '), '') AS authors,
            IFNULL(GROUP_CONCAT(DISTINCT aa.affiliation SEPARATOR ' '), '') AS affiliations,
            (SELECT IFNULL(AVG(a2.hIndex), 0) FROM authors a2
             JOIN author_publication ap2 ON a2.authorId = ap2.author_id
             WHERE ap2.publication_id = p.paperId) AS avgHIndex,
            IFNULL(GROUP_CONCAT(DISTINCT pfs.fieldName SEPARATOR ', '), '') AS fields,
            IFNULL((SELECT GROUP_CONCAT(DISTINCT ps.category SEPARATOR ', ') FROM paper_s2FieldsOfStudy ps
                    WHERE ps.paperId = p.paperId), '') AS f2fields,
            IFNULL((SELECT GROUP_CONCAT(DISTINCT ppt.typeName SEPARATOR ', ') FROM paper_publicationTypes ppt
                    WHERE ppt.paperId = p.paperId), '') AS pubTypes,
            IFNULL(GROUP_CONCAT(DISTINCT t.text SEPARATOR ' '), '') AS tldr,
            MAX(IFNULL(oap.url, '')) AS openAccessPdfUrl
        FROM publications p
        LEFT JOIN journals j ON p.journal_id = j.id
        LEFT JOIN publication_venues pv ON p.publicationVenue_id = pv.id
        LEFT JOIN author_publication ap ON p.paperId = ap.publication_id
        LEFT JOIN authors a ON ap.author_id = a.authorId
        LEFT JOIN author_affiliations aa ON a.authorId = aa.authorId
        LEFT JOIN paper_fieldsOfStudy pfs ON p.paperId = pfs.paperId
        LEFT JOIN tldrs t ON p.paperId = t.paperId
        LEFT JOIN open_access_pdfs oap ON p.paperId = oap.paperId
    """.trimIndent()

    fun findAllPublications(): List<Publication> {
        val sql = "$baseSelectQuery GROUP BY p.paperId"
        return jdbcTemplate.query(sql) { rs, _ -> mapRowToPublication(rs) }
    }

    fun findPublicationsByFilters(filters: SearchFilters): List<Publication> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (filters.query.isNotBlank()) {
            conditions += "MATCH(p.title, p.abstract) AGAINST(? IN NATURAL LANGUAGE MODE)"
            params += filters.query
        }
        filters.year?.let {
            conditions += "p.year = ?"
            params += it
        }
        filters.openAccess?.let {
            conditions += "p.isOpenAccess = ?"
            params += it
        }
        filters.authors?.takeIf { it.isNotEmpty() }?.let { authorList ->
            conditions += "EXISTS (SELECT 1 FROM author_publication ap2 JOIN authors a2 ON ap2.author_id = a2.authorId WHERE ap2.publication_id = p.paperId AND a2.name IN (${authorList.joinToString(",") { "?" }}))"
            params.addAll(authorList)
        }
        filters.journals?.takeIf { it.isNotEmpty() }?.let { journalList ->
            conditions += "j.name IN (${journalList.joinToString(",") { "?" }})"
            params.addAll(journalList)
        }
        filters.venues?.takeIf { it.isNotEmpty() }?.let { venueList ->
            conditions += "(p.venue IN (${venueList.joinToString(",") { "?" }}) OR pv.name IN (${venueList.joinToString(",") { "?" }}))"
            params.addAll(venueList)
            params.addAll(venueList) // для p.venue и pv.name
        }
        filters.pubTypes?.takeIf { it.isNotEmpty() }?.let { typeList ->
            conditions += "EXISTS (SELECT 1 FROM paper_publicationTypes ppt WHERE ppt.paperId = p.paperId AND ppt.typeName IN (${typeList.joinToString(",") { "?" }}))"
            params.addAll(typeList)
        }
        filters.hindexFrom?.let {
            conditions += "(SELECT IFNULL(AVG(a2.hIndex),0) FROM authors a2 JOIN author_publication ap2 ON a2.authorId=ap2.author_id WHERE ap2.publication_id=p.paperId) >= ?"
            params += it
        }
        filters.hindexTo?.let {
            conditions += "(SELECT IFNULL(AVG(a2.hIndex),0) FROM authors a2 JOIN author_publication ap2 ON a2.authorId=ap2.author_id WHERE ap2.publication_id=p.paperId) <= ?"
            params += it
        }
        filters.citationsFrom?.let {
            conditions += "p.citationCount >= ?"
            params += it
        }
        filters.citationsTo?.let {
            conditions += "p.citationCount <= ?"
            params += it
        }
        filters.dateFrom?.let {
            conditions += "p.publicationDate >= ?"
            params += it
        }
        filters.dateTo?.let {
            conditions += "p.publicationDate <= ?"
            params += it
        }

        val whereClause = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""
        val sql = "$baseSelectQuery $whereClause GROUP BY p.paperId"

        return jdbcTemplate.query(sql, params.toTypedArray()) { rs, _ -> mapRowToPublication(rs) }
    }

    private fun mapRowToPublication(rs: ResultSet): Publication = Publication(
        paperId = rs.getString("paperId"),
        title = rs.getString("title").orEmpty(),
        abstractText = rs.getString("abstract").orEmpty(),
        venue = rs.getString("venue").orEmpty(),
        journal = rs.getString("journalName").orEmpty(),
        year = rs.getInt("year"),
        publicationDate = rs.getString("publicationDate") ?: LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_DATE),
        citationCount = rs.getInt("citationCount"),
        influentialCitationCount = rs.getInt("influentialCitationCount"),
        avgHIndex = rs.getDouble("avgHIndex").takeIf { !rs.wasNull() } ?: 10.0,
        authors = rs.getString("authors").orEmpty(),
        fields = rs.getString("fields").orEmpty().split(", ").filter { it.isNotEmpty() },
        f2Fields = rs.getString("f2fields").orEmpty().split(", ").filter { it.isNotEmpty() },
        publicationTypes = rs.getString("pubTypes").orEmpty().split(", ").filter { it.isNotEmpty() },
        tldr = rs.getString("tldr").orEmpty(),
        isOpenAccess = rs.getBoolean("isOpenAccess"),
        openAccessPdfUrl = rs.getString("openAccessPdfUrl").orEmpty()
    ).also {
        it.affiliations = rs.getString("affiliations").orEmpty()
        it.fullText = listOf(it.title, it.abstractText, it.authors, it.affiliations, it.tldr).joinToString(" ")
    }

    fun findPublicationById(paperId: String): Publication? {
        val sql = "$baseSelectQuery WHERE p.paperId = ? GROUP BY p.paperId"
        return jdbcTemplate.query(sql, arrayOf(paperId)) { rs, _ ->
            mapRowToPublication(rs)
        }.firstOrNull()
    }
}