package mg.fikaliako.api.repository

import mg.fikaliako.api.model.ReviewItem
import mg.fikaliako.api.util.Cursor
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class ReviewRepository(
    private val jdbc: JdbcClient,
) {
    data class Row(
        val review: ReviewItem,
        val createdAt: Instant,
    )

    /** Published reviews for an establishment, newest first, keyset-paginated. */
    fun findForEstablishment(
        establishmentId: UUID,
        limit: Int,
        cursor: Cursor?,
    ): List<Row> {
        val cursorClause =
            if (cursor != null) "AND (rv.created_at, rv.id) < (:cursorCreatedAt, :cursorId)" else ""
        val sql =
            """
            SELECT rv.id, u.display_name AS author_name,
                   rv.rating_quality, rv.rating_price, rv.rating_cleanliness,
                   rv.rating_speed, rv.rating_welcome, rv.global_note, rv.comment, rv.created_at
            FROM reviews rv
            JOIN users u ON u.id = rv.user_id
            WHERE rv.establishment_id = :establishmentId AND rv.status = 'published' $cursorClause
            ORDER BY rv.created_at DESC, rv.id DESC
            LIMIT :limit
            """.trimIndent()

        val spec =
            jdbc
                .sql(sql)
                .param("establishmentId", establishmentId)
                .param("limit", limit)
        if (cursor != null) {
            spec.param("cursorCreatedAt", cursor.createdAt.atOffset(java.time.ZoneOffset.UTC))
            spec.param("cursorId", cursor.id)
        }
        return spec
            .query { rs, _ ->
                Row(
                    ReviewItem(
                        id = rs.getObject("id", UUID::class.java),
                        authorName = rs.getString("author_name"),
                        ratingQuality = rs.getInt("rating_quality"),
                        ratingPrice = rs.getInt("rating_price"),
                        ratingCleanliness = rs.getInt("rating_cleanliness"),
                        ratingSpeed = rs.getInt("rating_speed"),
                        ratingWelcome = rs.getInt("rating_welcome"),
                        globalNote = rs.getBigDecimal("global_note"),
                        comment = rs.getString("comment"),
                        createdAt = rs.getTimestamp("created_at").toInstant(),
                    ),
                    rs.getTimestamp("created_at").toInstant(),
                )
            }.list()
    }

    fun establishmentExists(id: UUID): Boolean =
        jdbc
            .sql("SELECT EXISTS (SELECT 1 FROM establishments WHERE id = :id)")
            .param("id", id)
            .query(Boolean::class.java)
            .single()
}
