# Netra — Hidden Number Vision Test (Flutter)

A simple, addictive Android game with **no image assets at all**:

> a plate is generated — a maze of coloured dots (or a tangle of lines) — and one number is
> hidden inside it. Find the number, type it, beat the timer.

Everything is drawn at runtime by two `CustomPainter`s from a 5×7 glyph bitmap, so the whole
game is ~600 lines of Dart, an APK under 20 MB, and it starts instantly.

This is the project I would build **after** porting `GuessThePlace` — it is the same shape of
app (quiz + timer + score) but the content is generated instead of hand-made, so there is an
endless supply of levels.

```
lib/
  game/
    digit_font.dart      5x7 pixel bitmaps for 0-9 (the only "art" in the app)
    puzzle.dart          generator: mask -> dots + line segments + palette + timer   <- the brain
    prefs.dart           best score (shared_preferences, fully optional)
  render/
    puzzle_painters.dart DotPlatePainter (ishihara style) + MazePainter (lines style) <- the eyes
  ui/
    home_screen.dart     style / round length / start level
    game_screen.dart     timer, keypad, lives, hints, feedback                       <- the loop
    result_screen.dart   score, accuracy, advice
  main.dart
test/
  puzzle_test.dart       geometry invariants (run: flutter test)
prototype/
  vision_core.js         the same generator in JS — reference implementation + test target
  index.html             PLAYABLE version, for tuning difficulty in a browser
  test.js / perf.js      node tests for the generator (383k assertions)
```

## Run it

```bash
# inside this folder: it only has dart code, so let flutter add the android wrapper
flutter create --platforms=android --org com.example.netra --project-name hidden_number_vision .
flutter pub get
flutter run                      # 'r' = hot reload, 'R' = restart

flutter build apk --release      # build/app/outputs/flutter-apk/app-release.apk
```

`flutter create .` only fills in the missing platform folders. If it rewrites `pubspec.yaml`
(`name:` or `flutter: uses-material-design`), restore this folder's copy — the rest is untouched.

Play with the idea first (no toolchain needed): open `prototype/index.html` in a browser.
That page uses the same generator and the same difficulty table, so you can drag the level
slider to decide what "too easy / too hard" means, then move those two numbers into
`lib/game/puzzle.dart`.

## How the difficulty works

`PuzzleGenerator.create(level:question:)` derives everything from one number:

| knob | level 1 | level 10+ | why |
|---|---|---|---|
| digits | 1 | 3 | `1 + (level-1)~/3`, capped at 3 |
| hue gap | 80° | 22° | near-isochromatic plates at high level |
| sat / val gap | 0.40 / 0.28 | 0.05 / 0.04 | same trick, softer |
| background dots | 44% of plate capacity | +70 | denser noise |
| decoy line segments | 220 | 500 | the lines style gets a real maze |
| time limit | 14.5 s | 8.5 s floor | pressure, not panic |

Two rules that matter, and that the tests enforce:

1. **The glyph grid is sized by its diagonal** (`cell = 2·R·0.86 / √(cols²+rows²)`), so no dot
   or stroke ever pokes outside the plate disc. Sizing it by width looks fine for `1` and
   breaks for `8`.
2. **Contrast is only cut down to a luminance floor (50 → 26 out of 255).** If you hide the
   number by hue alone, two colours of equal brightness are invisible to *everyone*, and the
   game stops being a game.

## Worth knowing (the Flutter bits)

* `CustomPaint` + `CustomPainter` = Android's `Canvas`/`onDraw`. The painter receives a `Size`,
  the puzzle stores **normalised 0..1 coordinates**, so rotation and split-screen cost nothing.
* The puzzle is generated **once** per question, in the state object — never in `paint()`.
  Painting 700 circles per frame is fine; regenerating them per frame is not.
* `shouldRepaint` compares puzzle + `hint`/`reveal` flags — that is the difference between 60 fps
  and a hot phone.
* Generation cost: ~4 ms per plate (measured, `prototype/perf.js`) — no isolate needed.
* No `setState` in `paint`, no plugins needed to play, `shared_preferences` only remembers a score.

## 5 upgrades, easiest first

1. **Sound + haptics** — `HapticFeedback` is already wired in `game_screen.dart`; add
   `audioplayers` for a tick and a "found it" chime.
2. **Share the plate** — `RepaintBoundary` + `RenderRepaintBoundary.toImage()` to export the
   puzzle as a PNG, `share_plus` to send it (without the answer, obviously).
3. **Daily plate** — seed the generator with `DateTime.now().toUtc().day` instead of
   `level*977 + question*131`, and everybody in the world sees the same puzzle that day.
4. **Real colour-blindness modes** — deuteran/protan simulation is a 3×3 matrix on the palette;
   a "simulate" toggle in the painter makes it an actual vision-test app.
5. **More shapes** — `DigitFont` is the only place shapes live. Add letters → "hidden word",
   add country silhouettes (you already have 17 monument photos for a *Guess the Place* mash-up).

## Notes

* Fun/brain-training only — **not** a medical eye test. Say so in the store listing too.
* `test/puzzle_test.dart` mirrors `prototype/test.js`; when you change the generator, run both.
