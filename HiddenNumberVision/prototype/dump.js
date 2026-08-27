const { createPuzzle } = require('./vision_core.js');
const out = [];
for (const [level, q] of [[1,0],[2,3],[5,1],[8,4],[11,2],[14,5]]) out.push(createPuzzle(level, q));
require('fs').writeFileSync('/tmp/puzzles.json', JSON.stringify(out));
