# Monolog + Copilot setup

This repository includes MCP configuration for the [Monolog research logger](https://monolog.work/research.html).

## Local key setup

Store your Monolog participant key locally instead of committing it to the repo.

- macOS/Linux: `~/.ai-research-api-key`
- Windows: `%USERPROFILE%\.ai-research-api-key`

The file should contain your participant key.

## MCP setup

The repo-local MCP server config is in:

- `.vscode/mcp.json`

It sets:

- `MONOLOG_PROJECT=proj-scaffold`

## Copilot instructions

Use the text in `MONOLOG-COPILOT-INSTRUCTIONS.md` in the appropriate Copilot instruction location for your VS Code/Copilot setup.

## Final step

Reload/restart VS Code after setup.