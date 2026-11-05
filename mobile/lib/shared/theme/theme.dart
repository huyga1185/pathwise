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
    surface: t.surface,
    onSurface: t.ink,
    surfaceContainerHighest: t.surfaceRaised,
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
    focusColor: t.route.withValues(alpha: 0.24),
  );
}
