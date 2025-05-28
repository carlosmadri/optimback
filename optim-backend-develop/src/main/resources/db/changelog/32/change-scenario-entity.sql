ALTER TABLE public.workload DROP COLUMN IF EXISTS scenario;
ALTER TABLE public.ppsid ADD COLUMN IF NOT EXISTS scenario character varying(255);