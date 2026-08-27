import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../game/prefs.dart';
import '../game/puzzle.dart';
import '../render/puzzle_painters.dart';
import 'result_screen.dart';

enum PlayStyle { dots, lines, mixed }

class GameScreen extends StatefulWidget {
  const GameScreen({
    super.key,
    required this.style,
    required this.roundLength,
    required this.startLevel,
  });

  final PlayStyle style;
  final int roundLength;
  final int startLevel;

  @override
  State<GameScreen> createState() => _GameScreenState();
}

enum _Phase { answering, feedback }

class _GameScreenState extends State<GameScreen> {
  static const int _maxLives = 3;
  static const int _maxHints = 3;

  final StringBuffer _typed = StringBuffer();

  Timer? _ticker;
  late Puzzle _puzzle;
  PlayStyle _style = PlayStyle.dots;

  _Phase _phase = _Phase.answering;
  int _question = 0;
  int _level = 1;
  int _score = 0;
  int _streak = 0;
  int _bestStreak = 0;
  int _correct = 0;
  int _lives = _maxLives;
  int _hints = _maxHints;
  int _remainingMs = 0;
  bool _hintOn = false;
  bool _usedHint = false;
  bool _reveal = false;
  String _banner = '';
  Color _bannerColor = Colors.transparent;

  @override
  void initState() {
    super.initState();
    _level = widget.startLevel;
    _deal();
    _startTimer();
  }

  @override
  void dispose() {
    _ticker?.cancel();
    super.dispose();
  }

  void _deal() {
    _puzzle = PuzzleGenerator.create(level: _level, question: _question);
    if (widget.style == PlayStyle.mixed) {
      _style = _question.isEven ? PlayStyle.dots : PlayStyle.lines;
    } else {
      _style = widget.style;
    }
    _typed.clear();
    _remainingMs = _puzzle.timeLimit.inMilliseconds;
    _hintOn = false;
    _usedHint = false;
    _reveal = false;
    _phase = _Phase.answering;
    _banner = '';
    _bannerColor = Colors.transparent;
  }

  void _startTimer() {
    _ticker?.cancel();
    _ticker = Timer.periodic(const Duration(milliseconds: 100), (Timer t) {
      if (_phase != _Phase.answering) return;
      setState(() => _remainingMs -= 100);
      if (_remainingMs <= 0) {
        t.cancel();
        _submit(timeout: true);
      }
    });
  }

  void _tap(String key) {
    if (_phase != _Phase.answering) return;
    unawaited(HapticFeedback.selectionClick());
    setState(() {
      if (key == 'del') {
        final s = _typed.toString();
        _typed.clear();
        if (s.isNotEmpty) _typed.write(s.substring(0, s.length - 1));
      } else if (_typed.length < _puzzle.answer.length) {
        _typed.write(key);
      }
    });
    // typing the last digit submits — nobody wants to hunt for a confirm button
    if (_typed.length == _puzzle.answer.length) unawaited(_submit());
  }

  Future<void> _submit({bool timeout = false}) async {
    if (_phase != _Phase.answering) return;
    _ticker?.cancel();
    final guess = _typed.toString();
    final ok = !timeout && guess == _puzzle.answer;
    final gained = ok
        ? scoreFor(_puzzle, Duration(milliseconds: _remainingMs), _streak, usedHint: _usedHint)
        : 0;

    setState(() {
      _phase = _Phase.feedback;
      _score += gained;
      _streak = ok ? _streak + 1 : 0;
      if (_streak > _bestStreak) _bestStreak = _streak;
      if (ok) {
        _correct++;
      } else {
        _lives--;
        _reveal = true;
      }
      _banner = ok
          ? 'Correct  +$gained'
          : (timeout ? 'Time up — it was ${_puzzle.answer}' : 'It was ${_puzzle.answer}');
      _bannerColor = ok ? const Color(0xFF6FE3A8) : const Color(0xFFFF8A8A);
    });

    unawaited(ok ? HapticFeedback.mediumImpact() : HapticFeedback.heavyImpact());

    await Future<void>.delayed(Duration(milliseconds: ok ? 700 : 1500));
    if (!mounted) return;

    final outOfQuestions = _question + 1 >= widget.roundLength;
    if (_lives <= 0 || outOfQuestions) {
      await _finish();
      return;
    }
    setState(() {
      _question++;
      _level++;
      _deal();
    });
    _startTimer();
  }

