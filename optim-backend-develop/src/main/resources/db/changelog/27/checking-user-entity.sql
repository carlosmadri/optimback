ALTER TABLE public.optim_user
DROP CONSTRAINT IF EXISTS fk_optim_user_siglum;

ALTER TABLE public.optim_user ADD COLUMN IF NOT EXISTS siglum_id BIGINT;

ALTER TABLE public.optim_user
ADD CONSTRAINT fk_optim_user_siglum
FOREIGN KEY (siglum_id) REFERENCES siglum(id);

ALTER TABLE public.optim_user DROP COLUMN IF EXISTS password_hash;
ALTER TABLE public.optim_user DROP COLUMN IF EXISTS lang_key;
ALTER TABLE public.optim_user DROP COLUMN IF EXISTS image_url;
ALTER TABLE public.optim_user DROP COLUMN IF EXISTS activation_key;
ALTER TABLE public.optim_user DROP COLUMN IF EXISTS reset_key;
ALTER TABLE public.optim_user DROP COLUMN IF EXISTS reset_date;
