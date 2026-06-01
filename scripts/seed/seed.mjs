#!/usr/bin/env node
/**
 * BeeLocal Firestore seed entry point.
 *
 * Usage:
 *   node seed.mjs                                 Additive seed (mock users + content)
 *   node seed.mjs --wipe                          Wipe all seeded collections, then re-seed
 *   node seed.mjs --current-user <uid>            Anchor data around your real signed-in account
 *                                                 (adds friends, daily completions, bingo + route
 *                                                 progress to this account on top of mock data)
 *   node seed.mjs --current-user <uid> --only-current-user
 *                                                 Seed ONLY the data belonging to that user
 *                                                 (skips creating mock users, daily challenges,
 *                                                 routes, bingo cards). Useful when those already
 *                                                 exist and you only want to wire your account in.
 *   node seed.mjs --wipe --current-user <uid>     Common workflow: full reset + anchor to me
 *
 * Authentication: drop a Firebase service-account JSON at
 * `scripts/seed/serviceAccountKey.json` — see README.md.
 */

import { initFirestore, admin } from './lib/firestore.mjs';
import {
  ts,
  geo,
  daysFromNow,
  pick,
  recursiveDeleteCollection,
  commitWrites,
} from './lib/helpers.mjs';
import { MOCK_USERS, MOCK_USER_STATS } from './data/users.mjs';
import { MOCK_DAILY_CHALLENGES } from './data/dailyChallenges.mjs';
import { MOCK_ROUTES, MOCK_ROUTE_REVIEWS } from './data/routes.mjs';
import { MOCK_BINGO_CARDS, CURRENT_CARD_ID } from './data/bingo.mjs';

// ---------------------------------------------------------------------------
// CLI parsing
// ---------------------------------------------------------------------------

function parseArgs(argv) {
  const args = { wipe: false, currentUser: null, onlyCurrentUser: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--wipe') args.wipe = true;
    else if (a === '--only-current-user') args.onlyCurrentUser = true;
    else if (a === '--current-user') {
      args.currentUser = argv[++i];
      if (!args.currentUser) throw new Error('--current-user requires a value');
    } else if (a === '-h' || a === '--help') {
      printHelp();
      process.exit(0);
    } else {
      console.warn(`⚠️  Unknown argument: ${a}`);
    }
  }
  if (args.onlyCurrentUser && !args.currentUser) {
    throw new Error('--only-current-user requires --current-user <uid>');
  }
  return args;
}

function printHelp() {
  console.log(`
BeeLocal Firestore seeder

Flags:
  --wipe                    Recursively delete every seeded collection first.
  --current-user <uid>      Also wire data around this real account.
  --only-current-user       Seed ONLY that user's data; skip global mock content.
  -h, --help                Show this help.
`);
}

// Collections owned/managed by this seeder. Wiping them is enough to get a
// fully clean state; we never touch collections we don't know about.
const SEEDED_COLLECTIONS = [
  'users',
  'user_statistics',
  'daily_challenges',
  'feed',
  'routes',
  'bingo_cards',
  'bingo_progress',
  'bingo_task_completions',
  'follow_requests',
  'route_progress',
  'route_completions',
];

// ---------------------------------------------------------------------------
// Wipe
// ---------------------------------------------------------------------------

async function wipe(db) {
  console.log('🧹 Wiping seeded collections...');
  for (const path of SEEDED_COLLECTIONS) {
    process.stdout.write(`   - ${path}... `);
    await recursiveDeleteCollection(db, path);
    console.log('done');
  }
}

// ---------------------------------------------------------------------------
// Mock content seeders (global, shared across all test accounts)
// ---------------------------------------------------------------------------

