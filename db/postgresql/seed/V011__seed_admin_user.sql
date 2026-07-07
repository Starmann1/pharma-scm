-- V011__seed_admin_user.sql
-- Description: Inserts a default admin user. 
-- WARNING: Change the password immediately in production!

INSERT INTO user_master (username, password_hash, full_name, role_id, email, is_active)
SELECT 'admin', 'admin123', 'System Administrator', r.role_id, 'admin@pharma.local', TRUE
FROM role_master r
WHERE r.role_name = 'Admin'
ON CONFLICT (username) DO NOTHING;
