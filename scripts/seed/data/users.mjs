/**
 * Hand-curated mock users. IDs are stable so re-runs are idempotent and other
 * data files can reference them by literal string.
 *
 * Profile images use Unsplash random source — fine for first iteration.
 */
export const MOCK_USERS = [
  {
    id: 'mock-user-anna',
    username: 'AnnaExplorer',
    email: 'anna@example.com',
    profileImageUrl:
      'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300',
    isProfilePublic: true,
  },
  {
    id: 'mock-user-ben',
    username: 'BenWanderer',
    email: 'ben@example.com',
    profileImageUrl:
      'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300',
    isProfilePublic: true,
  },
  {
    id: 'mock-user-clara',
    username: 'ClaraTrail',
    email: 'clara@example.com',
    profileImageUrl:
      'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=300',
    isProfilePublic: false,
  },
  {
    id: 'mock-user-david',
    username: 'DavidRoams',
    email: 'david@example.com',
    profileImageUrl:
      'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300',
    isProfilePublic: true,
  },
  {
    id: 'mock-user-eva',
    username: 'EvaPaths',
    email: 'eva@example.com',
    profileImageUrl:
      'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300',
    isProfilePublic: false,
  },
  {
    id: 'mock-user-frank',
    username: 'FrankHikes',
    email: 'frank@example.com',
    profileImageUrl:
      'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300',
    isProfilePublic: true,
  },
];

/** Map of userId -> denormalised stats for quick lookup from other data files. */
export const MOCK_USER_STATS = {
  'mock-user-anna': { streak: 12, xp: 2150 },
  'mock-user-ben': { streak: 5, xp: 1340 },
  'mock-user-clara': { streak: 28, xp: 4870 },
  'mock-user-david': { streak: 3, xp: 720 },
  'mock-user-eva': { streak: 17, xp: 3260 },
  'mock-user-frank': { streak: 1, xp: 280 },
};
