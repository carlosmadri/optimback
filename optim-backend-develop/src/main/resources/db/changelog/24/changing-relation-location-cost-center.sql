ALTER TABLE public.cost_center
ADD COLUMN IF NOT EXISTS location_id bigint;

ALTER TABLE public.cost_center
    DROP CONSTRAINT IF EXISTS fk_cost_center_location;

ALTER TABLE public.cost_center
ADD CONSTRAINT fk_cost_center_location
FOREIGN KEY (location_id) REFERENCES public.location(id)
ON DELETE CASCADE;

ALTER TABLE public.location
DROP COLUMN IF EXISTS cost_center_id;
