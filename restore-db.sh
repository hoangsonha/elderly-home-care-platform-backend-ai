#!/bin/bash
# Script to restore PostgreSQL database from backup
# Usage: ./restore-db.sh [backup_file.sql.gz]

set -e

BACKUP_DIR="backups"
BACKUP_FILE=""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  Database Restore Script"
echo "=========================================="
echo ""

# Check if backup file is provided as argument
if [ -n "$1" ]; then
    BACKUP_FILE="$1"
    # If path doesn't start with /, assume it's in backups folder
    if [[ ! "$BACKUP_FILE" =~ ^/ ]]; then
        BACKUP_FILE="$BACKUP_DIR/$BACKUP_FILE"
    fi
else
    # List available backups
    echo "Available backups in $BACKUP_DIR:"
    echo ""
    if [ ! -d "$BACKUP_DIR" ] || [ -z "$(ls -A $BACKUP_DIR/*.sql.gz 2>/dev/null)" ]; then
        echo -e "${RED}❌ No backups found in $BACKUP_DIR${NC}"
        exit 1
    fi
    
    backups=($(ls -t $BACKUP_DIR/*.sql.gz 2>/dev/null))
    count=1
    for backup in "${backups[@]}"; do
        filename=$(basename "$backup")
        size=$(du -h "$backup" | cut -f1)
        date=$(stat -c %y "$backup" 2>/dev/null || stat -f %Sm "$backup" 2>/dev/null || echo "N/A")
        echo "  $count. $filename ($size) - $date"
        ((count++))
    done
    echo ""
    read -p "Enter backup number to restore (or q to quit): " choice
    
    if [ "$choice" = "q" ] || [ "$choice" = "Q" ]; then
        echo "Restore cancelled."
        exit 0
    fi
    
    if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le "${#backups[@]}" ]; then
        BACKUP_FILE="${backups[$((choice-1))]}"
    else
        echo -e "${RED}❌ Invalid choice${NC}"
        exit 1
    fi
fi

# Check if backup file exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}❌ Backup file not found: $BACKUP_FILE${NC}"
    exit 1
fi

echo -e "${YELLOW}⚠️  WARNING: This will replace ALL current database data!${NC}"
echo "Backup file: $BACKUP_FILE"
echo "Database: elderly_platform"
echo ""
read -p "Are you sure you want to restore? Type 'yes' to continue: " confirm

if [ "$confirm" != "yes" ]; then
    echo "Restore cancelled."
    exit 0
fi

# Check if database container is running
if ! docker ps | grep -q elderly_db; then
    echo -e "${RED}❌ Database container (elderly_db) is not running!${NC}"
    echo "Start containers first with: docker-compose up -d"
    exit 1
fi

echo ""
echo "Starting restore process..."

# Decompress if needed
if [[ "$BACKUP_FILE" == *.gz ]]; then
    echo "Decompressing backup file..."
    TEMP_FILE=$(mktemp)
    gunzip -c "$BACKUP_FILE" > "$TEMP_FILE"
    SQL_FILE="$TEMP_FILE"
else
    SQL_FILE="$BACKUP_FILE"
    TEMP_FILE=""
fi

# Restore database
echo "Restoring database..."
if docker exec -i elderly_db psql -U postgres -d postgres -c "DROP DATABASE IF EXISTS elderly_platform;" && \
   docker exec -i elderly_db psql -U postgres -d postgres -c "CREATE DATABASE elderly_platform;" && \
   docker exec -i elderly_db psql -U postgres -d elderly_platform < "$SQL_FILE"; then
    echo -e "${GREEN}✅ Database restored successfully!${NC}"
else
    echo -e "${RED}❌ Restore failed!${NC}"
    if [ -n "$TEMP_FILE" ]; then
        rm -f "$TEMP_FILE"
    fi
    exit 1
fi

# Cleanup temp file
if [ -n "$TEMP_FILE" ]; then
    rm -f "$TEMP_FILE"
fi

echo ""
echo -e "${GREEN}✅ Restore completed!${NC}"
echo "You may need to restart backend container for changes to take effect."
