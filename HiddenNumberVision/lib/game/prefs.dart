import 'package:shared_preferences/shared_preferences.dart';

/// Tiny persistence helper. Every call is wrapped in try/catch, so the game still runs
/// if the plugin is missing (or its platform side was stripped from a build).
class Prefs {
  const Prefs._();

  static const String _kBest = 'netra.best';
  static const String _kGames = 'netra.games';

  static Future<int> best() async {
    try {
      final sp = await SharedPreferences.getInstance();
      return sp.getInt(_kBest) ?? 0;
    } catch (_) {
      return 0;
    }
  }

  /// Returns the score that was stored *before* this run, so the UI can say "new best".
  static Future<int> saveRun(int score) async {
    var previous = 0;
    try {
      final sp = await SharedPreferences.getInstance();
      previous = sp.getInt(_kBest) ?? 0;
      if (score > previous) await sp.setInt(_kBest, score);
      await sp.setInt(_kGames, (sp.getInt(_kGames) ?? 0) + 1);
    } catch (_) {
      // plugin unavailable: the run simply is not remembered
    }
    return previous;
  }
}
