import { geo } from '../lib/helpers.mjs';

/**
 * 5 routes spread across Brno + Prague + Olomouc so the city-filtered routes
 * list surfaces multiple cities in `getRoutesByCity`.
 *
 * Each point carries a quiz question/answer so the in-app checkpoint flow is
 * end-to-end testable. Ratings + review counts are pre-baked; per-route
 * reviews (subcollection) are seeded in the main script.
 */
export const MOCK_ROUTES = [
  {
    id: 'route-brno-historic-walk',
    name: 'Historic Brno Walk',
    description:
      'A relaxed loop through Brno’s old town, from Špilberk down to the cabbage market.',
    authorId: 'mock-user-anna',
    city: 'Brno',
    averageRating: 4.6,
    reviewCount: 3,
    tags: ['history', 'walking', 'easy'],
    imageUrl:
      'https://images.unsplash.com/photo-1597211684565-dca64d72bdfe?w=800',
    estimatedDurationMinutes: 90,
    distanceMeters: 4200,
    points: [
      {
        name: 'Špilberk Castle',
        description: 'Brno’s iconic hilltop fortress.',
        location: geo(49.1942, 16.5994),
        quizQuestion: 'What century was Špilberk Castle founded in?',
        quizAnswer: '13th',
      },
      {
        name: 'Petrov Cathedral',
        description: 'Cathedral of St. Peter and Paul.',
        location: geo(49.1909, 16.6076),
        quizQuestion: 'How many spires does Petrov have?',
        quizAnswer: '2',
      },
      {
        name: 'Cabbage Market',
        description: 'Lively baroque square with Parnas Fountain.',
        location: geo(49.1929, 16.6086),
        quizQuestion: 'What fountain stands on Cabbage Market?',
        quizAnswer: 'Parnas',
      },
    ],
  },
  {
    id: 'route-brno-modern-architecture',
    name: 'Functionalist Brno',
    description:
      'Tour Brno’s UNESCO-listed Villa Tugendhat plus a handful of interwar gems.',
    authorId: 'mock-user-clara',
    city: 'Brno',
    averageRating: 4.3,
    reviewCount: 2,
    tags: ['architecture', 'modernism'],
    imageUrl:
      'https://images.unsplash.com/photo-1503416997304-7f8bf166c121?w=800',
    estimatedDurationMinutes: 120,
    distanceMeters: 5600,
    points: [
      {
        name: 'Villa Tugendhat',
        description: 'Mies van der Rohe’s functionalist masterpiece.',
        location: geo(49.2074, 16.6157),
        quizQuestion: 'Who designed Villa Tugendhat?',
        quizAnswer: 'Mies van der Rohe',
      },
      {
        name: 'Avion Hotel',
        description: 'Narrowest interwar hotel in Europe.',
        location: geo(49.1957, 16.6075),
        quizQuestion: 'In what year was the Avion Hotel completed?',
        quizAnswer: '1928',
      },
    ],
  },
  {
    id: 'route-prague-castle-loop',
    name: 'Prague Castle Loop',
    description:
      'A classic stroll across the Charles Bridge, up to the castle and back.',
    authorId: 'mock-user-ben',
    city: 'Prague',
    averageRating: 4.8,
    reviewCount: 4,
    tags: ['history', 'iconic'],
    imageUrl:
      'https://images.unsplash.com/photo-1541849546-216549ae216d?w=800',
    estimatedDurationMinutes: 150,
    distanceMeters: 6100,
    points: [
      {
        name: 'Charles Bridge',
        description: 'Iconic Gothic stone bridge over the Vltava.',
        location: geo(50.0865, 14.4114),
        quizQuestion: 'How many statues line Charles Bridge?',
        quizAnswer: '30',
      },
      {
        name: 'St. Vitus Cathedral',
        description: 'Largest church in the Czech Republic.',
        location: geo(50.0907, 14.4001),
        quizQuestion: 'What style is St. Vitus Cathedral?',
        quizAnswer: 'Gothic',
      },
      {
        name: 'Astronomical Clock',
        description: 'Medieval clock on Old Town Hall.',
        location: geo(50.087, 14.4208),
        quizQuestion: 'In what year was the Astronomical Clock installed?',
        quizAnswer: '1410',
      },
    ],
  },
  {
    id: 'route-prague-letna-views',
    name: 'Letná Park Sunset',
    description: 'A short uphill walk for the best skyline view in Prague.',
    authorId: 'mock-user-eva',
    city: 'Prague',
    averageRating: 4.4,
    reviewCount: 1,
    tags: ['nature', 'sunset', 'easy'],
    imageUrl:
      'https://images.unsplash.com/photo-1519677100203-a0e668c92439?w=800',
    estimatedDurationMinutes: 60,
    distanceMeters: 2400,
    points: [
      {
        name: 'Letná Beer Garden',
        description: 'Sprawling beer garden with skyline views.',
        location: geo(50.0958, 14.4231),
        quizQuestion: 'What drink is Letná Beer Garden famous for?',
        quizAnswer: 'Pilsner',
      },
      {
        name: 'Metronome',
        description: 'Giant metronome where Stalin’s monument once stood.',
        location: geo(50.0938, 14.4189),
        quizQuestion: 'What sculpture stood here before the Metronome?',
        quizAnswer: 'Stalin',
      },
    ],
  },
  {
    id: 'route-olomouc-old-town',
    name: 'Olomouc Old Town',
    description:
      'Compact walk through Olomouc’s UNESCO-listed Holy Trinity Column area.',
    authorId: 'mock-user-david',
    city: 'Olomouc',
    averageRating: 4.1,
    reviewCount: 2,
    tags: ['history', 'short'],
    imageUrl:
      'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800',
    estimatedDurationMinutes: 75,
    distanceMeters: 3200,
    points: [
      {
        name: 'Holy Trinity Column',
        description: 'UNESCO baroque column on Upper Square.',
        location: geo(49.5938, 17.2509),
        quizQuestion: 'In what year was the Holy Trinity Column completed?',
        quizAnswer: '1754',
      },
      {
        name: 'Olomouc Astronomical Clock',
        description: 'Socialist-realist reinterpretation of a medieval clock.',
        location: geo(49.5946, 17.2509),
        quizQuestion: 'What style was the rebuilt Olomouc clock?',
        quizAnswer: 'Socialist realism',
      },
    ],
  },
];

