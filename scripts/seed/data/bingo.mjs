import { startOfThisWeek, startOfLastWeek } from '../lib/helpers.mjs';

const buildTasks = (cardId, items) =>
  items.map((it, i) => ({
    id: `${cardId}-task-${i + 1}`,
    title: it.title,
    description: it.description,
    type: it.type, // "PHOTO" | "LOCATION"
  }));

const CURRENT_CARD_ID = 'bingo-current-week';
const PAST_CARD_ID = 'bingo-last-week';

/**
 * Two bingo cards — current and previous week — each with the required 16
 * tasks so the 4x4 board renders fully and the [BingoTaskType] mix is
 * exercised. The "current" card is what `getCurrentBingoCard()` will return
 * since it has the most recent weekStartDate.
 */
export const MOCK_BINGO_CARDS = [
  {
    id: CURRENT_CARD_ID,
    weekStartDate: startOfThisWeek(),
    tasks: buildTasks(CURRENT_CARD_ID, [
      { title: 'Photo of sunrise', description: 'Snap a sunrise from any vantage point.', type: 'PHOTO' },
      { title: 'Visit a museum', description: 'Check in inside any museum.', type: 'LOCATION' },
      { title: 'Local coffee', description: 'Photo of a coffee at a local café.', type: 'PHOTO' },
      { title: 'Old bookstore', description: 'Find and visit a second-hand bookstore.', type: 'LOCATION' },
      { title: 'Street art', description: 'Photograph a piece of street art.', type: 'PHOTO' },
      { title: 'Public park', description: 'Visit any public park.', type: 'LOCATION' },
      { title: 'Tram ride', description: 'Capture a photo from inside a tram.', type: 'PHOTO' },
      { title: 'Historic square', description: 'Check in at a historic main square.', type: 'LOCATION' },
      { title: 'Local dessert', description: 'Photograph a local dessert.', type: 'PHOTO' },
      { title: 'River bridge', description: 'Stand on any river bridge.', type: 'LOCATION' },
      { title: 'Market stall', description: 'Snap a colourful market stall.', type: 'PHOTO' },
      { title: 'Hilltop view', description: 'Reach a hilltop viewpoint.', type: 'LOCATION' },
      { title: 'Neon sign', description: 'Photo of a neon sign at night.', type: 'PHOTO' },
      { title: 'Library', description: 'Visit a public library.', type: 'LOCATION' },
      { title: 'Bicycle', description: 'Photograph a parked bicycle.', type: 'PHOTO' },
      { title: 'Train station', description: 'Check in at any train station.', type: 'LOCATION' },
    ]),
  },
  {
    id: PAST_CARD_ID,
    weekStartDate: startOfLastWeek(),
    tasks: buildTasks(PAST_CARD_ID, [
      { title: 'Sunset photo', description: 'Capture the sunset.', type: 'PHOTO' },
      { title: 'Art gallery', description: 'Visit any art gallery.', type: 'LOCATION' },
      { title: 'Pastry', description: 'Photo of a local pastry.', type: 'PHOTO' },
      { title: 'Statue', description: 'Pose with a statue.', type: 'PHOTO' },
      { title: 'Cathedral', description: 'Visit a cathedral.', type: 'LOCATION' },
      { title: 'Reflection', description: 'Photo of a reflection in glass or water.', type: 'PHOTO' },
      { title: 'Fountain', description: 'Find a fountain in the city.', type: 'LOCATION' },
      { title: 'Old door', description: 'Photo of an old wooden door.', type: 'PHOTO' },
      { title: 'Café terrace', description: 'Sit at a café terrace.', type: 'LOCATION' },
      { title: 'Sky view', description: 'Photo of the sky between buildings.', type: 'PHOTO' },
      { title: 'Train station', description: 'Visit a train station.', type: 'LOCATION' },
      { title: 'Stair', description: 'Photograph a beautiful staircase.', type: 'PHOTO' },
      { title: 'Botanical garden', description: 'Visit a botanical garden.', type: 'LOCATION' },
      { title: 'Window', description: 'Photo of a decorative window.', type: 'PHOTO' },
      { title: 'Tower', description: 'Reach the top of a tower.', type: 'LOCATION' },
      { title: 'Coffee shop', description: 'Photograph a coffee shop interior.', type: 'PHOTO' },
    ]),
  },
];

export { CURRENT_CARD_ID, PAST_CARD_ID };
