package com.pv239.beelocal.ui.screens.profile.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.pv239.beelocal.R

/**
 * 8-tier XP ladder used to brand a user's profile with a flavour title and
 * matching colour. Tiers are ordered from the entry-level [Larva] to the
 * end-game [MasterExplorer]; each tier carries the **minimum XP** required
 * to reach it. Use [rankForXp] to resolve the rank for a given XP value and
 * [nextRank] to drive any progression UI.
 *
 * Colours were picked to feel cohesive with the BeeLocal bee/honey palette:
 * the early tiers stay desaturated, the mid-game leans into honey/orange,
 * and the late game shifts towards regal purples and a celebratory gold.
 */
enum class ExplorerRank(
    @StringRes val labelRes: Int,
    val minXp: Int,
    val color: Color,
) {
    Larva(R.string.profile_rank_larva, minXp = 0, color = Color(0xFF9E9E9E)),
    Hatchling(R.string.profile_rank_hatchling, minXp = 50, color = Color(0xFFB08968)),
    WorkerBee(R.string.profile_rank_worker_bee, minXp = 150, color = Color(0xFFE0A800)),
    Forager(R.string.profile_rank_forager, minXp = 500, color = Color(0xFFE07A1F)),
    Scout(R.string.profile_rank_scout, minXp = 1_000, color = Color(0xFF2E8B57)),
    Pathfinder(R.string.profile_rank_pathfinder, minXp = 2_500, color = Color(0xFF1E88E5)),
    HiveMaster(R.string.profile_rank_hive_master, minXp = 5_000, color = Color(0xFF7B3FF2)),
    MasterExplorer(R.string.profile_rank_master_explorer, minXp = 10_000, color = Color(0xFFFFB300)),
}

/**
 * Resolves the highest rank whose [ExplorerRank.minXp] threshold has been
 * met by [xp]. A negative XP value is clamped to [ExplorerRank.Larva].
 */
fun rankForXp(xp: Int): ExplorerRank {
    // Iterate descending so the first match is automatically the highest
    // tier the user qualifies for. This keeps the code resilient if more
    // ranks are inserted later.
    val ordered = ExplorerRank.entries.sortedByDescending { it.minXp }
    return ordered.firstOrNull { xp >= it.minXp } ?: ExplorerRank.Larva
}

/**
 * Returns the next rank above [rank], or `null` if [rank] is already the
 * top tier. Handy for "X XP to next rank" hints.
 */
fun nextRank(rank: ExplorerRank): ExplorerRank? {
    val ordered = ExplorerRank.entries.sortedBy { it.minXp }
    val idx = ordered.indexOf(rank)
    return ordered.getOrNull(idx + 1)
}
