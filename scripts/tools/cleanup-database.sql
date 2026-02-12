-- ═══════════════════════════════════════════════════════════════════════
-- Database Cleanup Script
-- ═══════════════════════════════════════════════════════════════════════
--
-- This script cleans up stuck transactions and authorizations that may
-- prevent the application from working correctly.
--
-- Usage in H2 Console:
--   1. Start H2 console: ./start-h2-console.sh
--   2. Connect to database
--   3. Run this script
--
-- ═══════════════════════════════════════════════════════════════════════

-- Show current state
SELECT '=== CURRENT STATE ===' AS info;

-- Active authorizations
SELECT 'Active Authorizations:' AS info;
SELECT * FROM pump_authorization WHERE status IN ('PENDING', 'PUMPING', 'AUTHORIZED');

-- Pending transactions
SELECT 'Pending Transactions:' AS info;
SELECT * FROM transactions WHERE status IN ('STARTED', 'PENDING');

-- ═══════════════════════════════════════════════════════════════════════
-- CLEANUP ACTIONS (uncomment to execute)
-- ═══════════════════════════════════════════════════════════════════════

-- Option 1: Mark all stuck authorizations as EXPIRED
-- UPDATE pump_authorization 
-- SET status = 'EXPIRED', 
--     updated_at = CURRENT_TIMESTAMP 
-- WHERE status IN ('PENDING', 'PUMPING', 'AUTHORIZED');

-- Option 2: Delete all stuck authorizations
-- DELETE FROM pump_authorization WHERE status IN ('PENDING', 'PUMPING', 'AUTHORIZED');

-- Option 3: Mark all pending transactions as FAILED
-- UPDATE transactions 
-- SET status = 'FAILED',
--     updated_at = CURRENT_TIMESTAMP
-- WHERE status IN ('STARTED', 'PENDING');

-- ═══════════════════════════════════════════════════════════════════════
-- COMPLETE RESET (DANGER: removes all data!)
-- ═══════════════════════════════════════════════════════════════════════

-- Uncomment to delete ALL data from all tables:
-- DELETE FROM pump_authorization;
-- DELETE FROM transactions;
-- DELETE FROM price_history;
-- DELETE FROM road_tax_settings;

-- Show final state
SELECT '=== AFTER CLEANUP ===' AS info;
SELECT * FROM pump_authorization;
SELECT * FROM transactions;
