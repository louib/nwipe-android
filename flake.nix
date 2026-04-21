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
          ];

          buildPhase = ''
            export GRADLE_USER_HOME=$(mktemp -d)
            export ANDROID_HOME=${androidSdk}/share/android-sdk
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
            gradle-wrapped
            jdk
            androidSdk
          ];
          shellHook = ''
            export ANDROID_HOME="${androidSdk}/share/android-sdk"
            # Ensure GRADLE_OPTS also carries the override for any other way gradle might be called
            export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=${aapt2}"
          '';
        };
      }
    );
}
