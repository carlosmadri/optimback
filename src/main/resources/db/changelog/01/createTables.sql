-- Drop Statements --

DROP TABLE IF EXISTS public.cost_center CASCADE;
DROP TABLE IF EXISTS public.employee CASCADE;
DROP TABLE IF EXISTS public.head_count CASCADE;
DROP TABLE IF EXISTS public.job_request CASCADE;
DROP TABLE IF EXISTS public.lever CASCADE;
DROP TABLE IF EXISTS public.location CASCADE;
DROP TABLE IF EXISTS public.optim_user CASCADE;
DROP TABLE IF EXISTS public.ppsid CASCADE;
DROP TABLE IF EXISTS public.purchase_orders CASCADE;
DROP TABLE IF EXISTS public.siglum CASCADE;
DROP TABLE IF EXISTS public.workload CASCADE;

DROP SEQUENCE IF EXISTS public.cost_center_id_seq;
DROP SEQUENCE IF EXISTS public.employee_id_seq;
DROP SEQUENCE IF EXISTS public.head_count_id_seq;
DROP SEQUENCE IF EXISTS public.job_request_id_seq;
DROP SEQUENCE IF EXISTS public.lever_id_seq;
DROP SEQUENCE IF EXISTS public.location_id_seq;
DROP SEQUENCE IF EXISTS public.optim_user_id_seq;
DROP SEQUENCE IF EXISTS public.ppsid_id_seq;
DROP SEQUENCE IF EXISTS public.purchase_orders_id_seq;
DROP SEQUENCE IF EXISTS public.sequence_generator;
DROP SEQUENCE IF EXISTS public.siglum_id_seq;
DROP SEQUENCE IF EXISTS public.workload_id_seq;


-- Create Statements --

CREATE SEQUENCE public.cost_center_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.employee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.head_count_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.job_request_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.lever_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.location_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.optim_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.ppsid_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.purchase_orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.sequence_generator
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.siglum_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.workload_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.cost_center (
    id bigint DEFAULT nextval('public.cost_center_id_seq'::regclass) NOT NULL,
    cost_center_code character varying(255),
    cost_center_financial_code character varying(255),
    efficiency double precision,
    rate_own double precision,
    rate_sub double precision
);

CREATE TABLE public.employee (
    id bigint DEFAULT nextval('public.employee_id_seq'::regclass) NOT NULL,
    employee_id integer,
    direct character varying(255),
    job character varying(255),
    collar character varying(255),
    last_name character varying(255),
    first_name character varying(255),
    active_workforce character varying(255),
    availability_reason character varying(255),
    contract_type character varying(255),
    f_te integer,
    job_request_id bigint
);

CREATE TABLE public.head_count (
    id bigint DEFAULT nextval('public.head_count_id_seq'::regclass) NOT NULL,
    exercise character varying(255),
    description character varying(255),
    year character varying(255),
    f_te integer
);

CREATE TABLE public.job_request (
    id bigint DEFAULT nextval('public.job_request_id_seq'::regclass) NOT NULL,
    workday_number character varying(255),
    type character varying(255),
    status character varying(255),
    description character varying(255),
    candidate character varying(255),
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    posting_date timestamp without time zone,
    external boolean,
    early_career boolean,
    on_top_hct boolean,
    is_critical boolean,
    f_te integer
);

CREATE TABLE public.lever (
    id bigint DEFAULT nextval('public.lever_id_seq'::regclass) NOT NULL,
    lever_type character varying(255) NOT NULL,
    highlights character varying(255),
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    amount double precision,
    employee_id bigint,
    siglum_destination_id bigint
);


CREATE TABLE public.location (
    id bigint DEFAULT nextval('public.location_id_seq'::regclass) NOT NULL,
    country character varying(255),
    site character varying(255),
    kapis_code character varying(255),
    cost_center_id bigint,
    employee_id bigint,
    job_request_id bigint,
    purchase_orders_id bigint,
    workload_id bigint
);

CREATE TABLE public.optim_user (
    id bigint DEFAULT nextval('public.optim_user_id_seq'::regclass) NOT NULL,
    login character varying(50) NOT NULL,
    password_hash character varying(60) NOT NULL,
    first_name character varying(50),
    last_name character varying(50),
    email character varying(254),
    activated boolean DEFAULT false NOT NULL,
    lang_key character varying(10),
    image_url character varying(256),
    activation_key character varying(20),
    reset_key character varying(20),
    reset_date timestamp without time zone,
    created_date timestamp without time zone
);

CREATE TABLE public.ppsid (
    id bigint DEFAULT nextval('public.ppsid_id_seq'::regclass) NOT NULL,
    ppsid character varying(255),
    ppsid_name character varying(255),
    mu_code character varying(255),
    mu_text character varying(255),
    business_line character varying(255),
    program_line character varying(255),
    production_center character varying(255),
    business_activity character varying(255),
    backlog_order_intake character varying(255),
    workload_id bigint
);

CREATE TABLE public.purchase_orders (
    id bigint DEFAULT nextval('public.purchase_orders_id_seq'::regclass) NOT NULL,
    description character varying(255),
    provider character varying(255),
    order_request character varying(255),
    purchase_document character varying(255),
    hmg character varying(255),
    pep character varying(255),
    quarter character varying(255),
    year character varying(255),
    k_eur double precision
);

CREATE TABLE public.siglum (
    id bigint DEFAULT nextval('public.siglum_id_seq'::regclass) NOT NULL,
    siglum_hr character varying(255),
    siglum_6 character varying(255),
    siglum_5 character varying(255),
    siglum_4 character varying(255),
    siglum_3 character varying(255),
    employee_id bigint,
    job_request_id bigint,
    head_count_id bigint,
    purchase_orders_id bigint,
    workload_id bigint
);

CREATE TABLE public.workload (
    id bigint DEFAULT nextval('public.workload_id_seq'::regclass) NOT NULL,
    direct character varying(255),
    collar character varying(255),
    own character varying(255),
    core character varying(255),
    scenario character varying(255),
    go character varying(255),
    description character varying(255),
    exercise character varying(255),
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    k_hrs double precision,
    f_te double precision,
    k_eur double precision
);