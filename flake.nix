{
  description = "Nix flake for nwipe-android";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, flake-utils, android-nixpkgs }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            android_sdk.accept_license = true;
            allowUnfree = true;
          };
        };

        androidSdk = android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
          cmdline-tools-latest
          build-tools-33-0-0
          platform-tools
          platforms-android-33
          emulator
          system-images-android-33-google-apis-x86-64
        ]);

        gradle = pkgs.gradle;
        jdk = pkgs.openjdk17;
        google-java-format = pkgs.google-java-format;
        aapt2 = "${androidSdk}/share/android-sdk/build-tools/33.0.0/aapt2";

        # Create a wrapper for gradle that always includes the aapt2 override
        gradle-wrapped = pkgs.writeShellScriptBin "gradle" ''
          exec ${gradle}/bin/gradle -Pandroid.aapt2FromMavenOverride=${aapt2} "$@"
        '';

        nwipe-android = pkgs.stdenv.mkDerivation {
          pname = "nwipe-android";
          version = "0.1";
          src = ./.;

          nativeBuildInputs = [
            gradle-wrapped
            jdk
            androidSdk
            google-java-format
          ];

          buildPhase = ''
            export GRADLE_USER_HOME=$(mktemp -d)
            export ANDROID_HOME=${androidSdk}/share/android-sdk
            # Build APKs (default) and Bundles
            gradle --no-daemon assembleRelease assembleDebug bundleRelease
          '';

          installPhase = ''
            mkdir -p $out/apks $out/bundles
            find app/build/outputs/apk -name "*.apk" -exec cp {} $out/apks/ \;
            find app/build/outputs/bundle -name "*.aab" -exec cp {} $out/bundles/ \;
          '';
        };
      in
      {
        packages.default = nwipe-android;
        devShells.default = pkgs.mkShell {
          buildInputs = [
            gradle-wrapped
            jdk
            androidSdk
            google-java-format
          ];
          shellHook = ''
            export ANDROID_HOME="${androidSdk}/share/android-sdk"
            export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=${aapt2}"
            
            alias format='google-java-format --replace $(find . -path "*/build" -prune -o -name "*.java" -print)'
            alias run='gradle assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.example.nwipe_android/.MainActivity'
            alias emu-create='avdmanager create avd -n nwipe -k "system-images;android-33;google_apis;x86_64"'
            alias emu='emulator -avd nwipe'
            
            # New commands for AAB and Signing
            alias bundle='gradle bundleRelease'
            alias sign-help='echo "To sign, run: gradle assembleRelease -Pandroid.injected.signing.store.file=/path/to/keystore.jks -Pandroid.injected.signing.store.password=pass -Pandroid.injected.signing.key.alias=alias -Pandroid.injected.signing.key.password=pass"'

            echo "Nix Android Dev Shell"
            echo "---------------------"
            echo "run       : Build/Install/Launch APK"
            echo "bundle    : Create App Bundle (.aab)"
            echo "sign-help : Show how to sign APKs/AABs"
            echo "format    : Format Java code"
            echo "emu       : Launch emulator"
          '';
        };
      }
    );
}
