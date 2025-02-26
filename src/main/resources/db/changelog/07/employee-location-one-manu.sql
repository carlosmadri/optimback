ALTER TABLE public.employee
ADD COLUMN IF NOT EXISTS location_id bigint;

ALTER TABLE public.employee
DROP CONSTRAINT IF EXISTS fk_employee_location;

ALTER TABLE public.employee
ADD CONSTRAINT fk_employee_location
FOREIGN KEY (location_id)
REFERENCES public.location(id)
ON DELETE SET NULL;

ALTER TABLE public.location
DROP COLUMN IF EXISTS employee_id;
