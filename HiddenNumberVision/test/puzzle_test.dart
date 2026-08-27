import 'dart:math';

import 'package:flutter_test/flutter_test.dart';
import 'package:hidden_number_vision/game/digit_font.dart';
import 'package:hidden_number_vision/game/puzzle.dart';

/// The same invariants the JS prototype is tested against (prototype/test.js).
/// Geometry bugs here are invisible in code review but very visible on a phone,
/// so the generator is worth pinning down.
void main() {
  // rebuild the expected glyph mask for a given answer straight from the font
  Set<int> maskFor(String answer, int gridCols) {
    final mask = <int>{};
    for (var i = 0; i < answer.length; i++) {
      final baseC = 1 + i * (DigitFont.cols + kDigitGap);
      final glyph = DigitFont.of(answer[i]);
      for (var r = 0; r < DigitFont.rows; r++) {
        for (var c = 0; c < DigitFont.cols; c++) {
          if (glyph[r][c] == '1') mask.add((r + 1) * gridCols + baseC + c);
        }
      }
    }
    return mask;
  }

  group('generator', () {
    test('answers are well formed and match the level curve', () {
      for (var level = 1; level <= 14; level++) {
        for (var q = 0; q < 12; q++) {
          final p = PuzzleGenerator.create(level: level, question: q);
          expect(int.tryParse(p.answer), isNotNull, reason: 'level $level q $q');
          expect(p.answer.length, PuzzleGenerator.digitCountForLevel(level));
          expect(p.answer.startsWith('0'), isFalse);
          expect(p.timeLimit.inMilliseconds, inInclusiveRange(8500, 14500));
        }
      }
    });

    test('nothing is drawn outside the plate', () {
      for (var level = 1; level <= 14; level++) {
        final p = PuzzleGenerator.create(level: level, question: 3);
        for (final d in p.dots) {
          expect(hypot(d.x - 0.5, d.y - 0.5) + d.r, lessThanOrEqualTo(kPlateRadius + 1e-6));
          expect(d.r, greaterThan(0));
        }
        for (final s in p.segments) {
          expect(hypot(s.x2 - s.x1, s.y2 - s.y1), lessThanOrEqualTo(p.cell * 2.2));
        }
      }
    });

    test('every glyph cell gets ink in both styles', () {
      for (var level = 1; level <= 12; level++) {
        for (var q = 0; q < 6; q++) {
          final p = PuzzleGenerator.create(level: level, question: q);
          final gridCols = p.digitCount * DigitFont.cols + (p.digitCount - 1) * kDigitGap + 2;
          final gridRows = DigitFont.rows + 2;
          final mask = maskFor(p.answer, gridCols);
          final originX = 0.5 - gridCols * p.cell / 2;
          final originY = 0.5 - gridRows * p.cell / 2;
          expect(mask, isNotEmpty);

          final targets = p.dots.where((PlateDot d) => d.target).toList();
          for (final k in mask) {
            final cx = originX + (k % gridCols + 0.5) * p.cell;
            final cy = originY + (k ~/ gridCols + 0.5) * p.cell;
            expect(
              targets.any((PlateDot d) => hypot(d.x - cx, d.y - cy) < p.cell * 0.8),
              isTrue,
              reason: 'L$level Q$q: glyph cell $k has no dot',
            );
            // and the line style must cover it too
            expect(
              p.segments
                  .where((MazeSegment s) => s.target)
                  .any((MazeSegment s) => hypot(s.x1 - cx, s.y1 - cy) < p.cell * 0.8 ||
                      hypot(s.x2 - cx, s.y2 - cy) < p.cell * 0.8),
              isTrue,
              reason: 'L$level Q$q: glyph cell $k has no stroke',
            );
          }
          // noise dots never land on the number
          for (final d in p.dots.where((PlateDot d) => !d.target)) {
            final c = ((d.x - originX) / p.cell - 0.5).round();
            final r = ((d.y - originY) / p.cell - 0.5).round();
            if (c < 0 || r < 0 || c >= gridCols || r >= gridRows) continue;
            if (!mask.contains(r * gridCols + c)) continue;
            final dist = hypot(d.x - (originX + (c + 0.5) * p.cell), d.y - (originY + (r + 0.5) * p.cell));
            expect(dist, greaterThan(p.cell * 0.6), reason: 'noise sitting on the glyph');
          }
        }
      }
    });

    test('contrast tightens as the level rises', () {
      double hueGap(int level) {
        var best = 1e9;
        for (var q = 0; q < 12; q++) {
          final p = PuzzleGenerator.create(level: level, question: q);
          final d = (p.fg.hue - p.bg.hue).abs() % 360;
          best = min(best, d > 180 ? 360 - d : d);
        }
        return best;
      }

      expect(hueGap(1), greaterThan(hueGap(9)));
      for (var level = 1; level <= 14; level++) {
        for (var q = 0; q < 12; q++) {
          final p = PuzzleGenerator.create(level: level, question: q);
          // but never so close that nobody can see the number
          expect(
            (luminanceOf(p.fg.toInt()) - luminanceOf(p.bg.toInt())).abs(),
            greaterThanOrEqualTo(18),
            reason: 'L$level Q$q: fg/bg brightness gap too small',
          );
        }
      }
    });

    test('scoring and determinism', () {
      final a = PuzzleGenerator.create(level: 5, question: 2);
      final b = PuzzleGenerator.create(level: 5, question: 2);
      expect(a.answer, b.answer);
      expect(a.dots.length, b.dots.length);
      expect(a.segments.length, b.segments.length);

      expect(scoreFor(a, const Duration(seconds: 6), 0), greaterThan(scoreFor(a, const Duration(seconds: 1), 0)));
      expect(scoreFor(a, Duration.zero, 0, usedHint: true), greaterThanOrEqualTo(25));
      expect(scoreFor(a, const Duration(seconds: 6), 3), greaterThan(scoreFor(a, const Duration(seconds: 6), 0)));
    });
  });
}
