package com.pv239.beelocal.domain

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Wraps a single page of Firestore results together with the cursor needed
 * to fetch the next page.
 *
 * @param T       The model type contained in this page.
 * @param items   The items on this page. Empty list means no more pages exist.
 * @param cursor  The last [DocumentSnapshot] on this page. Pass it to the
 *                next paginated call as [lastVisible]. Null when [items] is empty.
 */
data class Page<T>(
    val items: List<T>,
    val cursor: DocumentSnapshot?
) {
    /** True when there are no further pages to load. */
    val isLastPage: Boolean get() = items.isEmpty() || cursor == null
}