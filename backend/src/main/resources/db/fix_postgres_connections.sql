-- ============================================================================
-- PostgreSQL Connection Troubleshooting & Cleanup Script
-- Run this script in pgAdmin Query Tool or via psql command line
-- ============================================================================

-- 1. Check total current connection count and maximum allowed connections
SELECT 
    count(*) AS current_connections,
    current_setting('max_connections')::int AS max_connections,
    current_setting('superuser_reserved_connections')::int AS superuser_reserved_connections
FROM pg_stat_activity;

-- 2. View connections broken down by database, username, and state
SELECT 
    datname,
    usename,
    state,
    count(*) AS connection_count
FROM pg_stat_activity
GROUP BY datname, usename, state
ORDER BY connection_count DESC;

-- 3. View details of all active or idle connection sessions
SELECT 
    pid,
    usename,
    datname,
    client_addr,
    application_name,
    state,
    state_change,
    query
FROM pg_stat_activity
WHERE datname = 'farm_to_home'
ORDER BY state_change DESC;

-- 4. Terminate all lingering / idle connections for farm_to_home database
--    (Excludes your current pgAdmin query connection)
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'farm_to_home'
  AND pid <> pg_backend_pid();

-- 5. Terminate all idle connections lingering for more than 5 minutes across the server
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
  AND state_change < now() - interval '5 minutes'
  AND pid <> pg_backend_pid();

-- 6. (Optional) Increase max_connections to 200 if PostgreSQL server limit is set too low
-- Note: Requires PostgreSQL service reload/restart to take effect:
-- ALTER SYSTEM SET max_connections = 200;
-- ALTER SYSTEM SET idle_in_transaction_session_timeout = 60000; -- 60 seconds timeout for stuck transactions
-- SELECT pg_reload_conf();
