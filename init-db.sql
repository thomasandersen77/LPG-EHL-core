-- LPG-EHL Database Schema
-- Initial database setup for pump transaction tracking

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Transactions table - Master record of all fuel deliveries
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_number BIGSERIAL NOT NULL UNIQUE,
    dispenser_address INTEGER NOT NULL,
    
    -- Timestamps
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Delivery data
    volume_litres NUMERIC(10, 3) NOT NULL DEFAULT 0,  -- 3 decimal precision
    amount_cents INTEGER NOT NULL DEFAULT 0,          -- Amount in øre
    price_per_litre_cents INTEGER NOT NULL,           -- Price at time of transaction
    
    -- State tracking
    state VARCHAR(50) NOT NULL DEFAULT 'STARTED',     -- STARTED, DELIVERING, FINISHED, CANCELLED
    
    -- Azure sync tracking
    synced_to_azure BOOLEAN NOT NULL DEFAULT FALSE,
    synced_at TIMESTAMP,
    azure_sync_attempts INTEGER NOT NULL DEFAULT 0,
    last_sync_error TEXT,
    
    -- Indexes
    CONSTRAINT positive_volume CHECK (volume_litres >= 0),
    CONSTRAINT positive_amount CHECK (amount_cents >= 0),
    CONSTRAINT positive_price CHECK (price_per_litre_cents > 0),
    CONSTRAINT valid_dispenser CHECK (dispenser_address BETWEEN 1 AND 255)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_transactions_started_at ON transactions(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_state ON transactions(state);
CREATE INDEX IF NOT EXISTS idx_transactions_synced ON transactions(synced_to_azure, synced_at) WHERE NOT synced_to_azure;
CREATE INDEX IF NOT EXISTS idx_transactions_dispenser ON transactions(dispenser_address);

-- Protocol events table - Detailed EHL protocol communication log
CREATE TABLE IF NOT EXISTS protocol_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID REFERENCES transactions(id) ON DELETE CASCADE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Event details
    direction VARCHAR(20) NOT NULL,                   -- SEND, RECEIVE
    dispenser_address INTEGER NOT NULL,
    command_code INTEGER NOT NULL,
    command_name VARCHAR(50) NOT NULL,
    
    -- Packet data
    raw_bytes BYTEA,                                  -- Raw protocol bytes
    data_payload BYTEA,                               -- Data portion only
    checksum_valid BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Decoded data (JSON for flexibility)
    decoded_data JSONB,
    
    -- State at time of event
    dispenser_state VARCHAR(50),
    
    CONSTRAINT valid_direction CHECK (direction IN ('SEND', 'RECEIVE')),
    CONSTRAINT valid_dispenser_event CHECK (dispenser_address BETWEEN 1 AND 255)
);

-- Indexes for protocol events
CREATE INDEX IF NOT EXISTS idx_protocol_events_transaction ON protocol_events(transaction_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_protocol_events_timestamp ON protocol_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_protocol_events_command ON protocol_events(command_name);

-- System events table - Application health and errors
CREATE TABLE IF NOT EXISTS system_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Event classification
    level VARCHAR(20) NOT NULL,                       -- INFO, WARN, ERROR, CRITICAL
    category VARCHAR(50) NOT NULL,                    -- STARTUP, SHUTDOWN, DATABASE, SERIAL_PORT, AZURE_SYNC, etc.
    message TEXT NOT NULL,
    
    -- Context
    component VARCHAR(100),                           -- Which component logged this
    details JSONB,                                    -- Additional context as JSON
    
    -- Error tracking
    error_message TEXT,
    stack_trace TEXT,
    
    CONSTRAINT valid_level CHECK (level IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL'))
);

-- Indexes for system events
CREATE INDEX IF NOT EXISTS idx_system_events_timestamp ON system_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_system_events_level ON system_events(level);
CREATE INDEX IF NOT EXISTS idx_system_events_category ON system_events(category);

-- Dispenser status table - Current state of each dispenser
CREATE TABLE IF NOT EXISTS dispenser_status (
    dispenser_address INTEGER PRIMARY KEY,
    
    -- Current state
    current_state VARCHAR(50) NOT NULL,               -- IDLE, DELIVERING, FINISHED, ERROR, OFFLINE
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Current transaction (if any)
    active_transaction_id UUID REFERENCES transactions(id),
    
    -- Health metrics
    total_transactions INTEGER NOT NULL DEFAULT 0,
    total_volume_litres NUMERIC(12, 3) NOT NULL DEFAULT 0,
    total_amount_cents BIGINT NOT NULL DEFAULT 0,
    
    -- Error tracking
    consecutive_errors INTEGER NOT NULL DEFAULT 0,
    last_error_at TIMESTAMP,
    last_error_message TEXT,
    
    -- Configuration
    configured_price_per_litre_cents INTEGER NOT NULL,
    
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_dispenser_status CHECK (dispenser_address BETWEEN 1 AND 255)
);

-- Azure sync queue table - Outbox pattern for resilient Azure sync
CREATE TABLE IF NOT EXISTS azure_sync_queue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    
    -- Queue management
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP,
    
    -- Error tracking
    last_error TEXT,
    
    -- Payload
    payload JSONB NOT NULL,                           -- Transaction data to sync
    
    CONSTRAINT max_attempts CHECK (attempts <= 100)
);