/**
 * Reviews are stored in the `reviews` subcollection under each route. We
 * generate a small handcrafted set so the rating distribution is plausible
 * and the math (`averageRating`) lines up with `reviewCount` above.
 */
export const MOCK_ROUTE_REVIEWS = {
  'route-brno-historic-walk': [
    {
      userId: 'mock-user-ben',
      username: 'BenWanderer',
      rating: 5,
      comment: 'Loved every minute — pace was perfect.',
      photoUrls: [],
    },
    {
      userId: 'mock-user-david',
      username: 'DavidRoams',
      rating: 4,
      comment: 'Great loop, a bit short though.',
      photoUrls: [],
    },
    {
      userId: 'mock-user-frank',
      username: 'FrankHikes',
      rating: 5,
      comment: 'Excellent quiz questions, very Brno.',
      photoUrls: [],
    },
  ],
  'route-brno-modern-architecture': [
    {
      userId: 'mock-user-anna',
      username: 'AnnaExplorer',
      rating: 4,
      comment: 'Tugendhat alone makes this worth it.',
      photoUrls: [],
    },
    {
      userId: 'mock-user-eva',
      username: 'EvaPaths',
      rating: 5,
      comment: 'Niche but brilliantly curated.',
      photoUrls: [],
    },
  ],
  'route-prague-castle-loop': [
    {
      userId: 'mock-user-anna',
      username: 'AnnaExplorer',
      rating: 5,
      comment: 'The classic Prague experience.',
      photoUrls: [],
    },
    {
      userId: 'mock-user-clara',
      username: 'ClaraTrail',
      rating: 5,
      comment: 'Bring water and start early!',
      photoUrls: [],
    },
    {
      userId: 'mock-user-david',
      username: 'DavidRoams',
      rating: 4,
      comment: 'Crowded but unforgettable.',
      photoUrls: [],
    },
    {
      userId: 'mock-user-frank',
      username: 'FrankHikes',
      rating: 5,
      comment: 'Iconic from start to finish.',
      photoUrls: [],
    },
  ],
  'route-prague-letna-views': [
    {
      userId: 'mock-user-ben',
      username: 'BenWanderer',
      rating: 4,
      comment: 'Best at golden hour.',
      photoUrls: [],
    },
  ],
  'route-olomouc-old-town': [
    {
      userId: 'mock-user-ben',
      username: 'BenWanderer',
      rating: 4,
      comment: 'Underrated little city.',
      photoUrls: [],
    },
    {
      userId: 'mock-user-anna',
      username: 'AnnaExplorer',
      rating: 4,
      comment: 'Loved the column up close.',
      photoUrls: [],
    },
  ],
};
