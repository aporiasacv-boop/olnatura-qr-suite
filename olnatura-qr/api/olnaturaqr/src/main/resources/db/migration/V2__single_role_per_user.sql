-- 1. Agregar role_id a users
ALTER TABLE users
ADD COLUMN role_id UUID;

-- 2. Asignar rol si existía en user_roles (primer rol)
UPDATE users u
SET role_id = ur.role_id
FROM user_roles ur
WHERE ur.user_id = u.id;

-- 3. Hacerlo obligatorio
ALTER TABLE users
ALTER COLUMN role_id SET NOT NULL;

-- 4. FK al catálogo de roles
ALTER TABLE users
ADD CONSTRAINT fk_users_role
FOREIGN KEY (role_id)
REFERENCES roles(id)
ON DELETE RESTRICT;

-- 5. Eliminar tabla intermedia
DROP TABLE user_roles;