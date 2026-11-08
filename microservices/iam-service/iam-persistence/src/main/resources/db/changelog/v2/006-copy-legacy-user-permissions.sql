INSERT INTO user_perms (user_id, permission_id, effect, assigned_at, assigned_by)
SELECT up.user_id, up.permission_id, 'GRANT', CURRENT_TIMESTAMP, 'legacy-migration'
FROM users_permissions up
WHERE NOT EXISTS (
    SELECT 1 FROM user_perms current_assignment
    WHERE current_assignment.user_id = up.user_id
      AND current_assignment.permission_id = up.permission_id
      AND current_assignment.revoked_at IS NULL
);
