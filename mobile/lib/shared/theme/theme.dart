import 'package:flutter/material.dart';

import 'tokens.g.dart';

/// The design tokens of the current theme (P-42): `context.pw.route`, `context.pw.inkMuted`…
extension PwThemeContext on BuildContext {
  PwColorTokens get pw => Theme.of(this).extension<PwColors>()!.tokens;
}

/// Carries [PwColorTokens] in [ThemeData] so widgets read the token of the active theme.
class PwColors extends ThemeExtension<PwColors> {
  const PwColors(this.tokens);

  final PwColorTokens tokens;

  @override
  PwColors copyWith({PwColorTokens? tokens}) => PwColors(tokens ?? this.tokens);

  @override
  PwColors lerp(covariant PwColors? other, double t) => t < 0.5 || other == null ? this : other;
}

/// Light and dark Material themes built only from the tokens (ARCHITECTURE §8): no colour or size written here.
ThemeData pwTheme(Brightness brightness) {
  final t = brightness == Brightness.dark ? PwColorTokens.dark : PwColorTokens.light;
  final scheme = ColorScheme(
    brightness: brightness,
    primary: t.route,
    onPrimary: t.onAccent,
    secondary: t.route,
    onSecondary: t.onAccent,
    error: t.danger,
    onError: t.onAccent,
    // Material 3 roles filled from the tokens (P-77): tonal buttons, selected items and the next stop sit on
    // surface-tint with ink; cards, sheets and dialogs on surface-raised; snackbars on the inverse of the page.
    primaryContainer: t.surfaceTint,
    onPrimaryContainer: t.ink,
    secondaryContainer: t.surfaceTint,
    onSecondaryContainer: t.ink,
    tertiary: t.info,
    onTertiary: t.onAccent,
    surface: t.surface,
    onSurface: t.ink,
    onSurfaceVariant: t.inkMuted,
    surfaceContainerLowest: t.surfaceRaised,
    surfaceContainerLow: t.surfaceRaised,
    surfaceContainer: t.surfaceRaised,
    surfaceContainerHigh: t.surfaceRaised,
    surfaceContainerHighest: t.surfaceRaised,
    inverseSurface: t.ink,
    onInverseSurface: t.surface,
    outline: t.inkMuted,
    outlineVariant: t.line,
  );
  final text = TextTheme(
    headlineMedium: PwTypeStyles.display.copyWith(color: t.ink),
    titleLarge: PwTypeStyles.title.copyWith(color: t.ink),
    titleMedium: PwTypeStyles.heading.copyWith(color: t.ink),
    bodyLarge: PwTypeStyles.bodyTouch.copyWith(color: t.ink),
    bodyMedium: PwTypeStyles.body.copyWith(color: t.ink),
    bodySmall: PwTypeStyles.small.copyWith(color: t.inkMuted),
    labelLarge: PwTypeStyles.heading.copyWith(color: t.ink),
    labelSmall: PwTypeStyles.label.copyWith(color: t.inkMuted),
  );
  return ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: scheme,
    scaffoldBackgroundColor: t.surface,
    fontFamily: 'Be Vietnam Pro',
    textTheme: text,
    extensions: [PwColors(t)],
    // In the theme, not passed when the sheet opens: a colour passed then stays the old theme's when the user switches
    // light/dark from inside the sheet (the account sheet).
    bottomSheetTheme: BottomSheetThemeData(
      backgroundColor: t.surfaceRaised,
      modalBackgroundColor: t.surfaceRaised,
      showDragHandle: true,
    ),
    // Floating, above the bottom of the screen and the result buttons (M3: a brief notification at the bottom).
    snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
    focusColor: t.route.withValues(alpha: 0.24),
  );
}
