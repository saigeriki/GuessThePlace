const { createPuzzle } = require('./vision_core.js');
// how expensive is one puzzle? (Dart on a mid-range phone is roughly the same order)
createPuzzle(7, 3); // warm up
const t0 = process.hrtime.bigint();
const N = 2000;
let dots = 0, segs = 0;
for (let i = 0; i < N; i++) { const p = createPuzzle(1 + (i % 14), i % 12); dots += p.dots.length; segs += p.segments.length; }
const ms = Number(process.hrtime.bigint() - t0) / 1e6;
console.log(`avg ${ (ms/N).toFixed(2) } ms per puzzle in JS (dots ${(dots/N).toFixed(0)}, segs ${(segs/N).toFixed(0)})`);
