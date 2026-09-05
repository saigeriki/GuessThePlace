# SciCalc — Professional Scientific Calculator (Android · Kotlin · Jetpack Compose)

A modern, production-style **Scientific Calculator** built completely with **Kotlin** and **Jetpack Compose**.

It replaces the earlier `GuessThePlace` quiz project in this repository. Everything lives in the `:app` module.

---

## ✨ Features

### Basic calculator
- Addition `+`, subtraction `−`, multiplication `×`, division `÷`
- Decimal numbers
- Parentheses `( )`
- `%` (percentage / divide by 100)
- `±` (negate the current entry)
- Backspace and full clear (`AC`)
- Real-time result preview while you type

### Scientific functions
- Trigonometric: `sin`, `cos`, `tan`, `asin`, `acos`, `atan`
- Hyperbolic: `sinh`, `cosh`, `tanh`
- Logarithms: `ln`, `log` (base 10), `log₂`
- Powers and roots: `x²`, `x³`, `xʸ`, `eˣ`, `√`, `∛`, `ⁿ√`
- Factorial `x!` (supports non-integers via Gamma function)
- Reciprocal `1/x`
- Absolute value `|x|`
- Modulo `mod(a,b)` and generic `pow(a,b)` (use the `,` key to separate arguments)
- Constants `π` and `e`
- Angle modes: **DEG / RAD / GRAD**
- Implicit multiplication: `2(3+4)`, `2π`, `2sin(30)`

### Memory
- `MC` – memory clear
- `MR` – memory recall
- `M+` – add current value
- `M−` – subtract current value
- `MS` – store current value
- A green `M` indicator shows when memory is in use

### UX / Professional touches
- Clean Material 3 design with a **dark theme** and a **light theme**
- Manual theme toggle (🌙 / ☀️) in the top bar; default follows the system
- History strip above the display (last 6 calculations)
- Human-friendly number formatting (no `1.000000` style noise; supports scientific notation for very large/small numbers)
- Selections allowed on expression & result (long-press can copy)

---

## 🧰 Project structure

```
app/src/main/java/com/example/scientificcalculator/
├── MainActivity.kt                       # Composable entry point
├── calc/
│   └── CalculatorEngine.kt               # Tokenizer + parser + evaluator + formatter
└── ui/
    ├── CalculatorApp.kt                  # Full calculator UI + keypad + state
    └── theme/
        ├── Color.kt
        ├── Type.kt
        └── Theme.kt
```

- `CalculatorEngine.kt` is pure Kotlin (no Android dependencies), so it can be unit-tested easily.
- `CalculatorApp.kt` is the single-screen Compose app.
- `MainActivity.kt` wires Compose to Android.

---

## 🚀 Step-by-step: run it in Android Studio

### Step 1 — Open the project
1. Open **Android Studio** (Ladybug or newer recommended).
2. **File → Open** and select the root folder `GuessThePlace` (this repo).
3. Let Gradle sync finish. The project uses:
   - Kotlin `2.0.21`
   - Compose BOM `2024.09.00`
   - Material 3
   - AGP `9.0.1`

### Step 2 — Check the Module / SDK
1. Make sure an **Android SDK 36** platform is installed.
2. Open **File → Project Structure → SDK Location** and set `local.properties` or Android Studio’s bundled SDK path.
3. If sync shows a missing SDK, install **Android 36 (Baklava)** from the SDK Manager.

### Step 3 — Build
- **Build → Make Project** (or `./gradlew assembleDebug` in the terminal).

### Step 4 — Run on emulator / device
1. Open **Tools → Device Manager** and create a device (Pixel series works well).
2. Press **Run ▶**.
3. Select `app` configured for `com.example.scientificcalculator` and launch.

### Step 5 — Use it
- Type numbers with the number pad.
- Use the scientific rows for trig, logs, roots, powers, etc.
- Switch **DEG / RAD / GRAD** above the display.
- Use `M+`/`M−`/`MS`/`MR`/`MC` for memory.
- Press `=` to lock in a result; the result becomes the next expression so you can keep calculating.

---

## 🧪 Run the unit tests

The math engine is covered by unit tests:

```bash
./gradlew testDebugUnitTest
```

or from Android Studio: right-click `CalculatorEngineTest` → **Run**.

---

## 🎨 About the calculator expression engine

The engine is a small recursive-descent parser:

```
expression → term  ((+|-) term)*
term       → unary ((*|/) unary)*        # also handles implicit multiplication
unary      → (-|+) unary | power
power      → postfix (^ unary)           # right associative
postfix    → primary (!|%)*
primary    → number | π | e | (expression) | function(args)
```

Domain errors (e.g. `sqrt(-1)`, `ln(-1)`, `sin(90)`-type tangent singularities, division by zero) are reported cleanly on screen.

---

## 📌 Notes
- The project was renamed from `GuessThePlaceIndia` to **SciCalc** (`com.example.scientificcalculator`).
- Old quiz images, layouts, and activities were removed.
- All maths files (`Color.kt`, `Theme.kt`, `Type.kt`) use the Scientific Calculator theme; app is fully dark/light adaptive.
