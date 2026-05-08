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

nvm install $NODE_VERSION
nvm use $NODE_VERSION


