ALTER TABLE public.employee
    DROP CONSTRAINT IF EXISTS fk_employee_cost_center;
ALTER TABLE public.workload
    DROP CONSTRAINT IF EXISTS fk_workload_cost_center;
ALTER TABLE public.job_request
    DROP CONSTRAINT IF EXISTS fk_job_request_cost_center;
ALTER TABLE public.lever
    DROP CONSTRAINT IF EXISTS fk_lever_cost_center;

ALTER TABLE public.employee DROP COLUMN IF EXISTS location_id;
ALTER TABLE public.employee ADD COLUMN IF NOT EXISTS cost_center_id bigint;
ALTER TABLE public.employee ADD CONSTRAINT fk_employee_cost_center
    FOREIGN KEY (cost_center_id) REFERENCES public.cost_center (id) ON DELETE SET NULL;

ALTER TABLE public.workload DROP COLUMN IF EXISTS location_id;
ALTER TABLE public.workload ADD COLUMN IF NOT EXISTS cost_center_id bigint;
ALTER TABLE public.workload ADD CONSTRAINT fk_workload_cost_center
    FOREIGN KEY (cost_center_id) REFERENCES public.cost_center (id) ON DELETE SET NULL;

ALTER TABLE public.job_request DROP COLUMN IF EXISTS location_id;
ALTER TABLE public.job_request ADD COLUMN IF NOT EXISTS cost_center_id bigint;
ALTER TABLE public.job_request ADD CONSTRAINT fk_job_request_cost_center
    FOREIGN KEY (cost_center_id) REFERENCES public.cost_center (id) ON DELETE SET NULL;

ALTER TABLE public.lever DROP COLUMN IF EXISTS location_id;
ALTER TABLE public.lever ADD COLUMN IF NOT EXISTS cost_center_id bigint;
ALTER TABLE public.lever ADD CONSTRAINT fk_lever_cost_center
    FOREIGN KEY (cost_center_id) REFERENCES public.cost_center (id) ON DELETE SET NULL;

ALTER TABLE public.workload DROP CONSTRAINT IF EXISTS fk_workload_siglum;
ALTER TABLE public.workload DROP CONSTRAINT IF EXISTS fk_workload_ppsid;

ALTER TABLE public.workload
    ADD CONSTRAINT fk_workload_siglum FOREIGN KEY (siglum_id) REFERENCES public.siglum (id) ON DELETE SET NULL;

ALTER TABLE public.workload
    ADD CONSTRAINT fk_workload_ppsid FOREIGN KEY (ppsid_id) REFERENCES public.ppsid (id) ON DELETE SET NULL;

ALTER TABLE public.workload_evolution DROP CONSTRAINT IF EXISTS fk_siglum;

ALTER TABLE public.workload_evolution
    ADD CONSTRAINT fk_siglum FOREIGN KEY (siglum_id)
    REFERENCES public.siglum (id) ON DELETE SET NULL;

ALTER TABLE public.cost_center DROP CONSTRAINT IF EXISTS fk_cost_center_location;

ALTER TABLE public.cost_center
    ADD CONSTRAINT fk_cost_center_location
    FOREIGN KEY (location_id) REFERENCES public.location (id) ON DELETE SET NULL;