async function seedUsers(db) {
  console.log('👤 Seeding users + user_statistics...');
  const writes = [];
  for (const u of MOCK_USERS) {
    const userDoc = {
      username: u.username,
      // The repository writes `usernameNormalized` on save; mirror that here so
      // the username-search query in FirestoreRepository.searchUsers works as-is.
      usernameNormalized: u.username.toLowerCase().trim(),
      email: u.email,
      profileImageUrl: u.profileImageUrl,
      friends: [],
      isProfilePublic: u.isProfilePublic,
      notificationSettings: { emailEnabled: true, phoneEnabled: true },
    };
    writes.push({ ref: db.collection('users').doc(u.id), data: userDoc });

    const stats = MOCK_USER_STATS[u.id] ?? { streak: 0, xp: 0 };
    writes.push({
      ref: db.collection('user_statistics').doc(u.id),
      data: {
        userId: u.id,
        streak: stats.streak,
        xp: stats.xp,
        lastStreakUpdate: daysFromNow(0),
      },
    });
  }

  // Build a small friend graph among mock users so social/feed views are
  // interesting even before a real user is wired in.
  const friendships = {
    'mock-user-anna': ['mock-user-ben', 'mock-user-david'],
    'mock-user-ben': ['mock-user-anna', 'mock-user-frank'],
    'mock-user-david': ['mock-user-anna', 'mock-user-frank'],
    'mock-user-eva': ['mock-user-anna'],
    'mock-user-frank': ['mock-user-ben', 'mock-user-david'],
  };
  for (const [uid, friends] of Object.entries(friendships)) {
    writes.push({
      ref: db.collection('users').doc(uid),
      data: { friends },
      merge: true,
    });
  }

  // A couple of pending follow_requests targeting private profiles (Clara, Eva).
  writes.push({
    ref: db.collection('follow_requests').doc('mock-user-anna_mock-user-clara'),
    data: {
      fromUserId: 'mock-user-anna',
      fromUsername: 'AnnaExplorer',
      fromUserProfileImageUrl: MOCK_USERS[0].profileImageUrl,
      toUserId: 'mock-user-clara',
      requestedAt: daysFromNow(-1),
    },
  });
  writes.push({
    ref: db.collection('follow_requests').doc('mock-user-frank_mock-user-eva'),
    data: {
      fromUserId: 'mock-user-frank',
      fromUsername: 'FrankHikes',
      fromUserProfileImageUrl: MOCK_USERS[5].profileImageUrl,
      toUserId: 'mock-user-eva',
      requestedAt: daysFromNow(0),
    },
  });

  await commitWrites(db, writes);
  console.log(`   wrote ${MOCK_USERS.length} users + statistics + friend graph`);
}

async function seedDailyChallenges(db) {
  console.log('📅 Seeding daily_challenges...');
  const writes = MOCK_DAILY_CHALLENGES.map((c) => ({
    ref: db.collection('daily_challenges').doc(c.id),
    data: {
      imageUrl: c.imageUrl,
      location: c.location,
      radiusMeters: c.radiusMeters,
      date: c.date,
      is360View: c.is360View,
      cityName: c.cityName,
    },
  }));

  // For variety, also pre-populate a couple of completions by mock users for
  // past challenges. These flow into the social feed via `seedFeed`.
  const completionPlans = [
    {
      challengeId: 'daily-2024-brno-spilberk',
      userId: 'mock-user-ben',
      username: 'BenWanderer',
      photoUrl:
        'https://images.unsplash.com/photo-1597211684565-dca64d72bdfe?w=600',
      caption: 'Made it to Špilberk just in time!',
      daysAgo: -6,
    },
    {
      challengeId: 'daily-2024-prague-charles-bridge',
      userId: 'mock-user-anna',
      username: 'AnnaExplorer',
      photoUrl:
        'https://images.unsplash.com/photo-1541849546-216549ae216d?w=600',
      caption: 'Charles Bridge at sunset 🌅',
      daysAgo: -5,
    },
    {
      challengeId: 'daily-2024-brno-petrov',
      userId: 'mock-user-frank',
      username: 'FrankHikes',
      photoUrl:
        'https://images.unsplash.com/photo-1574871787809-d99b8e9a1f9f?w=600',
      caption: 'Petrov is so photogenic.',
      daysAgo: -4,
    },
  ];
  for (const p of completionPlans) {
    const author = MOCK_USERS.find((u) => u.id === p.userId);
    writes.push({
      ref: db
        .collection('users')
        .doc(p.userId)
        .collection('daily_completions')
        .doc(p.challengeId),
      data: {
        userId: p.userId,
        username: p.username,
        userProfileImageUrl: author?.profileImageUrl ?? null,
        challengeId: p.challengeId,
        imageId: `${p.challengeId}-${p.userId}`,
        photoUrl: p.photoUrl,
        caption: p.caption,
        location: MOCK_DAILY_CHALLENGES.find((c) => c.id === p.challengeId)
          ?.location,
        is360View: false,
        completedAt: daysFromNow(p.daysAgo),
        sharedToFeed: true,
      },
    });
  }

  await commitWrites(db, writes);
  console.log(
    `   wrote ${MOCK_DAILY_CHALLENGES.length} challenges + ${completionPlans.length} mock completions`,
  );
}

