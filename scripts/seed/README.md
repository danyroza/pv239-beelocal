# BeeLocal Firestore Seeder

A small Node.js script that populates the BeeLocal Firestore project with
realistic mock data — users, daily challenges, routes (+ reviews), bingo cards,
follow requests, and a social feed — so the app can be tested end-to-end.

It targets the **real Firestore project** declared in `app/google-services.json`
(`pv239-beelocal`). No emulator is used.

---

## 1. One-time setup

### 1.1 Get a service account key

Seeding uses the **firebase-admin SDK**, which authenticates with a private
service-account JSON instead of a user login. The key bypasses Firestore
security rules, so keep it out of git.

1. Open the Firebase Console for the **pv239-beelocal** project.
2. Go to ⚙️ **Project settings → Service accounts**.
3. Click **Generate new private key**, confirm, and download the JSON.
4. Save the file as `scripts/seed/serviceAccountKey.json` (filename matters).

`scripts/seed/.gitignore` already excludes this file from version control.

### 1.2 Install dependencies

```bash
cd scripts/seed
npm install
```

This installs `firebase-admin` and `@faker-js/faker`.

---

## 2. Running the seeder

All commands are run from `scripts/seed/`.

### Additive seed (safe default)

Adds mock users, challenges, routes, bingo cards, feed entries.
Re-running is idempotent because IDs are stable (e.g. `mock-user-anna`).

```bash
node seed.mjs
# or
npm run seed
```

### Wipe + re-seed

Recursively deletes every seeded collection first, then re-creates them.
Useful when schema or mock data changes.

```bash
node seed.mjs --wipe
# or
npm run seed:wipe
```

### Anchor data to your real account

Pass the Firebase UID of the user you're signed in as on the phone, and the
seeder will:

- add every mock user to your `friends` list (so your feed lights up),
- write `user_statistics` (streak + xp),
- create two past `daily_completions` (one with a paid hint unlocked),
- create partial `bingo_progress` + a couple of `bingo_task_completions`,
- create one in-progress and one completed route in `route_progress` /
  `route_completions`.

```bash
node seed.mjs --current-user <yourFirebaseUid>
```

Find your UID under **Authentication → Users** in the Firebase Console after
signing in once with the app.

### Seed *only* your user's data

Skip creating mock users/challenges/routes/bingo cards and only wire the
current-user content described above. Use this when the global mock data
already exists from a previous run.

```bash
node seed.mjs --current-user <yourFirebaseUid> --only-current-user
```

### Common workflow

Full reset + everything wired to your account in one go:

```bash
node seed.mjs --wipe --current-user <yourFirebaseUid>
```

---

## 3. What gets written

Top-level collections (mirrors `FirestoreCollections.kt`):

| Collection                  | Seeded contents                                                     |
| --------------------------- | -------------------------------------------------------------------- |
| `users`                     | 6 mock users (mix of public/private profiles) + friend graph         |
| `user_statistics`           | streak + xp per mock user (+ current user if flag set)               |
| `daily_challenges`          | 6 past + 1 today's challenge across Brno/Prague                      |
| `feed`                      | 5 entries spanning all `FeedEntryType` values                        |
| `routes`                    | 5 routes across Brno/Prague/Olomouc, each with `reviews` subcoll.    |
| `bingo_cards`               | Current + previous week, 16 tasks each                               |
| `bingo_task_completions`    | A couple per mock user + current user                                |
| `follow_requests`           | 2 pending requests targeting private profiles                        |

Per-user subcollections (`users/{uid}/...`):

| Subcollection         | Seeded contents                                          |
| --------------------- | -------------------------------------------------------- |
| `daily_completions`   | 2–3 past completions                                     |
| `daily_hints`         | 1 hint unlocked on one challenge (current user only)     |
| `bingo_progress`      | Partial completion of current week's card                |
| `route_progress`      | 1 in-progress + 1 completed route                        |
| `route_completions`   | 1 completed route                                        |

All image URLs point to public Unsplash CDN images.

---

## 4. Adding more mock data

Edit the data files in `scripts/seed/data/`:

```
data/
  users.mjs              # Mock users + their statistics
  dailyChallenges.mjs    # Daily challenges
  routes.mjs             # Routes + reviews
  bingo.mjs              # Bingo cards + tasks
```

Cross-references are by string ID (e.g. a feed entry's `challengeId` matches a
daily challenge's `id`), so keep IDs consistent when adding entries.

---

## 5. Troubleshooting

**`Missing service account key`** — you skipped step 1.1.

**`PERMISSION_DENIED`** — the service account file is for a different Firebase
project, or the account is missing the *Firebase Admin* / *Cloud Datastore
User* role. Re-download via the Firebase Console.

**Data wasn't deleted on `--wipe`** — `recursiveDelete` only touches the
collections listed in `SEEDED_COLLECTIONS` inside `seed.mjs`. If you added a
new top-level collection, add it to that list.
