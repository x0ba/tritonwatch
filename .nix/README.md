# Development environment

The project toolchain is pinned in `flake.lock`. Home Manager supplies direnv and nix-direnv.
Run `direnv allow` at the repository root once, then enter the directory to load it automatically.
For a command without direnv: `nix develop path:./.nix --command <command>`.
Update with `nix flake update --flake path:./.nix`.

Tools: nodejs_24, pnpm_11, temurin-bin-25, awscli2, ssm-session-manager-plugin, terraform.