async function seedRoutes(db) {
  console.log('🗺️  Seeding routes + reviews...');
  const writes = [];
  for (const r of MOCK_ROUTES) {
    writes.push({
      ref: db.collection('routes').doc(r.id),
      data: {
        name: r.name,
        description: r.description,
        authorId: r.authorId,
        city: r.city,
        points: r.points,
        averageRating: r.averageRating,
        reviewCount: r.reviewCount,
        tags: r.tags,
        imageUrl: r.imageUrl,
        estimatedDurationMinutes: r.estimatedDurationMinutes,
        distanceMeters: r.distanceMeters,
      },
    });

    const reviews = MOCK_ROUTE_REVIEWS[r.id] ?? [];
    for (const rev of reviews) {
      writes.push({
        ref: db.collection('routes').doc(r.id).collection('reviews').doc(),
        data: rev,
      });
    }
  }
  await commitWrites(db, writes);
  const reviewCount = Object.values(MOCK_ROUTE_REVIEWS).flat().length;
  console.log(`   wrote ${MOCK_ROUTES.length} routes + ${reviewCount} reviews`);
}

async function seedBingoCards(db) {
  console.log('🎯 Seeding bingo_cards...');
  const writes = MOCK_BINGO_CARDS.map((card) => ({
    ref: db.collection('bingo_cards').doc(card.id),
    data: {
      weekStartDate: card.weekStartDate,
      tasks: card.tasks,
    },
  }));

  // Each mock user has partial progress on the current card so the leaderboard /
  // social feed surfaces real bingo activity.
  for (let i = 0; i < MOCK_USERS.length; i++) {
    const u = MOCK_USERS[i];
    const card = MOCK_BINGO_CARDS[0];
    const completedCount = (i + 1) * 2; // 2, 4, 6, ... up to 12
    const completedTaskIds = card.tasks
      .slice(0, completedCount)
      .map((t) => t.id);

    writes.push({
      ref: db
        .collection('users')
        .doc(u.id)
        .collection('bingo_progress')
        .doc(card.id),
      data: {
        userId: u.id,
        bingoCardId: card.id,
        completedTaskIds,
        sharedToFeed: i % 2 === 0,
      },
    });

    // First two completed tasks become real BingoTaskCompletion docs (for feed).
    for (let j = 0; j < Math.min(2, completedTaskIds.length); j++) {
      const task = card.tasks[j];
      writes.push({
        ref: db.collection('bingo_task_completions').doc(),
        data: {
          userId: u.id,
          username: u.username,
          userProfileImageUrl: u.profileImageUrl,
          bingoCardId: card.id,
          taskId: task.id,
          taskTitle: task.title,
          taskType: task.type,
          photoUrl:
            task.type === 'PHOTO'
              ? 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=600'
              : null,
          caption: `Knocked out "${task.title}".`,
          location: null,
          completedAt: daysFromNow(-((j + 1) * 1)),
        },
      });
    }
  }

  await commitWrites(db, writes);
  console.log(`   wrote ${MOCK_BINGO_CARDS.length} cards + per-user progress`);
}

