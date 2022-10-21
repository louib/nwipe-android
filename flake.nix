{
  description = "Nix flake for nwipe-android development";

  inputs = {
    nixpkgs = {
      url = "github:NixOS/nixpkgs";
    };
    flake-utils = {
      url = "github:numtide/flake-utils";
    };
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }: (
    flake-utils.lib.eachDefaultSystem (
      system: (
        let
          pkgs = nixpkgs.legacyPackages.${system};
          devPkgs = with pkgs; [
            gradle
          ];
        in {
          devShell = pkgs.mkShell {
            buildInputs = devPkgs;
            nativeBuildInputs = devPkgs;
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
