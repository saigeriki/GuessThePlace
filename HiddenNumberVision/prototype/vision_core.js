/*
 * vision_core.js — pure puzzle generator (no DOM).
 * Reference implementation of the Dart code in ../lib/game/ — keep them in sync:
 * same constants, same geometry, same difficulty curve.
 *
 * One puzzle, two renderings:
 *   dots  -> ishihara-style colour plate (number made of dots)
 *   lines -> the number hidden inside a random line maze
 * Both are generated from the same 5x7 glyph bitmap, so the answer is identical.
 */

const GLYPH_COLS = 5;
const GLYPH_ROWS = 7;

const GLYPHS = {
  '0': ['01110', '10001', '10011', '10101', '11001', '10001', '01110'],
  '1': ['00100', '01100', '00100', '00100', '00100', '00100', '01110'],
  '2': ['01110', '10001', '00001', '00010', '00100', '01000', '11111'],
  '3': ['11111', '00010', '00100', '00010', '00001', '10001', '01110'],
  '4': ['00010', '00110', '01010', '10010', '11111', '00010', '00010'],
  '5': ['11111', '10000', '11110', '00001', '00001', '10001', '01110'],
  '6': ['00110', '01000', '10000', '11110', '10001', '10001', '01110'],
  '7': ['11111', '00001', '00010', '00100', '01000', '01000', '01000'],
  '8': ['01110', '10001', '10001', '01110', '10001', '10001', '01110'],
  '9': ['01110', '10001', '10001', '01111', '00001', '00010', '01100'],
};

const DIGIT_GAP = 2; // empty grid columns between two digits
const PLATE_R = 0.44; // plate radius, in normalized 0..1 canvas space
const FIT = 0.86; // the glyph grid's half-diagonal may use this much of the plate radius
const TINT_RANGE = 7; // per-dot hue wobble, in degrees

function lerp(a, b, t) {
  return a + (b - a) * t;
}
function clampD(v, lo, hi) {
  return v < lo ? lo : v > hi ? hi : v;
}
function clampI(v, lo, hi) {
  return v < lo ? lo : v > hi ? hi : v;
}

/** mulberry32: small deterministic PRNG, so (level, question) always renders the same plate. */
function makeRng(seed) {
  let a = seed >>> 0;
  return function () {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), 1 | t);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function digitCountForLevel(level) {
  return clampI(1 + Math.floor((level - 1) / 3), 1, 3);
}

// +x, +y and the two downward diagonals: every adjacency is visited from exactly one side.
const LINK_DIRS = [
  [1, 0],
  [0, 1],
  [1, 1],
  [-1, 1],
];

/**
 * @param {number} level   1..N — drives digit count, colour contrast, decoy density, timer
 * @param {number} question 0-based index inside the level
 */