async function seedFeed(db) {
  console.log('📰 Seeding feed entries...');
  // A mix of feed entry types so the social screen has visual variety.
  const writes = [
    {
      ref: db.collection('feed').doc(),
      data: {
        userId: 'mock-user-anna',
        username: 'AnnaExplorer',
        userProfileImageUrl: MOCK_USERS[0].profileImageUrl,
        type: 'DAILY_CHALLENGE',
        imageId: 'daily-2024-prague-charles-bridge-mock-user-anna',
        imageUrl:
          'https://images.unsplash.com/photo-1541849546-216549ae216d?w=600',
        imageUrls: [],
        location: geo(50.0865, 14.4114),
        timestamp: daysFromNow(-5),
        challengeId: 'daily-2024-prague-charles-bridge',
        routeId: null,
        bingoCardId: null,
        description: 'Charles Bridge at sunset 🌅',
      },
    },
    {
      ref: db.collection('feed').doc(),
      data: {
        userId: 'mock-user-ben',
        username: 'BenWanderer',
        userProfileImageUrl: MOCK_USERS[1].profileImageUrl,
        type: 'DAILY_CHALLENGE',
        imageId: 'daily-2024-brno-spilberk-mock-user-ben',
        imageUrl:
          'https://images.unsplash.com/photo-1597211684565-dca64d72bdfe?w=600',
        imageUrls: [],
        location: geo(49.1942, 16.5994),
        timestamp: daysFromNow(-6),
        challengeId: 'daily-2024-brno-spilberk',
        routeId: null,
        bingoCardId: null,
        description: 'Made it to Špilberk just in time!',
      },
    },
    {
      ref: db.collection('feed').doc(),
      data: {
        userId: 'mock-user-frank',
        username: 'FrankHikes',
        userProfileImageUrl: MOCK_USERS[5].profileImageUrl,
        type: 'BINGO_TASK_COMPLETED',
        imageId: '',
        imageUrl:
          'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=600',
        imageUrls: [],
        location: null,
        timestamp: daysFromNow(-1),
        challengeId: null,
        routeId: null,
        bingoCardId: CURRENT_CARD_ID,
        description: 'Knocked out "Photo of sunrise".',
      },
    },
    {
      ref: db.collection('feed').doc(),
      data: {
        userId: 'mock-user-david',
        username: 'DavidRoams',
        userProfileImageUrl: MOCK_USERS[3].profileImageUrl,
        type: 'ROUTE_COMPLETED',
        imageId: '',
        imageUrl:
          'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=600',
        imageUrls: [],
        location: geo(49.5938, 17.2509),
        timestamp: daysFromNow(-2),
        challengeId: null,
        routeId: 'route-olomouc-old-town',
        bingoCardId: null,
        description: 'Finished the Olomouc Old Town walk — loved it.',
      },
    },
    {
      ref: db.collection('feed').doc(),
      data: {
        userId: 'mock-user-anna',
        username: 'AnnaExplorer',
        userProfileImageUrl: MOCK_USERS[0].profileImageUrl,
        type: 'BINGO_COMPLETED',
        imageId: '',
        imageUrl:
          'https://images.unsplash.com/photo-1519677100203-a0e668c92439?w=600',
        imageUrls: [],
        location: null,
        timestamp: daysFromNow(-3),
        challengeId: null,
        routeId: null,
        bingoCardId: 'bingo-last-week',
        description: 'Filled the whole card last week! 🎉',
      },
    },
  ];
  await commitWrites(db, writes);
  console.log(`   wrote ${writes.length} feed entries`);
}

// ---------------------------------------------------------------------------
// Current-user anchoring
// ---------------------------------------------------------------------------

/**
 * Wire a real user (typically the one signed in on the device) into the seeded
 * data so they immediately see populated screens:
 *  - friend list seeded with every mock user → social feed lights up
 *  - completed past daily challenges (with hints unlocked on one) → history view
 *  - partial bingo progress + a couple of completions
 *  - one in-progress + one completed route
 *  - user_statistics with realistic streak + xp
 */
