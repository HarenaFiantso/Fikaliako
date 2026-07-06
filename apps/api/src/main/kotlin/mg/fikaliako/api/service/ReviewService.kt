package mg.fikaliako.api.service

import mg.fikaliako.api.exception.BadRequestException
import mg.fikaliako.api.exception.NotFoundException
import mg.fikaliako.api.model.Page
import mg.fikaliako.api.model.ReviewItem
import mg.fikaliako.api.repository.ReviewRepository
import mg.fikaliako.api.util.Cursor
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ReviewService(
    private val repository: ReviewRepository,
) {
    fun listForEstablishment(
        establishmentId: UUID,
        limit: Int?,
        cursorValue: String?,
    ): Page<ReviewItem> {
        if (!repository.establishmentExists(establishmentId)) {
            throw NotFoundException("Establishment '$establishmentId' not found.")
        }
        val cappedLimit = clampLimit(limit)
        val cursor = cursorValue?.let { Cursor.decode(it) }
        val rows = repository.findForEstablishment(establishmentId, cappedLimit + 1, cursor)
        val page = rows.take(cappedLimit)
        val next =
            if (rows.size > cappedLimit) {
                page.lastOrNull()?.let { Cursor(it.createdAt, it.review.id).encode() }
            } else {
                null
            }
        return Page(page.map { it.review }, next)
    }

    private fun clampLimit(limit: Int?): Int {
        val value = limit ?: DEFAULT_LIMIT
        if (value < 1) throw BadRequestException("limit must be at least 1.")
        return value.coerceAtMost(MAX_PAGE_SIZE)
    }

    companion object {
        const val DEFAULT_LIMIT = 20
        const val MAX_PAGE_SIZE = 100
    }
}
