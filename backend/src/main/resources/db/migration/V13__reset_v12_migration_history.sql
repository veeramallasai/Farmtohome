-- Reset V12 migration so it can run again with the fix
DELETE FROM flyway_schema_history 
WHERE script = 'V12__categories_variants_wishlist_reviews_notifications_activity.sql';
