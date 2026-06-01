package com.pv239.beelocal.domain

/**
 * Centralised XP economy constants. Adjusting a number here propagates to both
 * the repository transactions that award XP and the UI labels that advertise
 * the cost/reward to the user, so the two cannot drift out of sync.
 */
object XpRewards {
    /** Full reward for completing the daily challenge with no hints unlocked. */
    const val DAILY_CHALLENGE_FULL = 200

    /**
     * Cost of unlocking a single hint (direction or map) on the daily challenge.
     * Each hint deducts this from the eventual submission reward, so using both
     * still leaves a positive net reward of `DAILY_CHALLENGE_FULL - 2 * DAILY_CHALLENGE_HINT_COST`.
     */
    const val DAILY_CHALLENGE_HINT_COST = 50

    /** Reward for completing a single bingo task (any cell on the weekly card). */
    const val BINGO_TASK = 20

    /** One-time bonus awarded when all 16 cells on the weekly bingo card are completed. */
    const val BINGO_CARD_FULL = 100

    /** How many cells a full bingo card has. */
    const val BINGO_CARD_SIZE = 16

    /**
     * Reward for completing a route (all checkpoints answered correctly and the
     * journey wrapped up via [RouteRepository.completeRoute]). Awarded exactly
     * once per (user, route) pair — re-finishing an already-completed route
     * does not re-award XP.
     */
    const val ROUTE_COMPLETION = 100


    /**
     * Computes the XP reward for a daily-challenge submission given how many
     * paid hints (direction, map) the user unlocked beforehand.
     *
     * Defensive on both ends:
     *  - Negative inputs are clamped to 0 so a buggy caller can't accidentally
     *    over-credit XP above [DAILY_CHALLENGE_FULL].
     *  - The result is clamped to 0 so future tweaks (more hints, higher cost)
     *    can't yield a negative reward.
     */
    fun dailyChallengeReward(hintsUnlocked: Int): Int {
        val safeHints = hintsUnlocked.coerceAtLeast(0)
        return (DAILY_CHALLENGE_FULL - safeHints * DAILY_CHALLENGE_HINT_COST)
            .coerceAtLeast(0)
    }
}