function createPuzzle(level, question) {
  const rnd = makeRng(level * 977 + question * 131 + 17);
  const digitCount = digitCountForLevel(level);
  const diff = clampD((level - 1) / 9, 0, 1);

  const digits = [String(1 + Math.floor(rnd() * 9))];
  for (let i = 1; i < digitCount; i++) digits.push(String(Math.floor(rnd() * 10)));
  const answer = digits.join('');

  // ---------- grid + glyph mask ----------
  const gridRows = GLYPH_ROWS + 2;
  const gridCols = digitCount * GLYPH_COLS + (digitCount - 1) * DIGIT_GAP + 2;
  const cell = (2 * PLATE_R * FIT) / Math.hypot(gridCols, gridRows);
  const originX = 0.5 - (gridCols * cell) / 2;
  const originY = 0.5 - (gridRows * cell) / 2;

  const mask = new Set();
  for (let i = 0; i < digitCount; i++) {
    const baseC = 1 + i * (GLYPH_COLS + DIGIT_GAP);
    const g = GLYPHS[digits[i]];
    for (let r = 0; r < GLYPH_ROWS; r++) {
      for (let c = 0; c < GLYPH_COLS; c++) {
        if (g[r][c] === '1') mask.add((r + 1) * gridCols + baseC + c);
      }
    }
  }
  const cx = (c) => originX + (c + 0.5) * cell;
  const cy = (r) => originY + (r + 0.5) * cell;
  const keyC = (k) => k % gridCols;
  const keyR = (k) => Math.floor(k / gridCols);

  // Anything inside this radius of a glyph cell centre belongs to the number.
  const bandPad = cell * 0.62;
  const onNumber = (px, py, extra) => {
    const reach = bandPad + extra;
    const c0 = Math.floor((px - reach - originX) / cell);
    const c1 = Math.floor((px + reach - originX) / cell);
    const r0 = Math.floor((py - reach - originY) / cell);
    const r1 = Math.floor((py + reach - originY) / cell);
    for (let r = r0; r <= r1; r++) {
      for (let c = c0; c <= c1; c++) {
        if (c < 0 || r < 0 || c >= gridCols || r >= gridRows) continue;
        if (!mask.has(r * gridCols + c)) continue;
        const dx = px - cx(c);
        const dy = py - cy(r);
        if (dx * dx + dy * dy < reach * reach) return true;
      }
    }
    return false;
  };

  const insidePlate = (x, y, pad) => {
    const dx = x - 0.5;
    const dy = y - 0.5;
    const r = PLATE_R - pad;
    return dx * dx + dy * dy <= r * r;
  };

  // ---------- mode A: dot plate ----------
  const dots = [];
  for (const k of mask) {
    const c = keyC(k),
      r = keyR(k);
    // 2-3 dots per glyph cell so the number reads as a thick band, not a hairline
    const per = 2 + (rnd() < 0.5 ? 1 : 0);
    for (let i = 0; i < per; i++) {
      let x = cx(c) + (rnd() - 0.5) * cell * 0.9;
      let y = cy(r) + (rnd() - 0.5) * cell * 0.9;
      const rad = cell * (0.2 + rnd() * 0.22);
      if (!insidePlate(x, y, rad)) {
        // pull the stray dot back towards the plate centre
        const d = Math.hypot(x - 0.5, y - 0.5) || 1;
        const k2 = (PLATE_R - rad) / d;
        x = 0.5 + (x - 0.5) * k2;
        y = 0.5 + (y - 0.5) * k2;
      }
      dots.push({ x, y, r: rad, target: true, tint: (rnd() * 2 - 1) * TINT_RANGE });
    }
  }

  const avgR = cell * 0.31;
  const capacity = Math.round((Math.PI * PLATE_R * PLATE_R) / (avgR * avgR * 3.2));
  const bgGoal = clampI(Math.round(capacity * 0.44 + diff * 70), 80, 560);
  let guard = 0;
  while (dots.filter((d) => !d.target).length < bgGoal && guard < 40000) {
    guard++;
    const a = rnd() * 2 * Math.PI;
    const rr = PLATE_R * Math.sqrt(rnd()) * 0.97;
    const x = 0.5 + Math.cos(a) * rr;
    const y = 0.5 + Math.sin(a) * rr;
    const rad = cell * (0.13 + rnd() * 0.28);
    if (!insidePlate(x, y, rad)) continue; // a dot may not cross the plate rim
    if (onNumber(x, y, rad)) continue;
    let ok = true;
    for (const d of dots) {
      const dx = x - d.x;
      const dy = y - d.y;
      const minD = (rad + d.r) * 0.86;
      if (dx * dx + dy * dy < minD * minD) {
        ok = false;
        break;
      }
    }
    if (!ok) continue;
    dots.push({ x, y, r: rad, target: false, tint: (rnd() * 2 - 1) * TINT_RANGE });
  }

  // ---------- mode B: line maze ----------
  const segments = [];
  for (const k of mask) {
    const c = keyC(k),
      r = keyR(k);
    let links = 0;
    for (const [dc, dr] of LINK_DIRS) {
      const nc = c + dc,
        nr = r + dr;
      if (nc < 0 || nc >= gridCols || nr >= gridRows) continue;
      if (!mask.has(nr * gridCols + nc)) continue;
      const j = () => (rnd() - 0.5) * cell * 0.26;
      segments.push({
        x1: cx(c) + j(),
        y1: cy(r) + j(),
        x2: cx(nc) + j(),
        y2: cy(nr) + j(),
        target: true,
        w: 1.05,
        a: k,
        b: nr * gridCols + nc,
      });
      links++;
    }
    if (links === 0) {
      // isolated pixel: a short stub keeps the cell visible
      const ang = Math.floor(rnd() * 8) * (Math.PI / 4);
      segments.push({
        x1: cx(c),
        y1: cy(r),
        x2: cx(c) + Math.cos(ang) * cell * 0.5,
        y2: cy(r) + Math.sin(ang) * cell * 0.5,
        target: true,
        w: 1.05,
        a: k,
        b: k,
      });
    }
  }

  // decoys: short random walks on the 4 main directions -> reads as corridors, not confetti
  const decoyGoal = 220 + Math.round(diff * 280);
  const lim = PLATE_R * 0.985;
  let decoys = 0;
  let guard2 = 0;
  while (decoys < decoyGoal && guard2 < decoyGoal * 60) {
    guard2++;
    const a = rnd() * 2 * Math.PI;
    const rr = PLATE_R * Math.sqrt(rnd()) * 0.93;
    let x = 0.5 + Math.cos(a) * rr;
    let y = 0.5 + Math.sin(a) * rr;
    let dir = Math.floor(rnd() * 4) * (Math.PI / 2) + (rnd() - 0.5) * 0.5;
    const steps = 3 + Math.floor(rnd() * 6);
    const len = cell * (0.8 + rnd() * 0.9);
    for (let s = 0; s < steps; s++) {
      const nx = x + Math.cos(dir) * len;
      const ny = y + Math.sin(dir) * len;
      if (!insidePlate(nx, ny, cell * 0.04)) break;
      segments.push({ x1: x, y1: y, x2: nx, y2: ny, target: false, w: 0.78, a: -1, b: -1 });
      decoys++;
      x = nx;
      y = ny;
      if (rnd() < 0.55) dir += rnd() < 0.5 ? Math.PI / 2 : -Math.PI / 2;
    }
  }

  // ---------- palette: contrast is the difficulty knob ----------
  const baseHue = rnd() * 360;
  const hueGap = lerp(80, 22, diff);
  const satGap = lerp(0.4, 0.05, diff);
  const valGap = lerp(0.28, 0.04, diff);
  const fg = { hue: baseHue, sat: 0.85, val: 0.95 };
  const bg = { hue: baseHue + hueGap, sat: 0.85 - satGap, val: 0.95 - valGap };
  // Hue separation alone can produce two colours of equal brightness (then nobody finds the
  // number, even with perfect vision). Push the value gap until it is visible but not obvious.
  const lum = (c) => {
    const [r, g, b] = hsvToRgb(c.hue, c.sat, c.val);
    return 0.299 * r + 0.587 * g + 0.114 * b;
  };
  const needLum = lerp(50, 26, diff);
  for (let i = 0; i < 40 && Math.abs(lum(fg) - lum(bg)) < needLum; i++) {
    bg.val = Math.max(0.1, bg.val - 0.03);
    fg.val = Math.min(1, fg.val + 0.01);
    bg.sat = Math.min(1, bg.sat + 0.012);
  }

  return {
    level,
    question,
    answer,
    digitCount,
    diff,
    cell,
    gridCols,
    gridRows,
    originX,
    originY,
    mask: [...mask],
    dots,
    segments,
    fg,
    bg,
    lumGap: Math.abs(lum(fg) - lum(bg)),
    timeLimitMs: clampI(Math.round(14500 - 400 * (level - 1)), 8500, 14500),
    targetDotCount: dots.filter((d) => d.target).length,
  };
}

