-- Clean up any failed V12 migration entries so fresh migrations can run
DELETE FROM flyway_schema_history 
WHERE script = 'V12__categories_variants_wishlist_reviews_notifications_activity.sql' AND success = false;
