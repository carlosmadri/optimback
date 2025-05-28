ALTER TABLE public.employee
ADD COLUMN IF NOT EXISTS siglum_id bigint;

ALTER TABLE public.employee
DROP CONSTRAINT IF EXISTS fk_employee_siglum;

ALTER TABLE public.employee
ADD CONSTRAINT fk_employee_siglum
FOREIGN KEY (siglum_id)
REFERENCES public.siglum(id)
ON DELETE SET NULL;

ALTER TABLE public.siglum
DROP COLUMN IF EXISTS employee_id;
