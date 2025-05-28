ALTER TABLE public.workload ADD COLUMN location_id BIGINT;
ALTER TABLE public.workload ADD COLUMN siglum_id BIGINT;
ALTER TABLE public.workload ADD COLUMN ppsid_id BIGINT;

ALTER TABLE public.workload
    ADD CONSTRAINT fk_workload_location FOREIGN KEY (location_id) REFERENCES public.location (id) ON DELETE CASCADE;
ALTER TABLE public.workload
    ADD CONSTRAINT fk_workload_siglum FOREIGN KEY (siglum_id) REFERENCES public.siglum (id) ON DELETE CASCADE;
ALTER TABLE public.workload
    ADD CONSTRAINT fk_workload_ppsid FOREIGN KEY (ppsid_id) REFERENCES public.ppsid (id) ON DELETE CASCADE;

ALTER TABLE public.location DROP COLUMN IF EXISTS workload_id;
ALTER TABLE public.siglum DROP COLUMN IF EXISTS workload_id;
ALTER TABLE public.ppsid DROP COLUMN IF EXISTS workload_id;
