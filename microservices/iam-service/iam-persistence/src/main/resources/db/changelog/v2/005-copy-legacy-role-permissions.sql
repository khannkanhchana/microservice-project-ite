INSERT INTO role_perms (role_id, permission_id, assigned_at, assigned_by)
SELECT rp.role_id, rp.permission_id, CURRENT_TIMESTAMP, 'legacy-migration'
FROM roles_permissions rp
WHERE NOT EXISTS (
    SELECT 1 FROM role_perms current_assignment
    WHERE current_assignment.role_id = rp.role_id
      AND current_assignment.permission_id = rp.permission_id
      AND current_assignment.revoked_at IS NULL
);
