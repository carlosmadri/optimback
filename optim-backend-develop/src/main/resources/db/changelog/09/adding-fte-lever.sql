ALTER TABLE public.lever
DROP COLUMN IF EXISTS amount;

ALTER TABLE public.lever
ADD COLUMN IF NOT EXISTS f_te integer;
