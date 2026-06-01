package com.pv239.beelocal.domain

enum class FirestoreCollections(val value: String) {
    USERS("users"),
    USER_STATISTICS("user_statistics"),
    DAILY_CHALLENGES("daily_challenges"),
    DAILY_COMPLETIONS("daily_completions"),
    FEED("feed"),
    ROUTES("routes"),
    REVIEWS("reviews"),
    BINGO_CARDS("bingo_cards"),
    BINGO_PROGRESS("bingo_progress"),
    BINGO_TASK_COMPLETIONS("bingo_task_completions"),
    FOLLOW_REQUESTS("follow_requests")
}
