import 'package:flutter/material.dart';

import '../game/prefs.dart';
import '../game/puzzle.dart';
import 'game_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  PlayStyle _style = PlayStyle.mixed;
  int _roundLength = 10;
  int _startLevel = 1;
  int _best = 0;

  @override
  void initState() {
    super.initState();
    _loadBest();
  }

  Future<void> _loadBest() async {
    final best = await Prefs.best();
    if (mounted) setState(() => _best = best);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                Text('NETRA', style: theme.textTheme.titleMedium?.copyWith(letterSpacing: 6)),
                Chip(
                  label: Text('best $_best'),
                  backgroundColor: theme.colorScheme.surfaceContainerHighest,
                  side: BorderSide.none,
                ),
              ],
            ),
            const SizedBox(height: 18),
            Text(
              'Find the number\nhidden in the plate',
              style: theme.textTheme.displaySmall?.copyWith(
                fontWeight: FontWeight.w800,
                height: 1.05,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 10),
            Text(
              'Nothing is stored in a file — every puzzle is generated from a 5x7 glyph '
              'bitmap at runtime, then painted by a CustomPainter. Contrast, dot density '
              'and decoy count are what make it hard.',
              style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white70, height: 1.45),
            ),
            const SizedBox(height: 24),
            _Section(label: 'Style', child: _stylePicker(theme)),
            _Section(label: 'Questions', child: _roundPicker(theme)),
            _Section(label: 'Start level', child: _levelPicker(theme)),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => GameScreen(
                      style: _style,
                      roundLength: _roundLength,
                      startLevel: _startLevel,
                    ),
                  ),
                );
              },
              child: const Text('Start test'),
            ),
            const SizedBox(height: 16),
            Card(
              margin: EdgeInsets.zero,
              color: theme.colorScheme.surfaceContainerLow,
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        const Icon(Icons.visibility_outlined, size: 18),
                        const SizedBox(width: 8),
                        Text('Difficulty curve', style: theme.textTheme.titleSmall),
                      ],
                    ),
                    const SizedBox(height: 10),
                    for (final row in _curve())
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 2),
                        child: Text(row, style: theme.textTheme.bodySmall?.copyWith(color: Colors.white70)),
                      ),
                    const SizedBox(height: 10),
                    Text(
                      'For fun and practice only — this is not a medical eye test. '
                      'If you genuinely struggle with the coloured plates, see an optometrist.',
                      style: theme.textTheme.bodySmall?.copyWith(color: Colors.white38, height: 1.4),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<String> _curve() => <String>[
        'lvl ${_range(1, 3)} — 1 digit, wide colour gap',
        'lvl ${_range(4, 6)} — 2 digits, gap narrows',
        'lvl ${_range(7, 9)} — 3 digits, more decoy lines',
        'lvl 10+ — near-isochromatic, tight timer, 3 digits',
      ];

  static String _range(int a, int b) => '$a-$b';

  Widget _stylePicker(ThemeData theme) => SegmentedButton<PlayStyle>(
        segments: const <ButtonSegment<PlayStyle>>[
          ButtonSegment<PlayStyle>(value: PlayStyle.dots, label: Text('Dots')),
          ButtonSegment<PlayStyle>(value: PlayStyle.lines, label: Text('Lines')),
          ButtonSegment<PlayStyle>(value: PlayStyle.mixed, label: Text('Mixed')),
        ],
        selected: <PlayStyle>{_style},
        onSelectionChanged: (Set<PlayStyle> s) => setState(() => _style = s.first),
      );

  Widget _roundPicker(ThemeData theme) => SegmentedButton<int>(
        segments: const <ButtonSegment<int>>[
          ButtonSegment<int>(value: 5, label: Text('5')),
          ButtonSegment<int>(value: 10, label: Text('10')),
          ButtonSegment<int>(value: 20, label: Text('20')),
        ],
        selected: <int>{_roundLength},
        onSelectionChanged: (Set<int> s) => setState(() => _roundLength = s.first),
      );

  Widget _levelPicker(ThemeData theme) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Slider(
            value: _startLevel.toDouble(),
            min: 1,
            max: 12,
            divisions: 11,
            label: 'level $_startLevel',
            onChanged: (double v) => setState(() => _startLevel = v.round()),
          ),
          Text(
            'digits per puzzle: ${PuzzleGenerator.digitCountForLevel(_startLevel)}',
            style: theme.textTheme.bodySmall?.copyWith(color: Colors.white54),
          ),
        ],
      );
}

class _Section extends StatelessWidget {
  const _Section({required this.label, required this.child});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(label.toUpperCase(),
              style: theme.textTheme.labelSmall?.copyWith(letterSpacing: 2, color: Colors.white38)),
          const SizedBox(height: 8),
          child,
        ],
      ),
    );
  }
}
