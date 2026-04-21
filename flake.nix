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
        ]);

        gradle = pkgs.gradle;
        jdk = pkgs.openjdk17;
        aapt2 = "${androidSdk}/share/android-sdk/build-tools/33.0.0/aapt2";

        nwipe-android = pkgs.stdenv.mkDerivation {
          pname = "nwipe-android";
          version = "0.1";
          src = ./.;

          nativeBuildInputs = [
            gradle
            jdk
            androidSdk
          ];

          buildPhase = ''
            export GRADLE_USER_HOME=$(mktemp -d)
            export ANDROID_HOME=${androidSdk}/share/android-sdk
            
            # Update gradle.properties with the correct aapt2 path
            sed -i "s|android.aapt2FromMavenOverride=.*|android.aapt2FromMavenOverride=${aapt2}|" gradle.properties

            gradle --no-daemon assembleRelease assembleDebug
          '';

          installPhase = ''
            mkdir -p $out
            find app/build/outputs/apk -name "*.apk" -exec cp {} $out/ \;
          '';
        };
      in
      {
        packages.default = nwipe-android;
        devShells.default = pkgs.mkShell {
          buildInputs = [
            gradle
            jdk
            androidSdk
          ];
          shellHook = ''
            export ANDROID_HOME="${androidSdk}/share/android-sdk"
            # Update gradle.properties with the correct aapt2 path in the current directory
            # (only if we want to allow the user to run gradle directly)
            # Actually, it's better to just set it via alias or environment if possible,
            # but gradle.properties is most reliable for daemons.
            sed -i "s|android.aapt2FromMavenOverride=.*|android.aapt2FromMavenOverride=${aapt2}|" gradle.properties
          '';
        };
      }
    );
}
