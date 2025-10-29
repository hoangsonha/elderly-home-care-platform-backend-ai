#!/bin/bash
# Script to check SSH setup on VPS
# Run this on VPS: bash check-ssh-setup.sh

echo "=========================================="
echo "SSH Setup Check on VPS"
echo "=========================================="
echo ""

echo "1. Checking .ssh directory..."
if [ -d ~/.ssh ]; then
  echo "   ✅ .ssh directory exists"
  ls -la ~/.ssh/
else
  echo "   ❌ .ssh directory NOT found!"
  exit 1
fi
echo ""

echo "2. Checking authorized_keys file..."
if [ -f ~/.ssh/authorized_keys ]; then
  echo "   ✅ authorized_keys file exists"
  FILE_SIZE=$(wc -l < ~/.ssh/authorized_keys)
  echo "   📊 Number of keys: $FILE_SIZE"
  echo ""
  echo "   First few lines of authorized_keys:"
  head -3 ~/.ssh/authorized_keys
  echo "   ..."
else
  echo "   ❌ authorized_keys file NOT found!"
fi
echo ""

echo "3. Checking permissions..."
if [ -d ~/.ssh ]; then
  PERM_SSH=$(stat -c "%a" ~/.ssh 2>/dev/null || stat -f "%OLp" ~/.ssh 2>/dev/null)
  echo "   .ssh permissions: $PERM_SSH (should be 700)"
  if [ "$PERM_SSH" = "700" ]; then
    echo "   ✅ Permission correct"
  else
    echo "   ⚠️  Permission should be 700, fixing..."
    chmod 700 ~/.ssh
  fi
fi

if [ -f ~/.ssh/authorized_keys ]; then
  PERM_KEY=$(stat -c "%a" ~/.ssh/authorized_keys 2>/dev/null || stat -f "%OLp" ~/.ssh/authorized_keys 2>/dev/null)
  echo "   authorized_keys permissions: $PERM_KEY (should be 600)"
  if [ "$PERM_KEY" = "600" ]; then
    echo "   ✅ Permission correct"
  else
    echo "   ⚠️  Permission should be 600, fixing..."
    chmod 600 ~/.ssh/authorized_keys
  fi
fi
echo ""

echo "4. Current user:"
echo "   $(whoami)"
echo ""

echo "5. SSH service status:"
if command -v systemctl &> /dev/null; then
  systemctl status sshd --no-pager -l 2>/dev/null | head -5 || echo "   Cannot check SSH service"
else
  echo "   systemctl not available"
fi
echo ""

echo "=========================================="
echo "Check completed!"
echo ""
echo "If authorized_keys is empty or missing,"
echo "you need to add your public key again."
echo "=========================================="

