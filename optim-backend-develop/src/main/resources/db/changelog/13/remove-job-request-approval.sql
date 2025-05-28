ALTER TABLE IF EXISTS public.job_request_approval
    DROP CONSTRAINT IF EXISTS fk_employee;

ALTER TABLE IF EXISTS public.job_request_approval
    DROP CONSTRAINT IF EXISTS fk_job_request;

ALTER TABLE IF EXISTS public.job_request_approval
    DROP CONSTRAINT IF EXISTS job_request_approval_pkey;

DROP TABLE IF EXISTS public.job_request_approval;
