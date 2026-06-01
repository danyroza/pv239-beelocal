import { daysFromNow, geo } from '../lib/helpers.mjs';

/**
 * 7 daily challenges — 6 in the past, one **for today**. The "today" entry is
 * what the app will surface in `getDailyChallenge(Timestamp.now())`.
 *
 * Locations alternate between Brno and Prague landmarks so the routes screen
 * has variety even before any custom city filter is applied.
 */
export const MOCK_DAILY_CHALLENGES = [
  {
    id: 'daily-2024-brno-spilberk',
    imageUrl:
      'https://images.unsplash.com/photo-1597211684565-dca64d72bdfe?w=800',
    location: geo(49.1942, 16.5994),
    radiusMeters: 500,
    date: daysFromNow(-6),
    is360View: false,
    cityName: 'Brno',
  },
  {
    id: 'daily-2024-prague-charles-bridge',
    imageUrl:
      'https://images.unsplash.com/photo-1541849546-216549ae216d?w=800',
    location: geo(50.0865, 14.4114),
    radiusMeters: 400,
    date: daysFromNow(-5),
    is360View: true,
    cityName: 'Prague',
  },
  {
    id: 'daily-2024-brno-petrov',
    imageUrl:
      'https://images.unsplash.com/photo-1574871787809-d99b8e9a1f9f?w=800',
    location: geo(49.1909, 16.6076),
    radiusMeters: 350,
    date: daysFromNow(-4),
    is360View: false,
    cityName: 'Brno',
  },
  {
    id: 'daily-2024-prague-astronomical-clock',
    imageUrl:
      'https://images.unsplash.com/photo-1519677100203-a0e668c92439?w=800',
    location: geo(50.0870, 14.4208),
    radiusMeters: 300,
    date: daysFromNow(-3),
    is360View: false,
    cityName: 'Prague',
  },
  {
    id: 'daily-2024-brno-cabbage-market',
    imageUrl:
      'https://images.unsplash.com/photo-1582489091537-78f87ad6f5f2?w=800',
    location: geo(49.1929, 16.6086),
    radiusMeters: 350,
    date: daysFromNow(-2),
    is360View: false,
    cityName: 'Brno',
  },
  {
    id: 'daily-2024-prague-old-town',
    imageUrl:
      'https://images.unsplash.com/photo-1573599852326-2d4da0bbe613?w=800',
    location: geo(50.0875, 14.4213),
    radiusMeters: 500,
    date: daysFromNow(-1),
    is360View: true,
    cityName: 'Prague',
  },
  {
    id: 'daily-today',
    imageUrl:
      'https://images.unsplash.com/photo-1519677100203-a0e668c92439?w=800',
    location: geo(49.1951, 16.6068), // Brno main square
    radiusMeters: 500,
    date: daysFromNow(0),
    is360View: false,
    cityName: 'Brno',
  },
];