function scoreFor(puzzle, remainingMs, streak, usedHint) {
  const base = 100 * puzzle.digitCount;
  const timeBonus = Math.round((remainingMs / 1000) * 15);
  const raw = base + timeBonus + streak * 20 - (usedHint ? 60 : 0);
  return Math.max(25, raw);
}

function hsvToRgb(h, s, v) {
  const hh = (((h % 360) + 360) % 360) / 60;
  const c = v * s;
  const x = c * (1 - Math.abs((hh % 2) - 1));
  const m = v - c;
  let r = 0,
    g = 0,
    b = 0;
  if (hh < 1) [r, g, b] = [c, x, 0];
  else if (hh < 2) [r, g, b] = [x, c, 0];
  else if (hh < 3) [r, g, b] = [0, c, x];
  else if (hh < 4) [r, g, b] = [0, x, c];
  else if (hh < 5) [r, g, b] = [x, 0, c];
  else [r, g, b] = [c, 0, x];
  const q = (u) => Math.round(255 * clampD(u + m, 0, 1));
  return [q(r), q(g), q(b)];
}

if (typeof module !== 'undefined') {
  module.exports = {
    createPuzzle,
    scoreFor,
    hsvToRgb,
    digitCountForLevel,
    GLYPHS,
    lerp,
    clampD,
    clampI,
    makeRng,
    PLATE_R,
    TINT_RANGE,
  };
}
