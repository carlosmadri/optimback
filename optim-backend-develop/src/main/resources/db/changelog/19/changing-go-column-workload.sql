ALTER TABLE public.workload
ALTER COLUMN go TYPE BOOLEAN
USING (CASE
           WHEN go ILIKE 'true' THEN TRUE
           WHEN go ILIKE 'false' THEN FALSE
           ELSE NULL
       END);
