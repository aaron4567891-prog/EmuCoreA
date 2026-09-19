# EmuCoreA

[![Discord](https://img.shields.io/badge/Discord-Join%20the%20server-5865F2?logo=discord&logoColor=white)](https://discord.com/invite/c5EBeNRpz2)
[![Support on Patreon](https://img.shields.io/badge/Patreon-Support%20EmuCore-ff424d?logo=patreon&logoColor=white)](https://www.patreon.com/c/emucore/membership)

EmuCoreA is a PSP emulator and game library for Android. It combines a native Android interface with a vendored [PPSSPP](https://github.com/hrydgard/ppsspp) libretro core. The core is built with the app and does not need a separate download.

The project is under active development. Use your own legally obtained games. PPSSPP emulates the PSP system without a BIOS file.

## Highlights

- Vulkan, OpenGL ES, and software rendering with PSP internal resolution controls
- Game library with PSP title and ID extraction, cover art, search, and per-game settings
- Touch controls with a layout editor and physical gamepad support
- Save states, PSP savedata management, and memory stick size settings
- Cheat and replacement texture catalogs for supported PSP games
- RetroAchievements and optional Discord integration
- Localized interface for phones and tablets

## Supported content

ISO, CSO, and CHD are the main game image formats. PBP, ELF, and PRX support depends on the content and the bundled core. For ISO and PBP, the library can read the title, ID, and icon from PSP metadata. The bundled PSP catalog supplies additional cover art. No games, save data, or account credentials are included here.

## Build

Requirements: Android Studio, JDK 17, Android SDK 37, Android NDK `29.0.14206865`, and CMake `3.30.5`. The app currently targets ARM64 devices running Android 8.0 (API 26) or newer.

1. Open this directory in Android Studio and let Gradle sync.
2. Configure your Android SDK in the untracked `local.properties` file or through your usual SDK environment.
3. Run `./gradlew :app:assembleDebug` (`.\gradlew.bat :app:assembleDebug` on Windows).
4. Install `app/build/outputs/apk/debug/app-debug.apk` on an ARM64 Android device.

The PPSSPP sources are in `core/`, the Android core build is in `core-android/`, and the app and JNI frontend are in `app/`. The game catalog database is bundled at `app/src/main/assets/catalog/games.db`.

### Optional Discord SDK

Discord support is built when a compatible Discord Social SDK directory is supplied through `emucorex.discord.sdkDir` in `local.properties`, a Gradle property with the same name, or `DISCORD_SDK_DIR`. The directory must contain `include/discordpp.h`, `arm64-v8a/libdiscord_partner_sdk.so`, and `discord_partner_sdk.aar`. The SDK is not included in this repository.

## Content catalogs

- [PSP cheats](https://github.com/sashkinbro/EmuCoreA-Cheat)
- [PSP texture packs](https://github.com/sashkinbro/EmuCoreA-Textures)

The catalogs credit the original sources. Redistributable files are mirrored in their respective catalog releases; entries without redistribution permission link to their authors' downloads.

## Credits and license

EmuCoreA builds on PPSSPP and the libretro interface. The root [LICENSE.TXT](LICENSE.TXT) is an exact copy of PPSSPP's upstream license file. The vendored core and its dependencies retain their copyright and license notices in `core/`.

Thanks to the PPSSPP and libretro contributors and to the RetroAchievements team for rcheevos.

EmuCoreA is independent of Sony, PPSSPP, IGDB, Discord, and RetroAchievements. PSP is a trademark of Sony Interactive Entertainment. Game artwork and game data belong to their respective owners.

## Community

- [Discord](https://discord.com/invite/c5EBeNRpz2)
- [Patreon](https://www.patreon.com/c/emucore/membership)
