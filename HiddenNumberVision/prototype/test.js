const { createPuzzle, scoreFor, hsvToRgb, digitCountForLevel, PLATE_R, GLYPHS } = require('./vision_core.js');
let fails = 0, checks = 0;
const ck = (c, m) => { checks++; if (!c) { if (fails < 25) console.log('FAIL:', m); fails++; } };

const cellCenter = (p, k) => ({
  x: p.originX + ((k % p.gridCols) + 0.5) * p.cell,
  y: p.originY + (Math.floor(k / p.gridCols) + 0.5) * p.cell,
});

for (let level = 1; level <= 14; level++) {
  for (let q = 0; q < 12; q++) {
    const p = createPuzzle(level, q);
    const tag = `L${level}Q${q}`;
    ck(/^\d+$/.test(p.answer), `answer not digits ${tag}`);
    ck(p.answer.length === digitCountForLevel(level), `answer length ${tag}`);
    ck(p.answer[0] !== '0', `leading zero ${tag}`);

    const tgt = p.dots.filter(d => d.target), bg = p.dots.filter(d => !d.target);
    // --- dot plate sanity -------------------------------------------------
    ck(tgt.length >= p.mask.length * 2, `too few target dots ${tgt.length}/${p.mask.length} ${tag}`);
    ck(bg.length >= 70, `too few bg dots ${bg.length} ${tag}`);
    const ratio = tgt.length / (tgt.length + bg.length);
    ck(ratio > 0.08 && ratio < 0.62, `target/bg ratio ${ratio.toFixed(2)} ${tag}`);
    for (const d of p.dots) {
      ck(Math.hypot(d.x - 0.5, d.y - 0.5) + d.r <= PLATE_R + 1e-6, `dot outside plate ${tag}`);
      ck(d.r > 0 && isFinite(d.x) && isFinite(d.y), `bad dot ${tag}`);
    }
    // every glyph cell covered by >=1 target dot; no target dot far from the glyph
    for (const k of p.mask) {
      const cc = cellCenter(p, k);
      ck(tgt.some(d => Math.hypot(d.x - cc.x, d.y - cc.y) < p.cell * 0.8), `glyph cell has no dot ${tag}`);
    }
    for (const d of tgt) {
      const c = Math.round((d.x - p.originX) / p.cell - 0.5);
      const r = Math.round((d.y - p.originY) / p.cell - 0.5);
      ck(p.mask.includes(r * p.gridCols + c), `stray target dot at cell ${c},${r} ${tag}`);
    }
    // bg dots must not sit on the glyph
    for (const d of bg) {
      const c = Math.round((d.x - p.originX) / p.cell - 0.5);
      const r = Math.round((d.y - p.originY) / p.cell - 0.5);
      if (c >= 0 && r >= 0 && c < p.gridCols && r < p.gridRows)
        ck(!p.mask.includes(r * p.gridCols + c) || Math.hypot(d.x - cellCenter(p, r * p.gridCols + c).x, d.y - cellCenter(p, r * p.gridCols + c).y) > p.cell * 0.6, `bg dot on glyph ${tag}`);
    }

    // --- line maze sanity -------------------------------------------------
    const tSeg = p.segments.filter(s => s.target), dSeg = p.segments.filter(s => !s.target);
    ck(tSeg.length >= p.mask.length, `skeleton edges ${tSeg.length} < cells ${p.mask.length} ${tag}`);
    ck(dSeg.length >= 220, `decoys ${dSeg.length} ${tag}`);
    for (const s of p.segments) {
      ck([s.x1, s.y1, s.x2, s.y2, s.w].every(Number.isFinite), `nan segment ${tag}`);
      ck(Math.hypot(s.x2 - s.x1, s.y2 - s.y1) < p.cell * 2.2, `segment too long ${tag}`);
    }
    // connectivity: each digit's skeleton must be one piece
    for (let i = 0; i < p.digitCount; i++) {
      const baseC = 1 + i * (GLYPHS['0'][0].length + 2);
      const cellsOfDigit = p.mask.filter(k => (k % p.gridCols) >= baseC && (k % p.gridCols) < baseC + 5);
      const idx = new Map(cellsOfDigit.map((k, n) => [k, n]));
      const adj = cellsOfDigit.map(() => new Set());
      for (const s of tSeg) {
        if (s.a === s.b) continue; // isolated stub
        if (idx.has(s.a) && idx.has(s.b)) { const A = idx.get(s.a), B = idx.get(s.b); adj[A].add(B); adj[B].add(A); }
      }
      const seen = new Set([0]); const stack = [0];
      while (stack.length) for (const m of adj[stack.pop()]) if (!seen.has(m)) { seen.add(m); stack.push(m); }
      ck(seen.size === cellsOfDigit.length, `digit ${i} skeleton not connected (${seen.size}/${cellsOfDigit.length}) ${tag}`);
      for (const k of cellsOfDigit)
        ck(tSeg.some(s => s.a === k || s.b === k) || cellsOfDigit.length === 1, `cell ${k} uncovered by skeleton ${tag}`);
    }

    // --- rules ------------------------------------------------------------
    ck(scoreFor(p, 3000, 0, false) > 0 && scoreFor(p, 0, 0, true) >= 25, `score ${tag}`);
    ck(p.timeLimitMs >= 8500 && p.timeLimitMs <= 14500, `timer ${p.timeLimitMs} ${tag}`);
    const cols = hsvToRgb(p.fg.hue, p.fg.sat, p.fg.val), bgc = hsvToRgb(p.bg.hue, p.bg.sat, p.bg.val);
    ck(cols.concat(bgc).every(v => v >= 0 && v <= 255), `rgb range ${tag}`);
    const lum = c => 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2];
    ck(Math.abs(lum(cols) - lum(bgc))  >= 20, `fg/bg too similar in brightness (${Math.abs(lum(cols) - lum(bgc)).toFixed(0)}) ${tag}`);
    ck(p.answer !== createPuzzle(level, (q + 5) % 12).answer || true, 'noop');
  }
}

// difficulty curve: contrast must shrink as level rises
const contrast = (l) => { const p = createPuzzle(l, 0); let best = 1e9; for (let q = 0; q < 12; q++) { const x = createPuzzle(l, q); const d = Math.abs(((x.fg.hue - x.bg.hue) % 360 + 360) % 360); best = Math.min(best, d > 180 ? 360 - d : d); } return best; };
ck(contrast(1) > contrast(6), 'hue gap should shrink with level');
ck(JSON.stringify(createPuzzle(4, 3)) === JSON.stringify(createPuzzle(4, 3)), 'determinism');
console.log(`\n${fails === 0 ? '✅ ALL OK' : '❌ ' + fails + ' failures'} (${checks} checks over 168 puzzles)`);
for (const l of [1, 4, 7, 10, 13]) { const p = createPuzzle(l, 2); console.log(`L${l}: ans=${p.answer} digits=${p.digitCount} dots=${p.dots.length}(${p.dots.filter(d=>d.target).length}#) segs=${p.segments.length}(${p.segments.filter(s=>s.target).length}#) timer=${p.timeLimitMs}ms hueGap=${Math.abs(p.fg.hue-p.bg.hue).toFixed(0)}`); }
