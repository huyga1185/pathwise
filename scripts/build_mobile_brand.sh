#!/usr/bin/env bash
# Rasterises brand/logo into the driver app (docs/client/ARCHITECTURE.md §8: logo and icons come from brand/logo,
# never edited inside mobile/): the white mark for the login header, Android launcher icons (legacy and adaptive)
# and the iOS AppIcon set. Needs rsvg-convert (librsvg). The PNGs are committed; run again when the logo changes.
set -euo pipefail
cd "$(dirname "$0")/.."

LOGO=brand/logo
RES=mobile/android/app/src/main/res
IOS=mobile/ios/Runner/Assets.xcassets/AppIcon.appiconset
render() { rsvg-convert -w "$2" -h "$2" "$1" -o "$3"; }

# White mark: 40 logical px high in the login header (108 × 102 viewBox), 1x/2x/3x.
mkdir -p mobile/assets/brand/2.0x mobile/assets/brand/3.0x
rsvg-convert -h 40 "$LOGO/pathwise-mark-white.svg" -o mobile/assets/brand/pathwise-mark-white.png
rsvg-convert -h 80 "$LOGO/pathwise-mark-white.svg" -o mobile/assets/brand/2.0x/pathwise-mark-white.png
rsvg-convert -h 120 "$LOGO/pathwise-mark-white.svg" -o mobile/assets/brand/3.0x/pathwise-mark-white.png

# Android: legacy icon (48 dp) and adaptive foreground (108 dp) per density; the background is a colour resource.
for pair in mdpi:1 hdpi:1.5 xhdpi:2 xxhdpi:3 xxxhdpi:4; do
  density=${pair%%:*}; scale=${pair##*:}
  mkdir -p "$RES/mipmap-$density"
  render "$LOGO/app-icon.svg" "$(python3 -c "print(round(48*$scale))")" "$RES/mipmap-$density/ic_launcher.png"
  render "$LOGO/app-icon-foreground.svg" "$(python3 -c "print(round(108*$scale))")" "$RES/mipmap-$density/ic_launcher_foreground.png"
done

# iOS: every size of the AppIcon set (opaque, the square gradient of app-icon.svg).
for file in "$IOS"/Icon-App-*.png; do
  name=$(basename "$file" .png)                # Icon-App-83.5x83.5@2x
  size=${name#Icon-App-}; size=${size%%x*}      # 83.5
  scale=${name##*@}; scale=${scale%x}           # 2
  render "$LOGO/app-icon.svg" "$(python3 -c "print(round($size*$scale))")" "$file"
done
echo "brand assets written from $LOGO"
