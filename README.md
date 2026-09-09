# Neutron

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%204.0%2B-green.svg)](https://developer.android.com)

**Neutron** is a modern, high-performance J2ME (Java 2 Micro Edition) emulator for Android developed by **nsomatrix**. It allows you to run classic 2D and 3D mobile Java applications and games (MIDlets) with advanced graphical enhancements, flexible controls, and broad device compatibility.

---

## Features

- **Rich Graphics Support**: Full support for 2D and 3D games, including hardware-accelerated OpenGL ES rendering, M3G (JSR-184), and Mascot Capsule 3D.
- **Customizable Virtual Controls**: Fully configurable on-screen touch keyboard, customizable key layouts, button sizing, opacity, haptic feedback, and external controller support.
- **Per-Application Profiles**: Configure individual display resolution, orientation, scaling filters, shaders, and key mappings for each game.
- **Post-Processing Shaders**: Custom shader filters for CRT, scanlines, smoothing, and LCD display effects.
- **Modern Storage Integration**: Full support for Scoped Storage, Storage Access Framework (SAF), and document provider integration.
- **MIDlet Converter**: Built-in DEX translation engine (`dexlib`) for on-the-fly JAR conversion.

---

## Building

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 34)
- Android NDK (r22+)

### Command Line
Clone the repository and build the debug APK using the included Gradle wrapper:

```bash
git clone git@github.com:nsomatrix/neutron-android.git
cd neutron-android
./gradlew assembleOpenDebug
```

The compiled APK will be generated under `app/build/outputs/apk/`.

---

## Project Structure

```
neutron-android/
├── app/                  # Main Android application module (com.nsomatrix.neutron)
│   ├── src/main/java/    # Kotlin/Java sources (Neutron app & emulated MIDP APIs)
│   ├── src/main/cpp/     # Native C/C++ rendering engines (m3g, micro3d)
│   └── src/main/res/     # UI layouts, resources, and themes
├── dexlib/               # DEX bytecode manipulation & conversion library
└── gradle/               # Gradle wrapper configuration
```

---

## Acknowledgments & Upstream Heritage

Neutron is built upon the incredible open-source heritage of:
- **[J2ME-Loader](https://github.com/nikita36078/J2ME-Loader)** by Nikita Shakarun
- **[JL-Mod](https://github.com/woesss/JL-Mod)** by Yury Kharchenko (woesss)
- **[J2meLoader](https://github.com/NaikSoftware/J2meLoader)** by Nickolay Savchenko
- **[MicroEmulator](https://github.com/bartekt/microemulator)** by Bartek Teodorczyk

---

## License

Neutron is distributed under the [Apache License, Version 2.0](LICENSE).
See the `LICENSE` file and `app/src/main/assets/licenses.html` for full copyright and third-party license information.
