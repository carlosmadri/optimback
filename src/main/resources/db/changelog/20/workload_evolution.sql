DROP TABLE IF EXISTS public.workload_evolution CASCADE;

CREATE TABLE public.workload_evolution (
    id SERIAL PRIMARY KEY,
    exercise VARCHAR(255),
    status VARCHAR(255),
    siglum_id BIGINT,
    f_te DOUBLE PRECISION,
    k_hrs_own_direct DOUBLE PRECISION,
    k_hrs_own_indirect DOUBLE PRECISION,
    k_hrs_sub_direct DOUBLE PRECISION,
    k_hrs_sub_indirect DOUBLE PRECISION,
    submit_date TIMESTAMP,
    CONSTRAINT fk_siglum FOREIGN KEY (siglum_id) REFERENCES siglum (id) ON DELETE CASCADE
);