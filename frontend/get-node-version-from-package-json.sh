#!/usr/bin/env bash
# Get the node version from the package.json file

# Check if jq is installed
if ! command -v jq &> /dev/null; then
  NODE_VERSION=$(grep -o '"node": "[^"]*"' package.json | sed 's/[^0-9.]*//g')
else
  NODE_VERSION=$(jq -r '.engines.node' package.json | sed 's/[^0-9.]*//g')
fi  


if [ -z "$NODE_VERSION" ]; then
  echo "Unable to determine node version from package.json"
  exit 1
fi

# 1. Define the NVM directory
export NVM_DIR="$HOME/.nvm"

# 2. Load NVM into this shell session
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

if [ -n "$NVM_BIN" ] && [ "${BASH_SOURCE[0]}" != "$0" ]; then
  nvm install "$NODE_VERSION"
  nvm use "$NODE_VERSION"
else
  echo "This script must be sourced to update the current shell's node version:"
  echo ". ./get-node-version-from-package-json.sh"
  echo
  echo "Or run these commands manually:"
  echo nvm install "$NODE_VERSION"
  echo nvm use "$NODE_VERSION"
fi




