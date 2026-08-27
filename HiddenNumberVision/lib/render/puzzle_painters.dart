import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../game/puzzle.dart';

/// Paints the ishihara-style plate: the number is a band of dots in one colour,
/// everything else is noise in a close colour.
///
/// [hint] fades the noise, [reveal] lights the number up (used after a miss / timeout).
class DotPlatePainter extends CustomPainter {
  DotPlatePainter(this.puzzle, {this.hint = false, this.reveal = false});

  final Puzzle puzzle;
  final bool hint;
  final bool reveal;

  @override
  void paint(Canvas canvas, Size size) {
    final side = math.min(size.width, size.height);
    if (side <= 0) return;
    final ox = (size.width - side) / 2;
    final oy = (size.height - side) / 2;
    final paint = Paint()..isAntiAlias = true;

    paint.color = const Color(0xFF07080D);
    canvas.drawRect(Offset.zero & size, paint);
    paint.color = const Color(0xFF141926);
    canvas.drawCircle(Offset(ox + side * 0.5, oy + side * 0.5), kPlateRadius * side * 1.03, paint);

    for (final dot in puzzle.dots) {
      paint.color = _colorFor(dot.target, dot.tint);
      canvas.drawCircle(Offset(ox + dot.x * side, oy + dot.y * side), dot.r * side, paint);
    }

    if (reveal) {
      paint
        ..style = PaintingStyle.stroke
        ..strokeWidth = side * 0.006
        ..color = Colors.white54;
      canvas.drawCircle(Offset(ox + side * 0.5, oy + side * 0.5), kPlateRadius * side * 1.03, paint);
    }
  }

  Color _colorFor(bool target, double tint) {
    final base = target ? puzzle.fg : puzzle.bg;
    var pal = base.shift(hue: tint * 1.4);
    if (hint) {
      pal = target ? pal.shift(sat: 0.1, val: 0.06) : pal.shift(sat: -0.5, val: -0.35);
    }
    if (reveal) {
      pal = target ? Palette(pal.hue, 0, 1) : pal.shift(sat: -0.6, val: -0.55);
    }
    return Color(pal.toInt());
  }

  @override
  bool shouldRepaint(DotPlatePainter old) =>
      old.puzzle != puzzle || old.hint != hint || old.reveal != reveal;
}

/// Paints the same answer as a path puzzle: the digit skeleton is drawn first, then the
/// decoy corridors are drawn *over* it, so the number has to be traced by eye.
class MazePainter extends CustomPainter {
  MazePainter(this.puzzle, {this.hint = false, this.reveal = false});

  final Puzzle puzzle;
  final bool hint;
  final bool reveal;

  @override
  void paint(Canvas canvas, Size size) {
    final side = math.min(size.width, size.height);
    if (side <= 0) return;
    final ox = (size.width - side) / 2;
    final oy = (size.height - side) / 2;
    final paint = Paint()
      ..isAntiAlias = true
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    paint.color = const Color(0xFF07080D);
    paint.style = PaintingStyle.fill;
    canvas.drawRect(Offset.zero & size, paint);
    paint.color = const Color(0xFF141926);
    canvas.drawCircle(Offset(ox + side * 0.5, oy + side * 0.5), kPlateRadius * side * 1.02, paint);
    paint.style = PaintingStyle.stroke;

    void stroke(MazeSegment s) {
      paint
        ..strokeWidth = math.max(0.7, s.w * puzzle.cell * side * 0.82)
        ..color = _colorFor(s.target, 0);
      canvas.drawLine(Offset(ox + s.x1 * side, oy + s.y1 * side), Offset(ox + s.x2 * side, oy + s.y2 * side), paint);
    }

    for (final s in puzzle.segments) {
      if (!s.target) stroke(s);
    }
    for (final s in puzzle.segments) {
      if (s.target) stroke(s);
    }
  }

  Color _colorFor(bool target, double tint) {
    final base = target ? puzzle.fg : puzzle.bg;
    var pal = base.shift(hue: tint);
    if (hint) {
      pal = target ? pal.shift(sat: 0.1, val: 0.06) : pal.shift(sat: -0.5, val: -0.35);
    }
    if (reveal) {
      pal = target ? Palette(pal.hue, 0, 1) : pal.shift(sat: -0.6, val: -0.55);
    }
    return Color(pal.toInt());
  }

  @override
  bool shouldRepaint(MazePainter old) => old.puzzle != puzzle || old.hint != hint || old.reveal != reveal;
}
