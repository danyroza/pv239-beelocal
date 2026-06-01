import { admin } from './firestore.mjs';

/** Firestore Timestamp from a JS Date. */
export const ts = (date) => admin.firestore.Timestamp.fromDate(date);

/** Timestamp `n` days from now (negative = in the past). */
export const daysFromNow = (n) => {
  const d = new Date();
  d.setHours(12, 0, 0, 0);
  d.setDate(d.getDate() + n);
  return ts(d);
};

/** Today at 00:00:00 local time. */
export const startOfToday = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return ts(d);
};

/** Monday of the current week at 00:00:00 local time. */
export const startOfThisWeek = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  // 0 = Sunday, 1 = Monday, ...
  const day = d.getDay();
  const offset = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + offset);
  return ts(d);
};

/** Monday of last week at 00:00:00 local time. */
export const startOfLastWeek = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  const day = d.getDay();
  const offset = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + offset - 7);
  return ts(d);
};

/** Firestore GeoPoint. */
export const geo = (lat, lng) => new admin.firestore.GeoPoint(lat, lng);

/** Pick a deterministic-ish element from an array using an index. */
export const pick = (arr, i) => arr[i % arr.length];

/**
 * Recursively delete an entire collection (and all of its subcollections)
 * 500 docs at a time. Used by the --wipe flag so the seeder can guarantee a
 * clean slate.
 */
export async function recursiveDeleteCollection(db, collectionPath) {
  // Admin SDK ships a server-side recursive delete; it's the fastest option
  // and correctly handles subcollections.
  const ref = db.collection(collectionPath);
  await db.recursiveDelete(ref);
}

/**
 * Commit an array of write operations using batched writes (max 500 per batch).
 *
 * Each entry should be `{ ref, data, merge? }`. We pre-chunk to stay within
 * Firestore's batch-size limit and to keep memory usage bounded for large seeds.
 */
export async function commitWrites(db, writes) {
  const CHUNK = 450;
  for (let i = 0; i < writes.length; i += CHUNK) {
    const batch = db.batch();
    for (const w of writes.slice(i, i + CHUNK)) {
      if (w.merge) batch.set(w.ref, w.data, { merge: true });
      else batch.set(w.ref, w.data);
    }
    await batch.commit();
  }
}
