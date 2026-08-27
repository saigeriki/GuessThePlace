import 'package:flutter/material.dart';

import 'game_screen.dart';

class ResultScreen extends StatelessWidget {
  const ResultScreen({
    super.key,
    required this.score,
    required this.previousBest,
    required this.correct,
    required this.total,
    required this.bestStreak,
    required this.startLevel,
    required this.endLevel,
    required this.style,
    required this.roundLength,
  });

  final int score;
  final int previousBest;
  final int correct;
  final int total;
  final int bestStreak;
  final int startLevel;
  final int endLevel;
  final PlayStyle style;
  final int roundLength;

  /// `int.clamp` hands back a `num`, so the curve is pinned with plain comparisons.
  int get _nextStartLevel {
    final v = endLevel - 1;
    return v < 1 ? 1 : (v > 12 ? 12 : v);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isNewBest = score > previousBest;
    final accuracy = total == 0 ? 0 : (correct * 100 / total).round();

    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(22, 16, 22, 24),
          children: <Widget>[
            Text('RESULT',
                style: theme.textTheme.labelSmall?.copyWith(letterSpacing: 4, color: Colors.white38)),
            const SizedBox(height: 6),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: <Widget>[
                Text('$score',
                    style: theme.textTheme.displayLarge?.copyWith(
                      fontWeight: FontWeight.w900,
                      color: const Color(0xFF6FE3A8),
                      height: 1,
                    )),
                const SizedBox(width: 10),
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Text('points', style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white38)),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (isNewBest)
              const _Pill(text: 'NEW PERSONAL BEST', color: Color(0xFF6FE3A8))
            else
              _Pill(text: 'best $previousBest', color: Colors.white38),
            const SizedBox(height: 22),
            _Stat(label: 'found', value: '$correct / $total'),
            _Stat(label: 'accuracy', value: '$accuracy%'),
            _Stat(label: 'best streak', value: '$bestStreak'),
            _Stat(label: 'levels', value: '$startLevel → $endLevel'),
            const SizedBox(height: 18),
            Card(
              margin: EdgeInsets.zero,
              color: theme.colorScheme.surfaceContainerLow,
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Text(
                  _advice(accuracy, startLevel, endLevel),
                  style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white70, height: 1.45),
                ),
              ),
            ),
            const SizedBox(height: 22),
            FilledButton(
              onPressed: () => Navigator.of(context).pushReplacement(
                MaterialPageRoute<void>(
                  builder: (_) => GameScreen(
                    style: style,
                    roundLength: roundLength,
                    startLevel: _nextStartLevel,
                  ),
                ),
              ),
              child: Text('Play again  ·  from level $_nextStartLevel'),
            ),
            const SizedBox(height: 10),
            OutlinedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(48)),
              child: const Text('Back to menu'),
            ),
          ],
        ),
      ),
    );
  }

  static String _advice(int accuracy, int from, int to) {
    if (accuracy >= 90) {
      return 'You are seeing plates most people miss. Try the Lines style at level 12 — '
          'there the number and the decoys are almost the same brightness, so shape has to do the work.';
    }
    if (accuracy >= 60) {
      return 'Solid. The jump from level ${to + 1 > 12 ? 12 : to + 1} is where two digits become three: '
          'stop hunting for colours and start tracing one continuous path with your eyes.';
    }
    if (accuracy >= 30) {
      return 'Normal — these plates are deliberately near-isochromatic. Use the "fade noise" hint '
          'once per puzzle to learn what the shape looks like, then wean yourself off it.';
    }
    return 'Keep the screen at arm\'s length and look at the plate as a whole instead of scanning '
        'dot by dot — peripheral vision separates colour families much better than focus does.';
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: color.withAlpha(28),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withAlpha(90)),
      ),
      child: Text(text,
          style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1)),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        children: <Widget>[
          SizedBox(
            width: 110,
            child: Text(label.toUpperCase(),
                style: const TextStyle(fontSize: 11, letterSpacing: 1.5, color: Colors.white38)),
          ),
          Text(value, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700)),
        ],
      ),
    );
  }
}
