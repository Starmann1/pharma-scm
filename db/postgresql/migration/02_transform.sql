-- 02_transform.sql
-- Description: Transforms and maps MySQL data to PostgreSQL schema if using staging tables.
-- Alternatively, if transforming CSV directly, this script provides instructions/templates.

-- Example: Assuming data was loaded into a temporary staging schema `mysql_staging`.
-- Here we demonstrate how to cast booleans and rename columns.

-- 1. Transform booleans (MySQL 0/1 to PostgreSQL FALSE/TRUE)
/*
UPDATE mysql_staging.User_Master 
SET is_active_pg = CASE WHEN is_active = 1 THEN TRUE ELSE FALSE END;

UPDATE mysql_staging.Material_Master
SET is_active_pg = CASE WHEN is_active = 1 THEN TRUE ELSE FALSE END;
*/

-- 2. Transform casing and map to public schema
/*
INSERT INTO public.user_master (user_id, username, password_hash, full_name, role_id, is_active, created_at, updated_at)
SELECT user_id, username, password_hash, full_name, role_id, is_active_pg, created_at, updated_at
FROM mysql_staging.User_Master;
*/

-- (This file acts as a placeholder or template for specific data cleansing rules 
-- required for the user's specific MySQL data anomalies)
