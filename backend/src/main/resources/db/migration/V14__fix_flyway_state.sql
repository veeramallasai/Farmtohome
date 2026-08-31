-- V14__fix_flyway_state.sql
-- Clean up the failed V12 migration record so it can run again
DELETE FROM flyway_schema_history 
WHERE script = 'V12__categories_variants_wishlist_reviews_notifications_activity.sql'
  AND success = false;
