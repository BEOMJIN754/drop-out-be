-- Existing local ADMIN_USERNAMES=admin is migrated once. Roles are now stored in users.
BEGIN;
ALTER TABLE users ADD COLUMN role varchar(20) NOT NULL DEFAULT 'USER';
UPDATE users SET role='ADMIN' WHERE username='admin';
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK(role IN ('USER','ADMIN'));
UPDATE brands SET category = CASE category
    WHEN 'Streetwear' THEN 'STREETWEAR'
    WHEN 'Skate / Streetwear' THEN 'SKATE_STREETWEAR'
    WHEN 'Technical / Streetwear' THEN 'TECHNICAL_STREETWEAR'
    WHEN 'Casual / Collaboration' THEN 'CASUAL_COLLABORATION'
    WHEN 'Sportswear' THEN 'SPORTSWEAR'
    WHEN 'Contemporary' THEN 'CONTEMPORARY'
    ELSE category END;
UPDATE brands SET category='OTHER' WHERE category IS NULL;
ALTER TABLE brands ALTER COLUMN category SET DEFAULT 'OTHER';
ALTER TABLE brands ALTER COLUMN category SET NOT NULL;
ALTER TABLE brands ADD CONSTRAINT brands_category_check CHECK(category IN
    ('STREETWEAR','SKATE_STREETWEAR','TECHNICAL_STREETWEAR','CASUAL_COLLABORATION','SPORTSWEAR','CONTEMPORARY','OTHER'));
ALTER TABLE brand_requests ADD CONSTRAINT brand_requests_status_check CHECK(status IN ('PENDING','APPROVED','REJECTED'));
COMMIT;
