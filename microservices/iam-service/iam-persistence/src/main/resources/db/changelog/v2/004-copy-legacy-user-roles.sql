INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by)
SELECT ur.user_id, ur.role_id, CURRENT_TIMESTAMP, 'legacy-migration'
FROM users_roles ur
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles current_assignment
    WHERE current_assignment.user_id = ur.user_id
      AND current_assignment.role_id = ur.role_id
      AND current_assignment.revoked_at IS NULL
);
