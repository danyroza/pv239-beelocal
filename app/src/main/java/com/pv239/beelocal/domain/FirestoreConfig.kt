package com.pv239.beelocal.domain

/**
 * Central configuration for all Firestore query limits and page sizes.
 *
 * Adjust these values to tune bandwidth usage and UX without touching
 * repository logic.
 */
object FirestoreConfig {
    /** Maximum users returned per search query page. */
    const val USERS_SEARCH_PAGE_SIZE: Long = 20

    /** Number of feed entries loaded per page. Applied per whereIn chunk;
     *  the final merged page is also trimmed to this size. */
    const val FEED_PAGE_SIZE: Long = 25

    /** Routes returned per page when browsing by city,
     *  ordered by rating descending. */
    const val ROUTES_BY_CITY_PAGE_SIZE: Long = 20
}