ALTER TABLE IF EXISTS public.lever
DROP CONSTRAINT IF EXISTS fk_employee;

ALTER TABLE IF EXISTS public.lever
ADD CONSTRAINT fk_employee
FOREIGN KEY (employee_id)
REFERENCES public.employee (id)
ON DELETE SET NULL;

ALTER TABLE IF EXISTS public.lever
DROP CONSTRAINT IF EXISTS uq_lever_employee;
