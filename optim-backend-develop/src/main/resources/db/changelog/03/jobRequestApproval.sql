DROP TABLE IF EXISTS public.job_request_approval CASCADE;

DROP SEQUENCE IF EXISTS public.job_request_approval_id_seq CASCADE;

CREATE SEQUENCE public.job_request_approval_id_seq;

CREATE TABLE public.job_request_approval (
    id bigint DEFAULT nextval('public.job_request_approval_id_seq') NOT NULL,
    employee_id bigint NOT NULL,
    job_request_id bigint NOT NULL,
    approval_status character varying(255) NOT NULL,
    approval_date timestamp without time zone,
    CONSTRAINT job_request_approval_pkey PRIMARY KEY (id)
);
