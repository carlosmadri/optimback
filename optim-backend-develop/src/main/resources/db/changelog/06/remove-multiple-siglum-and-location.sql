ALTER TABLE public.employee
ADD COLUMN IF NOT EXISTS siglum_id bigint,
ADD COLUMN IF NOT EXISTS location_id bigint;

ALTER TABLE public.employee
ADD CONSTRAINT fk_employee_siglum FOREIGN KEY (siglum_id) REFERENCES public.siglum(id) ON DELETE SET NULL;

ALTER TABLE public.employee
ADD CONSTRAINT fk_employee_location FOREIGN KEY (location_id) REFERENCES public.location(id) ON DELETE SET NULL;
