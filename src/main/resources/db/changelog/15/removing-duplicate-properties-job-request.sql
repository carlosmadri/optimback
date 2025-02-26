ALTER TABLE public.job_request
    DROP COLUMN IF EXISTS f_te,
    DROP COLUMN IF EXISTS on_top_hc_target,
    DROP COLUMN IF EXISTS externalised,
    DROP COLUMN IF EXISTS critical_job;