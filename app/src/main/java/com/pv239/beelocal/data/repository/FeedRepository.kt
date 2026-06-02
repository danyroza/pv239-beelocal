package com.pv239.beelocal.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pv239.beelocal.domain.FirestoreCollections
import com.pv239.beelocal.domain.FirestoreConfig.FEED_PAGE_SIZE
import com.pv239.beelocal.domain.Page
import com.pv239.beelocal.model.FeedEntry
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read/write access to the friends-feed collection.
 *
 * Writes generally happen via the feature-specific repositories
 * (e.g. [DailyChallengeRepository.shareChallengeToFeed],
 * [BingoRepository.shareBingoToFeed], [RouteRepository.shareRouteCompletionToFeed])
 * so that the feed entry is created atomically with the "sharedToFeed" flip on
 * the originating document. This repository exposes the reads consumed by the
 * Social screen plus a generic [addFeedEntry] for non-transactional cases.
 */
@Singleton
class FeedRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val feedCollection get() = firestore.collection(FirestoreCollections.FEED.value)

    /**
     * Returns a page of feed entries from the given friends, newest first.
     *
     * Because Firestore's [whereIn] is capped at 10 IDs and cursors are
     * query-shape-specific, a single merged [com.google.firebase.firestore.DocumentSnapshot]
     * cursor cannot safely be shared across chunks. Instead, each chunk fetches one extra
     * item ([FEED_PAGE_SIZE] + 1) so we can detect whether more data exists without
     * exposing a broken cursor. The merged result is trimmed to [FEED_PAGE_SIZE].
     *
     * [Page.cursor] is always null for feed pages. Use [Page.hasMore] to decide
     * whether to show a "load more" control; offset-based or timestamp-based
     * continuation is left to the caller if deeper paging is required.
     */
    suspend fun getFriendsFeed(friendIds: List<String>): Page<FeedEntry> {
        if (friendIds.isEmpty()) return Page(emptyList(), null, hasMore = false)

        val fetchLimit = FEED_PAGE_SIZE + 1 // +1 to probe for a next page

        val allDocuments = friendIds.chunked(WHERE_IN_CHUNK_SIZE).flatMap { chunk ->
            feedCollection
                .whereIn("userId", chunk)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(fetchLimit)
                .get()
                .await()
                .documents
        }

        val sorted = allDocuments.sortedByDescending { it.getTimestamp("timestamp") }
        val hasMore = sorted.size > FEED_PAGE_SIZE.toInt()
        val pageDocuments = sorted.take(FEED_PAGE_SIZE.toInt())

        return Page(
            items = pageDocuments.mapNotNull { it.toObject(FeedEntry::class.java) },
            cursor = null,
            hasMore = hasMore,
        )
    }

    suspend fun addFeedEntry(entry: FeedEntry) {
        feedCollection.add(entry).await()
    }

    companion object {
        /** Firestore's hard cap on the size of a `whereIn` filter value list. */
        private const val WHERE_IN_CHUNK_SIZE = 10
    }
}
