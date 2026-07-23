package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.EstablishmentFilters
import mg.fikaliako.api.model.exception.BadRequestException

object EstablishmentFilterParams {
  val ESTABLISHMENT_TYPES =
    setOf(
      "restaurant",
      "gargotte",
      "cafe",
      "snack",
      "food_truck",
      "street_vendor",
      "pastry_shop",
      "bar_restaurant",
      "hotel_restaurant",
    )

  fun build(
    type: String?,
    minPrice: Int?,
    maxPrice: Int?,
    cuisine: String?,
    payment: String?,
    filtersCsv: String?,
    openNow: Boolean,
  ): EstablishmentFilters {
    type?.let {
      if (it !in ESTABLISHMENT_TYPES) throw BadRequestException("Unknown establishment type: $it")
    }
    if (minPrice != null && minPrice < 0) throw BadRequestException("min_price must be >= 0.")
    if (maxPrice != null && maxPrice < 0) throw BadRequestException("max_price must be >= 0.")
    if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
      throw BadRequestException("min_price must not exceed max_price.")
    }
    val amenities =
      filtersCsv
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.onEach {
          if (it !in EstablishmentFilters.AMENITY_COLUMNS) {
            throw BadRequestException("Unknown filter: $it")
          }
        }.orEmpty()
    return EstablishmentFilters(
      types = listOfNotNull(type),
      minPrice = minPrice,
      maxPrice = maxPrice,
      cuisine = cuisine,
      payment = payment,
      amenities = amenities,
      openNow = openNow,
    )
  }
}
