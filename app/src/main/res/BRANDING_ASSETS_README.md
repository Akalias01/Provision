# REVERIE Branding Assets - Quick Reference

## Asset Inventory

### App Icon (Adaptive Icon - API 26+)

#### Vector Drawables
- `drawable/ic_launcher_background.xml` - Background layer (black with subtle gradient)
- `drawable/ic_launcher_foreground.xml` - Foreground layer (headphones + sound waves)

#### Configuration Files
- `mipmap-anydpi-v26/ic_launcher.xml` - Adaptive icon configuration
- `mipmap-anydpi-v26/ic_launcher_round.xml` - Round adaptive icon configuration

#### Legacy Icons (Pre-API 26)
- All `mipmap-*` folders contain PNG versions:
  - `ic_launcher.png` - Standard launcher icon
  - `ic_launcher_round.png` - Round launcher icon variant
  - `ic_launcher_foreground.png` - Foreground layer for adaptive icon

**Note:** You should regenerate these PNGs from the new vector drawables using Android Studio's Image Asset tool.

---

### Splash Screen

#### Vector Assets
- `drawable/splash_screen_logo.xml` - Large icon for splash (200dp)
- `drawable/splash_screen_text.xml` - REVERIE wordmark with copper accent
- `drawable/splash_screen_combined.xml` - Combined layout (icon + text)

#### Current Configuration
The splash screen is configured in `values/themes.xml`:
```xml
<item name="android:windowSplashScreenBackground">#000000</item>
<item name="android:windowSplashScreenAnimatedIcon">@android:color/transparent</item>
```

**To use new splash screen:** Change the `windowSplashScreenAnimatedIcon` to:
```xml
<item name="android:windowSplashScreenAnimatedIcon">@drawable/splash_screen_logo</item>
```

Or use the combined version:
```xml
<item name="android:windowSplashScreenAnimatedIcon">@drawable/splash_screen_combined</item>
```

---

### Colors

All branding colors are defined in `values/colors.xml`:

```xml
<!-- REVERIE Glass Theme Colors -->
<color name="copper_accent">#CD7F32</color>
<color name="copper_accent_light">#E09850</color>
<color name="copper_accent_dark">#A66528</color>
<color name="glass_dark_bg">#1A1A1A</color>
<color name="glass_darker_bg">#0D0D0D</color>
```

---

## Backup Location

All original branding assets have been backed up to:
```
app/src/main/res/branding_backup_v1/
```

This includes:
- All mipmap folders (all densities)
- Original adaptive icon XMLs
- Original drawable assets (splash_logo.png, etc.)
- README.txt explaining the backup

---

## How to Generate PNG Icons from Vectors

1. Right-click on `res/` folder in Android Studio
2. Select **New > Image Asset**
3. Choose **Foreground Layer** tab
4. Path: Select `drawable/ic_launcher_foreground.xml`
5. Choose **Background Layer** tab
6. Path: Select `drawable/ic_launcher_background.xml`
7. Review previews for all densities and launcher styles
8. Click **Next**, then **Finish**

This will automatically generate all necessary PNG files in the correct mipmap folders.

---

## Design Files Reference

For full design documentation, color usage, implementation guidelines, and design philosophy, see:
```
BRANDING_DESIGN_CONCEPT.md
```
(Located in project root)

---

## Asset Specifications

### App Icon
- **Format:** Vector XML (scalable)
- **Viewport:** 108x108dp
- **Safe zone:** 66dp diameter circle
- **Colors:** Black, White, Copper (#CD7F32)
- **Style:** Glass aesthetic with depth

### Splash Screen Logo
- **Format:** Vector XML (scalable)
- **Viewport:** 200x200dp
- **Colors:** Black, White, Copper (#CD7F32)
- **Style:** Matches app icon, larger scale

### Splash Screen Text
- **Format:** Vector XML (scalable)
- **Viewport:** 240x60dp
- **Text:** "REVERIE" wordmark
- **Accent:** Copper accent with glow effect

---

## Quick Color Reference

| Color Name | Hex Code | Usage |
|------------|----------|-------|
| Black | #000000 | Primary background |
| Glass Dark | #1A1A1A | Secondary surfaces |
| Glass Darker | #0D0D0D | Tertiary depth |
| White | #FFFFFF | Text, icons |
| Copper | #CD7F32 | Primary accent |
| Copper Light | #E09850 | Hover states |
| Copper Dark | #A66528 | Pressed states |

---

## Status

- [x] Backup original assets
- [x] Create vector app icon (foreground + background)
- [x] Update adaptive icon XMLs
- [x] Create splash screen assets
- [x] Define color palette
- [ ] Generate PNG icons from vectors (use Image Asset tool)
- [ ] Test on multiple devices/launchers
- [ ] Apply to in-app UI components

---

Last Updated: December 26, 2025
