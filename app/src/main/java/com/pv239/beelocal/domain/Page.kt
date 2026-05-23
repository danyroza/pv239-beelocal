package com.pv239.beelocal.domain

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Wraps a single page of Firestore results.
 *
 * @param T        The model type contained in this page.
 * @param items    The items on this page.
 * @param cursor   Firestore cursor ([DocumentSnapshot]) for the next page.
 *                 Only non-null when [hasMore] is true. For feed queries backed
 *                 by multiple [whereIn] chunks this is always null — use
 *                 [hasMore] to decide whether to load more.
 * @param hasMore  Explicitly set by the repository: true only when the fetched
 *                 count equals the requested page size, meaning a further page
 *                 likely exists.
 */
data class Page<T>(
    val items: List<T>,
    val cursor: DocumentSnapshot?,
    val hasMore: Boolean
) {
    /** True when there are no further pages to load. */
    val isLastPage: Boolean get() = !hasMore
}