  Future<void> _finish() async {
    final previousBest = await Prefs.saveRun(_score);
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute<void>(
        builder: (_) => ResultScreen(
          score: _score,
          previousBest: previousBest,
          correct: _correct,
          total: _question + 1,
          bestStreak: _bestStreak,
          startLevel: widget.startLevel,
          endLevel: _level,
          style: widget.style,
          roundLength: widget.roundLength,
        ),
      ),
    );
  }

  void _useHint() {
    if (_phase != _Phase.answering || _hints <= 0 || _hintOn) return;
    setState(() {
      _hints--;
      _hintOn = true;
      _usedHint = true;
    });
    unawaited(Future<void>.delayed(const Duration(milliseconds: 2600), () {
      if (mounted) setState(() => _hintOn = false);
    }));
  }

  void _skip() {
    if (_phase != _Phase.answering) return;
    unawaited(_submit(timeout: true));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final totalMs = _puzzle.timeLimit.inMilliseconds;
    final frac = totalMs <= 0 ? 0.0 : clamp01(_remainingMs / totalMs);
    final secondsLeft = (_remainingMs / 1000).ceil();

    return PopScope(
      canPop: _phase != _Phase.answering,
      onPopInvokedWithResult: (bool didPop, Object? result) {
        if (didPop) return;
        unawaited(_confirmQuit(context));
      },
      child: Scaffold(
        body: SafeArea(
          child: Column(
            children: <Widget>[
              _hud(theme, secondsLeft, frac),
              Expanded(
                child: Stack(
                  children: <Widget>[
                    Positioned.fill(
                      child: CustomPaint(
                        painter: _style == PlayStyle.dots
                            ? DotPlatePainter(_puzzle, hint: _hintOn, reveal: _reveal)
                            : MazePainter(_puzzle, hint: _hintOn, reveal: _reveal),
                      ),
                    ),
                    if (_banner.isNotEmpty)
                      Align(
                        alignment: Alignment.bottomCenter,
                        child: Padding(
                          padding: const EdgeInsets.only(bottom: 10),
                          child: DefaultTextStyle(
                            style: TextStyle(
                              color: _bannerColor,
                              fontWeight: FontWeight.w800,
                              fontSize: 15,
                              letterSpacing: 0.4,
                              shadows: const <Shadow>[Shadow(color: Colors.black54, blurRadius: 8)],
                            ),
                            child: Text(_banner),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              _answerRow(theme),
              _keypad(context),
              _actions(theme),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _confirmQuit(BuildContext context) async {
    final leave = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Leave the test?'),
        content: Text('Score $_score will be lost.'),
        actions: <Widget>[
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Keep playing')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Leave')),
        ],
      ),
    );
    if (leave == true && mounted) Navigator.of(context).pop();
  }

  Widget _hud(ThemeData theme, int secondsLeft, double frac) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 6, 16, 4),
      child: Column(
        children: <Widget>[
          Row(
            children: <Widget>[
              Text('${_question + 1}/${widget.roundLength}',
                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: Colors.white10,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text('Lvl $_level · ${_puzzle.digitCount} digits',
                    style: theme.textTheme.labelSmall),
              ),
              const Spacer(),
              for (var i = 0; i < _maxLives; i++)
                Icon(
                  Icons.circle,
                  size: 9,
                  color: i < _lives ? const Color(0xFFFF6B6B) : Colors.white24,
                ),
              const SizedBox(width: 12),
              Text('$_score',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF6FE3A8),
                  )),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: <Widget>[
              SizedBox(
                width: 34,
                child: Text('${secondsLeft}s',
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: frac < 0.25 ? const Color(0xFFFF8A8A) : Colors.white54,
                      fontWeight: FontWeight.w700,
                    )),
              ),
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: LinearProgressIndicator(
                    value: frac,
                    minHeight: 5,
                    backgroundColor: Colors.white10,
                    valueColor: AlwaysStoppedAnimation<Color>(
                      frac > 0.5
                          ? const Color(0xFF6FE3A8)
                          : (frac > 0.25 ? const Color(0xFFFFC66B) : const Color(0xFFFF6B6B)),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Text(_style == PlayStyle.dots ? 'DOTS' : 'LINES',
                  style: theme.textTheme.labelSmall?.copyWith(
                    letterSpacing: 2,
                    color: Colors.white24,
                  )),
            ],
          ),
        ],
      ),
    );
  }

  Widget _answerRow(ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.only(top: 6, bottom: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          for (var i = 0; i < _puzzle.answer.length; i++)
            Container(
              width: 40,
              height: 48,
              margin: const EdgeInsets.symmetric(horizontal: 4),
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: _reveal && _typed.toString() != _puzzle.answer
                    ? const Color(0x22FF6B6B)
                    : const Color(0x0FFFFFFF),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(
                  color: _phase == _Phase.answering ? Colors.white24 : _bannerColor,
                ),
              ),
              child: Text(
                _reveal
                    ? _puzzle.answer[i]
                    : (i < _typed.length ? _typed.toString()[i] : ''),
                style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800),
              ),
            ),
        ],
      ),
    );
  }

  Widget _keypad(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(14, 4, 14, 0),
      child: Column(
        children: <Widget>[
          for (final row in <List<String>>[
            <String>['1', '2', '3'],
            <String>['4', '5', '6'],
            <String>['7', '8', '9'],
            <String>['del', '0', 'ok'],
          ])
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 3),
              child: Row(children: <Widget>[for (final key in row) Expanded(child: _Key(key: key, onTap: _tap))]),
            ),
        ],
      ),
    );
  }

  Widget _actions(ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 6, 16, 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          TextButton.icon(
            onPressed: _hints > 0 ? _useHint : null,
            icon: const Icon(Icons.tips_and_updates_outlined, size: 18),
            label: Text('Fade noise ($_hints)'),
          ),
          TextButton(
            onPressed: _phase == _Phase.answering ? _skip : null,
            child: const Text('Reveal & skip'),
          ),
        ],
      ),
    );
  }
}

class _Key extends StatelessWidget {
  const _Key({required this.label, required this.onTap});

  final String label;
  final ValueChanged<String> onTap;

  @override
  Widget build(BuildContext context) {
    final isAction = key == 'del' || key == 'ok';
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 3),
      child: Material(
        color: isAction ? const Color(0x1AFFFFFF) : const Color(0x0DFFFFFF),
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: () => onTap(label),
          child: SizedBox(
            height: 52,
            child: Center(
              child: label == 'del'
                  ? const Icon(Icons.backspace_outlined, size: 20)
                  : (label == 'ok'
                      ? const Icon(Icons.check, size: 22)
                      : Text(label, style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w700))),
            ),
          ),
        ),
      ),
    );
  }
}
