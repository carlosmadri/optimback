ALTER TABLE public.job_request DROP CONSTRAINT IF EXISTS fk_location_id;
ALTER TABLE public.job_request DROP CONSTRAINT IF EXISTS fk_siglum_id;

ALTER TABLE public.job_request
ADD COLUMN IF NOT EXISTS active_workforce character varying(255),
ADD COLUMN IF NOT EXISTS worker_type character varying(255),
ADD COLUMN IF NOT EXISTS on_top_hc_target boolean,
ADD COLUMN IF NOT EXISTS externalised boolean,
ADD COLUMN IF NOT EXISTS early_career boolean,
ADD COLUMN IF NOT EXISTS critical_job boolean,
ADD COLUMN IF NOT EXISTS approved_qmc boolean,
ADD COLUMN IF NOT EXISTS approved_shrbph1q boolean,
ADD COLUMN IF NOT EXISTS approved_hocoohohrcoo boolean,
ADD COLUMN IF NOT EXISTS approved_employment_commitee boolean,
ADD COLUMN IF NOT EXISTS location_id bigint,
ADD COLUMN IF NOT EXISTS siglum_id bigint,
DROP CONSTRAINT IF EXISTS fk_location_id,
DROP CONSTRAINT IF EXISTS fk_siglum_id,
ADD CONSTRAINT fk_location_id FOREIGN KEY (location_id) REFERENCES public.location (id),
ADD CONSTRAINT fk_siglum_id FOREIGN KEY (siglum_id) REFERENCES public.siglum (id);

ALTER TABLE public.siglum DROP CONSTRAINT IF EXISTS fk_job_request_id;

ALTER TABLE public.siglum
DROP COLUMN IF EXISTS job_request_id,
ADD COLUMN IF NOT EXISTS job_request_id bigint,
DROP CONSTRAINT IF EXISTS fk_job_request_id,
ADD CONSTRAINT fk_job_request_id FOREIGN KEY (job_request_id) REFERENCES public.job_request (id);

ALTER TABLE public.location DROP CONSTRAINT IF EXISTS fk_job_request_id;

ALTER TABLE public.location
DROP COLUMN IF EXISTS job_request_id,
ADD COLUMN IF NOT EXISTS job_request_id bigint,
DROP CONSTRAINT IF EXISTS fk_job_request_id,
ADD CONSTRAINT fk_job_request_id FOREIGN KEY (job_request_id) REFERENCES public.job_request (id);