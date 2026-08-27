import 'dart:math';

import 'digit_font.dart';

/// ---- tuning knobs (keep in sync with prototype/vision_core.js) ----------
/// plate radius in normalised 0..1 canvas space
const double kPlateRadius = 0.44;
/// how much of the plate radius the glyph grid's half-diagonal may use
const double kFit = 0.86;
/// empty grid columns between two digits
const int kDigitGap = 2;
/// per-element hue wobble in degrees, so the plate does not look flat
const double kTintRange = 7;
/// stroke widths in grid cells: the number is a touch heavier than the clutter, and it is
/// drawn last — if the decoys sit on top they bury it completely and it stops being a puzzle
const double kNumberStroke = 1.05;
const double kDecoyStroke = 0.62;

double _lerp(double a, double b, double t) => a + (b - a) * t;
double _clampD(double v, double lo, double hi) => v < lo ? lo : (v > hi ? hi : v);
int _clampI(int v, int lo, int hi) => v < lo ? lo : (v > hi ? hi : v);

double clamp01(double v) => _clampD(v, 0, 1);

/// HSV -> packed 0xAARRGGBB. Kept here (instead of `HSLColor`) so this file stays
/// free of any Flutter import and can be unit tested as pure Dart.
int hsvToInt(double hue, double sat, double val, {double alpha = 1}) {
  final h = (((hue % 360) + 360) % 360) / 60;
  final c = val * sat;
  final x = c * (1 - (h % 2 - 1).abs());
  final m = val - c;
  double r = 0, g = 0, b = 0;
  if (h < 1) {
    r = c;
    g = x;
  } else if (h < 2) {
    r = x;
    g = c;
  } else if (h < 3) {
    g = c;
    b = x;
  } else if (h < 4) {
    g = x;
    b = c;
  } else if (h < 5) {
    r = x;
    b = c;
  } else {
    r = c;
    b = x;
  }
  return (_clampI((alpha * 255).round(), 0, 255) << 24) |
      (_clampI((255 * (r + m)).round(), 0, 255) << 16) |
      (_clampI((255 * (g + m)).round(), 0, 255) << 8) |
      _clampI((255 * (b + m)).round(), 0, 255);
}

/// Perceived brightness of a packed colour — used to keep the number actually findable.
double luminanceOf(int argb) =>
    0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF) + 0.114 * (argb & 0xFF);

class Palette {
  const Palette(this.hue, this.sat, this.val);
  final double hue;
  final double sat;
  final double val;

  Palette shift({double hue = 0, double sat = 0, double val = 0}) =>
      Palette(this.hue + hue, clamp01(this.sat + sat), clamp01(this.val + val));

  int toInt({double alpha = 1}) => hsvToInt(hue, sat, val, alpha: alpha);
}

class PlateDot {
  const PlateDot(this.x, this.y, this.r, this.target, this.tint);

  /// normalised (0..1) centre inside a square canvas
  final double x;
  final double y;
  final double r;

  /// true -> part of the hidden number, false -> noise
  final bool target;
  final double tint;
}

class MazeSegment {
  const MazeSegment(this.x1, this.y1, this.x2, this.y2, this.w, this.target);

  final double x1;
  final double y1;
  final double x2;
  final double y2;

  /// stroke width, expressed in grid cells
  final double w;
  final bool target;
}

class Puzzle {
  const Puzzle({
    required this.level,
    required this.question,
    required this.answer,
    required this.digitCount,
    required this.cell,
    required this.dots,
    required this.segments,
    required this.fg,
    required this.bg,
    required this.timeLimit,
  });

  final int level;
  final int question;
  final String answer;
  final int digitCount;

  /// size of one glyph grid cell, in normalised units
  final double cell;
  final List<PlateDot> dots;
  final List<MazeSegment> segments;
  final Palette fg;
  final Palette bg;
  final Duration timeLimit;

  int get points => 100 * digitCount;
}

/// Deterministic: the same (level, question) always produces the same plate,
/// so a puzzle can be re-rendered (rotate, hint, share) without changing it.
class PuzzleGenerator {
  const PuzzleGenerator._();

  static int digitCountForLevel(int level) => _clampI(1 + (level - 1) ~/ 3, 1, 3);

