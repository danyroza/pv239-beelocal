import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import admin from 'firebase-admin';

const __dirname = dirname(fileURLToPath(import.meta.url));

/**
 * Initialise firebase-admin against the real Firestore project. The script
 * authenticates with a service-account JSON that is intentionally **not** in
 * version control — drop it next to seed.mjs as `serviceAccountKey.json`.
 *
 * See README.md for how to generate it from the Firebase Console.
 */
export function initFirestore() {
  if (admin.apps.length) return admin.firestore();

  const keyPath = resolve(__dirname, '..', 'serviceAccountKey.json');
  if (!existsSync(keyPath)) {
    console.error(
      `\n❌ Missing service account key at:\n   ${keyPath}\n\n` +
        `   Generate one in the Firebase Console:\n` +
        `   Project Settings → Service accounts → "Generate new private key"\n` +
        `   Save the downloaded file as scripts/seed/serviceAccountKey.json\n`,
    );
    process.exit(1);
  }

  const credentials = JSON.parse(readFileSync(keyPath, 'utf-8'));
  admin.initializeApp({
    credential: admin.credential.cert(credentials),
    projectId: credentials.project_id,
  });

  const db = admin.firestore();
  db.settings({ ignoreUndefinedProperties: true });
  console.log(`🔥 Connected to Firestore project: ${credentials.project_id}`);
  return db;
}

export { admin };
