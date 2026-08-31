-- Delete the failed V12 record so it can be retried
DELETE FROM flyway_schema_history 
WHERE script = 'V12__categories_variants_wishlist_reviews_notifications_activity.sql' 
  AND success = false;
