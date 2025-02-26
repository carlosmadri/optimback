ALTER TABLE public.job_request
    ADD COLUMN IF NOT EXISTS direct character varying(255);

ALTER TABLE public.job_request
    DROP COLUMN IF EXISTS end_date;

ALTER TABLE public.job_request
    ADD COLUMN IF NOT EXISTS release_date timestamp without time zone;

ALTER TABLE public.job_request
    DROP COLUMN IF EXISTS worker_type;

ALTER TABLE public.job_request
    ADD COLUMN IF NOT EXISTS kapis_code character varying(10),
    ADD COLUMN IF NOT EXISTS collar character varying(10);

ALTER TABLE public.job_request
    ADD COLUMN IF NOT EXISTS cost_center_id bigint;

ALTER TABLE public.job_request
    DROP CONSTRAINT IF EXISTS fk_cost_center,
    ADD CONSTRAINT fk_cost_center FOREIGN KEY (cost_center_id) REFERENCES public.cost_center (id);
