# nwipe-android
![Android CI badge](https://github.com/louib/nwipe-android/workflows/android_ci/badge.svg)

[nwipe-android](https://github.com/louib/nwipe-android) is an application to wipe the internal storage of an Android device.

**This app is still beta software.**

## Screenshots

<img src="./screenshots/start.png" alt="start" width="30%" height="30%"/><img src="./screenshots/wiping.png" alt="wiping" width="30%" height="30%"/><img src="./screenshots/success.png" alt="success" width="30%" height="30%"/>

## Build with Nix

This project includes a Nix flake for reproducible builds without requiring Android Studio.

### Prerequisites

- [Nix](https://nixos.org/download.html) with [Flakes enabled](https://nixos.wiki/wiki/Flakes).

### Build steps

1.  **Enter the development shell:**

    ```bash
    nix develop
    ```

    The shell will automatically configure `ANDROID_HOME` and update `gradle.properties` with the correct path to the Nix-provided `aapt2`.

2.  **Build the application:**

    ```bash
    gradle assembleRelease
    ```

    Or as a one-liner:

    ```bash
    nix develop .# -c gradle assembleRelease
    ```

The resulting APK will be at `app/build/outputs/apk/release/app-release-unsigned.apk`.

## License

GPL-3.0
