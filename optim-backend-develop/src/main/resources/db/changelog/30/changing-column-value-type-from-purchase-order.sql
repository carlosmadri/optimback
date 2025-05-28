ALTER TABLE public.purchase_orders
DROP COLUMN approved;

ALTER TABLE public.purchase_orders
ADD COLUMN IF NOT EXISTS approved character varying(255) NOT NULL DEFAULT ('false');