  static Puzzle create({required int level, required int question}) {
    final rnd = Random(level * 977 + question * 131 + 17);
    final digitCount = digitCountForLevel(level);
    final diff = clamp01((level - 1) / 9);

    final digits = <String>['${1 + rnd.nextInt(9)}'];
    for (var i = 1; i < digitCount; i++) {
      digits.add('${rnd.nextInt(10)}');
    }
    final answer = digits.join();

    // ---------- grid + glyph mask ----------
    final gridRows = DigitFont.rows + 2;
    final gridCols = digitCount * DigitFont.cols + (digitCount - 1) * kDigitGap + 2;
    // size the grid by its diagonal: the outermost glyph corners must stay inside the disc
    final cell = (2 * kPlateRadius * kFit) / sqrt(gridCols * gridCols + gridRows * gridRows);
    final originX = 0.5 - gridCols * cell / 2;
    final originY = 0.5 - gridRows * cell / 2;

    final mask = <int>{};
    for (var i = 0; i < digitCount; i++) {
      final baseC = 1 + i * (DigitFont.cols + kDigitGap);
      final glyph = DigitFont.of(digits[i]);
      for (var r = 0; r < DigitFont.rows; r++) {
        final line = glyph[r];
        for (var c = 0; c < DigitFont.cols; c++) {
          if (line.codeUnitAt(c) == 0x31) mask.add((r + 1) * gridCols + baseC + c);
        }
      }
    }

    double cx(int c) => originX + (c + 0.5) * cell;
    double cy(int r) => originY + (r + 0.5) * cell;

    final bandPad = cell * 0.62;
    bool onNumber(double px, double py, double extra) {
      final reach = bandPad + extra;
      final c0 = ((px - reach - originX) / cell).floor();
      final c1 = ((px + reach - originX) / cell).floor();
      final r0 = ((py - reach - originY) / cell).floor();
      final r1 = ((py + reach - originY) / cell).floor();
      for (var r = r0; r <= r1; r++) {
        for (var c = c0; c <= c1; c++) {
          if (c < 0 || r < 0 || c >= gridCols || r >= gridRows) continue;
          if (!mask.contains(r * gridCols + c)) continue;
          final dx = px - cx(c);
          final dy = py - cy(r);
          if (dx * dx + dy * dy < reach * reach) return true;
        }
      }
      return false;
    }

    bool insidePlate(double x, double y, double pad) {
      final dx = x - 0.5;
      final dy = y - 0.5;
      final r = kPlateRadius - pad;
      return dx * dx + dy * dy <= r * r;
    }

    // ---------- style A: ishihara-style dot plate ----------
    final dots = <PlateDot>[];
    for (final k in mask) {
      final c = k % gridCols;
      final r = k ~/ gridCols;
      // 2-3 dots per glyph cell: the number reads as a thick band, not a hairline
      final per = 2 + (rnd.nextBool() ? 1 : 0);
      for (var i = 0; i < per; i++) {
        var x = cx(c) + (rnd.nextDouble() - 0.5) * cell * 0.9;
        var y = cy(r) + (rnd.nextDouble() - 0.5) * cell * 0.9;
        final rad = cell * (0.2 + rnd.nextDouble() * 0.22);
        if (!insidePlate(x, y, rad)) {
          final d = sqrt((x - 0.5) * (x - 0.5) + (y - 0.5) * (y - 0.5));
          if (d > 0) {
            final k2 = (kPlateRadius - rad) / d;
            x = 0.5 + (x - 0.5) * k2;
            y = 0.5 + (y - 0.5) * k2;
          }
        }
        dots.add(PlateDot(x, y, rad, true, (rnd.nextDouble() * 2 - 1) * kTintRange));
      }
    }

    // Background dots: rejection sampling inside the disc — never on the number, never
    // overlapping another dot, which is what gives the plate its organic look.
    final avgR = cell * 0.31;
    final capacity = (pi * kPlateRadius * kPlateRadius / (avgR * avgR * 3.2)).round();
    final bgGoal = _clampI((capacity * 0.44 + diff * 70).round(), 80, 560);
    var bgCount = 0;
    var guard = 0;
    while (bgCount < bgGoal && guard < 40000) {
      guard++;
      final a = rnd.nextDouble() * 2 * pi;
      final rr = kPlateRadius * sqrt(rnd.nextDouble()) * 0.97;
      final x = 0.5 + cos(a) * rr;
      final y = 0.5 + sin(a) * rr;
      final rad = cell * (0.13 + rnd.nextDouble() * 0.28);
      if (!insidePlate(x, y, rad)) continue; // a dot may not cross the plate rim
      if (onNumber(x, y, rad)) continue;
      var ok = true;
      for (final d in dots) {
        final dx = x - d.x;
        final dy = y - d.y;
        final minD = (rad + d.r) * 0.86;
        if (dx * dx + dy * dy < minD * minD) {
          ok = false;
          break;
        }
      }
      if (!ok) continue;
      dots.add(PlateDot(x, y, rad, false, (rnd.nextDouble() * 2 - 1) * kTintRange));
      bgCount++;
    }

    // ---------- style B: number hidden in a line maze ----------
    final segments = <MazeSegment>[];
    double j() => (rnd.nextDouble() - 0.5) * cell * 0.26;

    for (final k in mask) {
      final c = k % gridCols;
      final r = k ~/ gridCols;
      var links = 0;
      // +x, +y and both downward diagonals: every adjacency is visited from one side only.
      // The diagonals matter — without them '2' and '7' break into floating pieces.
      for (var d = 0; d < _linkDirs.length; d++) {
        final dc = _linkDirs[d][0];
        final dr = _linkDirs[d][1];
        final nc = c + dc;
        final nr = r + dr;
        if (nc < 0 || nc >= gridCols || nr >= gridRows) continue;
        if (!mask.contains(nr * gridCols + nc)) continue;
        segments.add(MazeSegment(cx(c) + j(), cy(r) + j(), cx(nc) + j(), cy(nr) + j(), kNumberStroke, true));
        links++;
      }
      if (links == 0) {
        // an isolated pixel gets a short stub, so no part of the digit disappears
        final ang = rnd.nextInt(8) * pi / 4;
        segments.add(MazeSegment(
          cx(c),
          cy(r),
          cx(c) + cos(ang) * cell * 0.5,
          cy(r) + sin(ang) * cell * 0.5,
          kNumberStroke,
          true,
        ));
      }
    }

    // Decoys: short random walks on 4 directions -> reads as corridors, not confetti.
    // They are allowed to cross the number; that overlap is the whole difficulty of this style.
    final decoyGoal = (220 + diff * 280).round();
    var decoys = 0;
    var guard2 = 0;
    while (decoys < decoyGoal && guard2 < decoyGoal * 60) {
      guard2++;
      final a = rnd.nextDouble() * 2 * pi;
      final rr = kPlateRadius * sqrt(rnd.nextDouble()) * 0.93;
      var x = 0.5 + cos(a) * rr;
      var y = 0.5 + sin(a) * rr;
      var dir = rnd.nextInt(4) * pi / 2 + (rnd.nextDouble() - 0.5) * 0.5;
      final steps = 3 + rnd.nextInt(6);
      final len = cell * (0.8 + rnd.nextDouble() * 0.9);
      for (var s = 0; s < steps; s++) {
        final nx = x + cos(dir) * len;
        final ny = y + sin(dir) * len;
        if (!insidePlate(nx, ny, cell * 0.04)) break;
        segments.add(MazeSegment(x, y, nx, ny, kDecoyStroke, false));
        decoys++;
        x = nx;
        y = ny;
        if (rnd.nextDouble() < 0.55) dir += rnd.nextBool() ? pi / 2 : -pi / 2;
      }
    }

    // ---------- palette: colour contrast IS the difficulty ----------
    final baseHue = rnd.nextDouble() * 360;
    final hueGap = _lerp(80, 22, diff);
    final satGap = _lerp(0.4, 0.05, diff);
    final valGap = _lerp(0.28, 0.04, diff);
    var fg = Palette(baseHue, 0.85, 0.95);
    var bg = Palette(baseHue + hueGap, clamp01(0.85 - satGap), clamp01(0.95 - valGap));

    // Hue separation alone can produce two colours of equal brightness — then nobody finds
    // the number, not even people with perfect vision. Force a visible-but-not-obvious gap.
    final needLum = _lerp(50, 26, diff);
    for (var i = 0; i < 40 && (luminanceOf(fg.toInt()) - luminanceOf(bg.toInt())).abs() < needLum; i++) {
      fg = fg.shift(val: 0.01);
      bg = bg.shift(sat: 0.012, val: -0.03);
    }

    return Puzzle(
      level: level,
      question: question,
      answer: answer,
      digitCount: digitCount,
      cell: cell,
      dots: List<PlateDot>.unmodifiable(dots),
      segments: List<MazeSegment>.unmodifiable(segments),
      fg: fg,
      bg: bg,
      timeLimit: Duration(
        milliseconds: _clampI((14500 - 400 * (level - 1)).round(), 8500, 14500),
      ),
    );
  }

  static const List<List<int>> _linkDirs = <List<int>>[
    <int>[1, 0],
    <int>[0, 1],
    <int>[1, 1],
    <int>[-1, 1],
  ];
}

/// Base points scale with the number length, then time and streak bonuses are added.
/// A hint costs points so it stays a choice, not a reflex.
int scoreFor(Puzzle puzzle, Duration remaining, int streak, {bool usedHint = false}) {
  final timeBonus = (remaining.inMilliseconds / 1000 * 15).round();
  final raw = puzzle.points + timeBonus + streak * 20 - (usedHint ? 60 : 0);
  return raw < 25 ? 25 : raw;
}