-- Indexes for sync queue
CREATE INDEX IF NOT EXISTS idx_sync_queue_next_retry ON azure_sync_queue(next_retry_at) WHERE attempts < 100;
CREATE INDEX IF NOT EXISTS idx_sync_queue_transaction ON azure_sync_queue(transaction_id);

-- Daily summary view - For quick reporting
CREATE OR REPLACE VIEW daily_summary AS
SELECT 
    DATE(started_at) as transaction_date,
    dispenser_address,
    COUNT(*) as transaction_count,
    SUM(volume_litres) as total_volume_litres,
    SUM(amount_cents) / 100.0 as total_amount_kr,
    AVG(price_per_litre_cents) / 100.0 as avg_price_per_litre_kr,
    COUNT(CASE WHEN state = 'FINISHED' THEN 1 END) as completed_transactions,
    COUNT(CASE WHEN state = 'CANCELLED' THEN 1 END) as cancelled_transactions
FROM transactions
WHERE started_at IS NOT NULL
GROUP BY DATE(started_at), dispenser_address
ORDER BY transaction_date DESC, dispenser_address;

-- Unsynced transactions view - For Azure sync monitoring
CREATE OR REPLACE VIEW unsynced_transactions AS
SELECT 
    t.id,
    t.transaction_number,
    t.dispenser_address,
    t.started_at,
    t.finished_at,
    t.volume_litres,
    t.amount_cents / 100.0 as amount_kr,
    t.azure_sync_attempts,
    t.last_sync_error,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - t.created_at)) / 3600 as hours_since_creation
FROM transactions t
WHERE t.synced_to_azure = FALSE
  AND t.state = 'FINISHED'
ORDER BY t.created_at;

-- Function to update dispenser status
CREATE OR REPLACE FUNCTION update_dispenser_status()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO dispenser_status (
        dispenser_address,
        current_state,
        total_transactions,
        total_volume_litres,
        total_amount_cents,
        configured_price_per_litre_cents
    )
    VALUES (
        NEW.dispenser_address,
        NEW.state,
        1,
        NEW.volume_litres,
        NEW.amount_cents,
        NEW.price_per_litre_cents
    )
    ON CONFLICT (dispenser_address) DO UPDATE SET
        current_state = EXCLUDED.current_state,
        last_seen_at = CURRENT_TIMESTAMP,
        total_transactions = dispenser_status.total_transactions + 1,
        total_volume_litres = dispenser_status.total_volume_litres + EXCLUDED.total_volume_litres,
        total_amount_cents = dispenser_status.total_amount_cents + EXCLUDED.total_amount_cents,
        updated_at = CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update dispenser status
CREATE TRIGGER trigger_update_dispenser_status
AFTER INSERT OR UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION update_dispenser_status();

-- Function to add to Azure sync queue
CREATE OR REPLACE FUNCTION queue_for_azure_sync()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.state = 'FINISHED' AND NOT NEW.synced_to_azure THEN
        INSERT INTO azure_sync_queue (transaction_id, payload)
        VALUES (
            NEW.id,
            jsonb_build_object(
                'transaction_id', NEW.id,
                'transaction_number', NEW.transaction_number,
                'dispenser_address', NEW.dispenser_address,
                'started_at', NEW.started_at,
                'finished_at', NEW.finished_at,
                'volume_litres', NEW.volume_litres,
                'amount_cents', NEW.amount_cents,
                'price_per_litre_cents', NEW.price_per_litre_cents
            )
        )
        ON CONFLICT (transaction_id) DO NOTHING;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically queue finished transactions
CREATE TRIGGER trigger_queue_for_azure_sync
AFTER INSERT OR UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION queue_for_azure_sync();

-- Insert initial system event
INSERT INTO system_events (level, category, message, component)
VALUES ('INFO', 'STARTUP', 'Database initialized successfully', 'init-db.sql');

-- Grant permissions (if needed for specific user)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO lpg_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO lpg_user;
