REZON8 BRANDING BACKUP - VERSION 1
==================================
Date: December 22, 2025
Purpose: Backup of original branding assets before redesign

CONTENTS:
---------

1. MIPMAP FOLDERS (All density variants):
   - mipmap-mdpi/
   - mipmap-hdpi/
   - mipmap-xhdpi/
   - mipmap-xxhdpi/
   - mipmap-xxxhdpi/
   - mipmap-anydpi-v26/

   Each folder contains:
   - ic_launcher.png (standard launcher icon)
   - ic_launcher_round.png (round launcher icon variant)
   - ic_launcher_foreground.png (adaptive icon foreground layer)

2. ADAPTIVE ICON XML (mipmap-anydpi-v26/):
   - ic_launcher.xml
   - ic_launcher_round.xml

3. DRAWABLE ASSETS (drawable/):
   - splash_logo.png (splash screen logo)
   - ic_launcher_foreground_new.png (new foreground design)
   - ic_launcher_foreground_padded.xml (padded wrapper for foreground)

RESTORATION INSTRUCTIONS:
-------------------------
To restore these assets, copy the contents of each folder back to their
original locations in app/src/main/res/

NOTE:
-----
The app uses adaptive icons (API 26+) with:
- Background: Black (#000000) - defined in colors.xml
- Foreground: ic_launcher_foreground_padded.xml which references
             ic_launcher_foreground_new.png with 12dp padding on all sides
