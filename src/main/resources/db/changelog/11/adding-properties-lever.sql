ALTER TABLE IF EXISTS public.lever
    ADD COLUMN IF NOT EXISTS direct character varying(255);

ALTER TABLE IF EXISTS public.lever
    ADD COLUMN IF NOT EXISTS active_workforce character varying(255);

ALTER TABLE IF EXISTS public.lever
    ADD COLUMN IF NOT EXISTS location_id bigint;

ALTER TABLE IF EXISTS public.lever
    ADD CONSTRAINT fk_location FOREIGN KEY (location_id) REFERENCES public.location(id);
