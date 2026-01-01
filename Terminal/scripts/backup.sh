#!/bin/sh
# Automatic PostgreSQL Backup Script
# Runs continuously in Docker container to create hourly backups

set -e

echo "🔄 LPG-EHL Backup Service Starting..."
echo "Database: ${POSTGRES_DB}"
echo "Keep hours: ${BACKUP_KEEP_HOURS:-24}"
echo "Keep days: ${BACKUP_KEEP_DAYS:-7}"

# Wait for PostgreSQL to be ready
until pg_isready -h postgres -U "${POSTGRES_USER}"; do
  echo "⏳ Waiting for PostgreSQL to be ready..."
  sleep 5
done

echo "✅ PostgreSQL is ready. Starting backup loop..."

while true; do
  TIMESTAMP=$(date +%Y%m%d_%H%M%S)
  BACKUP_FILE="/backups/backup_${TIMESTAMP}.sql.gz"
  
  echo ""
  echo "📦 Creating backup: backup_${TIMESTAMP}.sql.gz"
  
  # Create backup
  if pg_dump -h postgres -U "${POSTGRES_USER}" "${POSTGRES_DB}" | gzip > "${BACKUP_FILE}"; then
    BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    echo "✅ Backup completed successfully (${BACKUP_SIZE})"
    
    # Create a 'latest' symlink
    ln -sf "backup_${TIMESTAMP}.sql.gz" /backups/latest.sql.gz
    
    # Clean up old hourly backups (keep last 24 hours)
    echo "🧹 Cleaning hourly backups older than ${BACKUP_KEEP_HOURS:-24} hours..."
    find /backups -name "backup_*.sql.gz" -type f -mmin +$((${BACKUP_KEEP_HOURS:-24} * 60)) -delete
    
    # Keep one backup per day for the last week
    echo "🧹 Keeping daily backups for last ${BACKUP_KEEP_DAYS:-7} days..."
    find /backups -name "backup_*.sql.gz" -type f -mtime +${BACKUP_KEEP_DAYS:-7} -delete
    
    # Show backup statistics
    BACKUP_COUNT=$(find /backups -name "backup_*.sql.gz" -type f | wc -l)
    TOTAL_SIZE=$(du -sh /backups | cut -f1)
    echo "📊 Total backups: ${BACKUP_COUNT}, Total size: ${TOTAL_SIZE}"
    
  else
    echo "❌ Backup failed!"
  fi
  
  # Wait 1 hour before next backup
  echo "⏰ Next backup in 1 hour..."
  sleep 3600
done