async function anchorCurrentUser(db, uid) {
  console.log(`🪡 Anchoring data to current user: ${uid}`);
  // We don't overwrite username/email because that user already exists in
  // Firestore (created by the app on signup). We merge only the friends list.
  const writes = [];

  // -- friends: follow every mock user -------------------------------------
  writes.push({
    ref: db.collection('users').doc(uid),
    data: { friends: MOCK_USERS.map((u) => u.id) },
    merge: true,
  });

  // -- statistics ----------------------------------------------------------
  writes.push({
    ref: db.collection('user_statistics').doc(uid),
    data: {
      userId: uid,
      streak: 7,
      xp: 1850,
      lastStreakUpdate: daysFromNow(0),
    },
    merge: true,
  });

  // -- daily completions for two past challenges ---------------------------
  const completionTargets = [
    {
      challengeId: 'daily-2024-prague-old-town',
      caption: 'Explored the old town today.',
      daysAgo: -1,
      hints: { directionUnlocked: true, mapUnlocked: false },
    },
    {
      challengeId: 'daily-2024-brno-cabbage-market',
      caption: 'Cabbage Market in the morning light.',
      daysAgo: -2,
      hints: { directionUnlocked: false, mapUnlocked: false },
    },
  ];

  for (const target of completionTargets) {
    const challenge = MOCK_DAILY_CHALLENGES.find(
      (c) => c.id === target.challengeId,
    );
    writes.push({
      ref: db
        .collection('users')
        .doc(uid)
        .collection('daily_completions')
        .doc(target.challengeId),
      data: {
        userId: uid,
        username: 'You',
        userProfileImageUrl: null,
        challengeId: target.challengeId,
        imageId: `${target.challengeId}-${uid}`,
        photoUrl: challenge?.imageUrl ?? '',
        caption: target.caption,
        location: challenge?.location ?? null,
        is360View: false,
        completedAt: daysFromNow(target.daysAgo),
        sharedToFeed: false,
      },
    });

    // Hints document — only written when at least one is unlocked.
    if (target.hints.directionUnlocked || target.hints.mapUnlocked) {
      writes.push({
        ref: db
          .collection('users')
          .doc(uid)
          .collection('daily_hints')
          .doc(target.challengeId),
        data: target.hints,
      });
    }
  }

  // -- bingo progress + a couple of task completions -----------------------
  const card = MOCK_BINGO_CARDS[0];
  const myCompletedIds = card.tasks.slice(0, 5).map((t) => t.id);
  writes.push({
    ref: db
      .collection('users')
      .doc(uid)
      .collection('bingo_progress')
      .doc(card.id),
    data: {
      userId: uid,
      bingoCardId: card.id,
      completedTaskIds: myCompletedIds,
      sharedToFeed: false,
    },
  });
  for (let j = 0; j < 2; j++) {
    const task = card.tasks[j];
    writes.push({
      ref: db.collection('bingo_task_completions').doc(),
      data: {
        userId: uid,
        username: 'You',
        userProfileImageUrl: null,
        bingoCardId: card.id,
        taskId: task.id,
        taskTitle: task.title,
        taskType: task.type,
        photoUrl:
          task.type === 'PHOTO'
            ? 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=600'
            : null,
        caption: '',
        location: null,
        completedAt: daysFromNow(-(j + 1)),
      },
    });
  }

  // -- route progress: one in-progress, one completed ----------------------
  const inProgressRoute = MOCK_ROUTES[0]; // Historic Brno Walk
  writes.push({
    ref: db
      .collection('users')
      .doc(uid)
      .collection('route_progress')
      .doc(inProgressRoute.id),
    data: {
      userId: uid,
      routeId: inProgressRoute.id,
      completedPointIds: ['0'],
      lastAnswers: { '0': inProgressRoute.points[0].quizAnswer },
      isCompleted: false,
      startedAt: daysFromNow(-1),
      completedAt: null,
    },
  });

  const completedRoute = MOCK_ROUTES[4]; // Olomouc Old Town
  writes.push({
    ref: db
      .collection('users')
      .doc(uid)
      .collection('route_progress')
      .doc(completedRoute.id),
    data: {
      userId: uid,
      routeId: completedRoute.id,
      completedPointIds: completedRoute.points.map((_, i) => String(i)),
      lastAnswers: Object.fromEntries(
        completedRoute.points.map((p, i) => [String(i), p.quizAnswer]),
      ),
      isCompleted: true,
      startedAt: daysFromNow(-3),
      completedAt: daysFromNow(-2),
    },
  });
  writes.push({
    ref: db
      .collection('users')
      .doc(uid)
      .collection('route_completions')
      .doc(completedRoute.id),
    data: {
      userId: uid,
      username: 'You',
      userProfileImageUrl: null,
      routeId: completedRoute.id,
      routeName: completedRoute.name,
      city: completedRoute.city,
      totalPoints: completedRoute.points.length,
      photoUrl: completedRoute.imageUrl,
      caption: 'Wrapped up the Olomouc walk!',
      startedAt: daysFromNow(-3),
      completedAt: daysFromNow(-2),
      sharedToFeed: false,
    },
  });

  await commitWrites(db, writes);
  console.log(`   wired friends + completions + progress for ${uid}`);
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const db = initFirestore();

  console.log('');
  console.log('🌱 BeeLocal seed starting');
  console.log(`   wipe:              ${args.wipe}`);
  console.log(`   currentUser:       ${args.currentUser ?? '(none)'}`);
  console.log(`   onlyCurrentUser:   ${args.onlyCurrentUser}`);
  console.log('');

  if (args.wipe) {
    await wipe(db);
  }

  if (!args.onlyCurrentUser) {
    await seedUsers(db);
    await seedDailyChallenges(db);
    await seedRoutes(db);
    await seedBingoCards(db);
    await seedFeed(db);
  } else {
    console.log('⏭️  Skipping global mock content (--only-current-user)');
  }

  if (args.currentUser) {
    await anchorCurrentUser(db, args.currentUser);
  }

  console.log('');
  console.log('✅ Seed complete.');
  process.exit(0);
}

main().catch((err) => {
  console.error('');
  console.error('❌ Seed failed:');
  console.error(err);
  process.exit(1);
});
