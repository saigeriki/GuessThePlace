/// A tiny 5x7 pixel font.
///
/// Both puzzle styles (dot plate + line maze) are painted from these bitmaps, so the two
/// modes always hide exactly the same number and the difficulty curve stays comparable.
/// No image assets, no font files — the whole puzzle is generated at runtime.
class DigitFont {
  const DigitFont._();

  static const int cols = 5;
  static const int rows = 7;

  static const Map<String, List<String>> glyphs = <String, List<String>>{
    '0': <String>['01110', '10001', '10011', '10101', '11001', '10001', '01110'],
    '1': <String>['00100', '01100', '00100', '00100', '00100', '00100', '01110'],
    '2': <String>['01110', '10001', '00001', '00010', '00100', '01000', '11111'],
    '3': <String>['11111', '00010', '00100', '00010', '00001', '10001', '01110'],
    '4': <String>['00010', '00110', '01010', '10010', '11111', '00010', '00010'],
    '5': <String>['11111', '10000', '11110', '00001', '00001', '10001', '01110'],
    '6': <String>['00110', '01000', '10000', '11110', '10001', '10001', '01110'],
    '7': <String>['11111', '00001', '00010', '00100', '01000', '01000', '01000'],
    '8': <String>['01110', '10001', '10001', '01110', '10001', '10001', '01110'],
    '9': <String>['01110', '10001', '10001', '01111', '00001', '00010', '01100'],
  };

  static List<String> of(String digit) => glyphs[digit] ?? glyphs['0']!;
}
