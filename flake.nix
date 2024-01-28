{
  description = "Nix flake for nwipe-android development";

  inputs = {
    flake-utils = {
      url = "github:numtide/flake-utils";
    };
    nixpkgs = {
      url = "github:NixOS/nixpkgs?ref=65f1d643032cb47e9ca92c5c7d82c1d6f51b5969";
    };
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs";

      # The main branch follows the "canary" channel of the Android SDK
      # repository. Use another android-nixpkgs branch to explicitly
      # track an SDK release channel.
      #
      # url = "github:tadfisher/android-nixpkgs/stable";
      # url = "github:tadfisher/android-nixpkgs/beta";
      # url = "github:tadfisher/android-nixpkgs/preview";
      # url = "github:tadfisher/android-nixpkgs/canary";

      inputs.nixpkgs.follows = "nixpkgs";
      inputs.flake-utils.follows = "flake-utils";
    };
  };

  outputs = {
    self,
    nixpkgs,
    android-nixpkgs,
    flake-utils,
  }: (
    flake-utils.lib.eachDefaultSystem (
      system: (
        let
          pkgs = import nixpkgs {
            inherit system;
            config = {
              android_sdk.accept_license = true;
              allowUnfree = true;
            };
          };
          devPkgs = [
            pkgs.gradle_6
            pkgs.openjdk8
            pkgs.androidsdk_9_0
            pkgs.android-studio
          ];
          androidPackage = (
            android-nixpkgs.sdk.${system} (
              sdkPkgs:
                with sdkPkgs; [
                  cmdline-tools-latest
                  build-tools-33-0-0
                  platform-tools
                  platforms-android-33
                  emulator
                ]
            )
          );
        in {
          devShell = pkgs.mkShell {
            buildInputs = devPkgs ++ [androidPackage];
            nativeBuildInputs = devPkgs ++ [androidPackage];
          };
          /*
          defaultPackage = pkgs.androidPlatform.buildAndroidPackage {
            pname = "nwipe-android";
            # TODO fetch the version from the manifest.
            version = "0.0.1";
            buildInputs = devPkgs;
            nativeBuildInputs = devPkgs;
          };
          */
        }
      )
    )
  );
}
