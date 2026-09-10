{
  description = "Project development tools";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  outputs = { nixpkgs, ... }: {
    devShells = nixpkgs.lib.genAttrs [ "aarch64-darwin" "aarch64-linux" "x86_64-linux" ] (system:
      let pkgs = import nixpkgs { inherit system; config.allowUnfree = true; };
      in { default = pkgs.mkShell {
        packages = with pkgs; [ nodejs_24 pnpm_11 temurin-bin-25 awscli2 ssm-session-manager-plugin terraform ];
        JAVA_HOME = "${pkgs.temurin-bin-25}/Contents/Home";
      }; });
  };
}
