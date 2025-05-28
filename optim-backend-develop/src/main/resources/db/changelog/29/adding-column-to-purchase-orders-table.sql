ALTER TABLE public.purchase_orders
ADD COLUMN IF NOT EXISTS approved BOOLEAN default false NOT NULL;

ALTER TABLE public.purchase_orders
ADD COLUMN IF NOT EXISTS location_id bigint;

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT fk_purchase_orders_location FOREIGN KEY (location_id) REFERENCES public.location(id) ON DELETE SET NULL;
