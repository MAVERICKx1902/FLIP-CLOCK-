# Liquid Prism Clock

A premium, fullscreen **liquid-glass flip clock** for Android. Translucent glass
digits, prismatic light, OLED-friendly near-black backgrounds, floating light
fragments, and a physics-inspired *liquid fold* flip animation — built entirely
with **Jetpack Compose + Canvas**.

```
                    🌙 Night

              0 9 : 4 2 : 1 7

            Monday, September 7, 2026
```

Each digit is its own animated glass card. When a digit changes, *only that card*
melts and folds into the next number while a subtle ripple passes through its
neighbors and a tiny burst of refracted light sparks off it.

---

## Build & run

Requires **Android Studio (Koala / 2024.2 or newer)** with the Android SDK
installed, or a Gradle 8.9 + JDK 17 toolchain on the command line.

```bash
# 1. Open the project root in Android Studio, then press Run, or:
./gradlew :app:assembleDebug      # build the debug APK
./gradlew :app:installDebug       # install on a connected device/emulator
```

- **Min SDK:** 26 (Android 8.0) · **Target/Compile SDK:** 34 (Android 14)
- **Kotlin:** 2.0.21 · **Compose BOM:** 2024.10.01 · **JVM target:** 17

The first build downloads Gradle 8.9 and the AndroidX/Compose dependencies from
`google()` / `mavenCentral()`. No special SDK manager packages beyond the
platform (android-34) and build-tools are needed.

---

## What's inside

### Architecture (UI → ViewModel → StateFlow → state)

```
com.example.liquidprismclock
├── MainActivity.kt              edge-to-edge, fullscreen, keep-screen-on flags
├── data/PreferencesRepository   DataStore persistence (all settings)
├── model/
│   ├── TimeData.kt              real system clock snapshot + digit slots
│   ├── PrismPalette.kt          PrismPalette + 4 time-of-day palettes, smooth lerp
│   ├── Settings.kt / TimeOfDay.kt / Intensity / PerformanceTier
├── animation/
│   ├── LiquidFlipAnimation.kt   4-phase liquid fold timeline (pure, testable)
│   ├── LiquidEasing.kt          spring settle, ripple wave, gooey eases
│   └── ParticlePool.kt          allocation-free, pre-allocated particle pool
├── viewmodel/FlipClockViewModel.kt   StateFlow of Settings / TimeData / palette
└── ui/
    ├── ClockScreen.kt           hero screen + controls + tap-to-reveal
    ├── SettingsScreen.kt        Material 3 grouped settings
    ├── components/
    │   ├── LiquidDigit.kt       one glass digit card (the liquid flip)
    │   ├── FlipClock.kt         adaptive 6-digit row + colons
    │   ├── PrismBackground.kt   drifting blobs, glow pool, grain, particles
    │   ├── ParticleSystem.kt    additive glass-dust rendering
    │   ├── GlassButton.kt       floating glass control
    │   ├── FrameLoop.kt         withFrameNanos shared frame clock
    │   └── PerfOverlay.kt       real FPS meter + debug info
    └── theme/                   Material 3 dark scheme, type, colors
```

### The liquid flip (per digit)

Driven by a single shared `withFrameNanos` clock (frame-synchronized — never a
blind `delay(16)`), evaluated against the wall-clock timestamp of the change:

| Phase | Time | Effect |
|-------|------|--------|
| **1 Pre-flip** | 0–100 ms | glow swell, glass highlight intensifies, chromatic hint |
| **2 Liquid flip** | 100–400 ms | old digit folds down, stretches, skews, fades with a glass trail; new digit rises and settles |
| **3 Settle** | 400–500 ms | under-damped spring overshoot, glow eases off |
| **4 Ripple** | 500–650 ms | damped wave propagates to adjacent cards |

Chromatic aberration is a faint red/blue RGB split during the fold; it always
returns to zero so text stays readable.

### Prism color system

`PrismPalette(primary, secondary, accent, background, glow)` blends continuously
against the fractional hour — there are **no hard switches** at 06:00/12:00/…;
themes cross-fade over a smooth transition window and wrap cleanly across
midnight. Backgrounds stay near-black for OLED. Manual themes (Morning /
Afternoon / Evening / Night) or fully Automatic.

### Performance

- **No per-frame allocation** in the render hot paths: `Paint`, `Path`, and
  unit-space `Shader` objects are reused and re-pointed via `Matrix` transforms
  and cached color filters.
- Continuous animation runs in the **draw phase only** (frame state read inside
  `Canvas` lambdas), so it never recomposes the tree.
- Particles use a fixed pre-allocated pool with object reuse; counts scale with
  a measured `PerformanceTier` (cores/memory) and the user's intensity setting.
- Blur is avoided entirely (soft glow is done with cheap radial gradients);
  grain is a cached 128×128 tile, skipped on low-tier GPUs.
- The time loop wakes roughly once per second (aligned to the real boundary),
  so idle CPU/GPU is minimal.
- The **FPS counter** measures real frame deltas over a rolling window — it is
  never a constant.

> Note: "Keep screen awake" adds `FLAG_KEEP_SCREEN_ON` while the clock Activity
> is visible. Regular apps cannot replace the system Always-On Display, so this
> is deliberately an in-app screen-on mode, not a system AOD.

### Settings (persisted via DataStore)

- **Clock:** 12/24-hour, show seconds, show date, show time-of-day
- **Appearance:** automatic/manual prism theme, animation/particle/glow intensity
- **Display:** fullscreen (immersive), keep screen awake
- **Developer:** real FPS counter, animation debug overlay

Fullscreen uses the modern `WindowInsetsControllerCompat` immersive APIs (not
the deprecated system-ui flags) and works with gesture navigation.

---

## Verified in this workspace

Because the sandboxed build environment cannot reach Google's Maven (AndroidX
AARs) or Gradle distribution servers, the project was validated as far as that
allows:

- ✅ **60/60 core-logic tests pass** (real Kotlin 2.4.10 compiler): time-of-day
  quadrants, `59→00`, `09:59→10:00`, `23:59→00:00`, `12:59 PM→1 PM`, 12/24h
  digits/spoken time, continuous (no-jump) palette interpolation incl. midnight
  wrap, the full 4-phase flip timeline, ripple attenuation, and the bounded
  particle pool over hundreds of frames.
- ✅ **All 83 Kotlin classes compile** with the Kotlin 2.4.10 compiler against
  the Android 14 framework `android.jar` (plus API-compatible Compose/AndroidX
  stubs) — zero unresolved references or type errors.
- ✅ **All resources + manifest link** with `aapt2` (`compile` + `link`) against
  API 34: strings, themes, adaptive launcher icons, and backup rules resolve;
  badging reports the package, launchable activity, and SDK levels correctly.

In an environment with network access to `google()`/`mavenCentral()`,
`./gradlew :app:assembleDebug` produces the APK directly.
