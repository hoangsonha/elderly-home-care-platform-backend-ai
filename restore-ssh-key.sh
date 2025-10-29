#!/bin/bash
# Script to restore SSH authorized_keys from backup
# Usage: ./restore-ssh-key.sh

BACKUP_FILE="$HOME/elderly-home-care-platform-backend-ai/.ssh_authorized_keys_backup"
SSH_KEY_FILE="$HOME/.ssh/authorized_keys"

echo "=========================================="
echo "  Restore SSH authorized_keys"
echo "=========================================="
echo ""

if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    echo "✅ Backup file found: $BACKUP_FILE"
    echo ""
    echo "Backup contents:"
    cat "$BACKUP_FILE"
    echo ""
    read -p "Restore this key to ~/.ssh/authorized_keys? (y/N): " confirm
    
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        # Ensure .ssh directory exists
        mkdir -p ~/.ssh
        
        # Restore from backup
        cp "$BACKUP_FILE" "$SSH_KEY_FILE"
        
        # Set correct permissions
        chmod 700 ~/.ssh
        chmod 600 "$SSH_KEY_FILE"
        
        echo ""
        echo "✅ SSH key restored successfully!"
        echo ""
        echo "Current authorized_keys:"
        cat "$SSH_KEY_FILE"
    else
        echo "Restore cancelled."
    fi
else
    echo "❌ Backup file not found: $BACKUP_FILE"
    echo ""
    echo "Please add SSH key manually:"
    echo "  echo 'YOUR_PUBLIC_KEY' > ~/.ssh/authorized_keys"
    echo "  chmod 600 ~/.ssh/authorized_keys"
fi

