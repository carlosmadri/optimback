ALTER TABLE ONLY public.cost_center
    ADD CONSTRAINT cost_center_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.head_count
    ADD CONSTRAINT head_count_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.job_request
    ADD CONSTRAINT job_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.lever
    ADD CONSTRAINT lever_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.location
    ADD CONSTRAINT location_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.optim_user
    ADD CONSTRAINT optim_user_email_key UNIQUE (email);

ALTER TABLE ONLY public.optim_user
    ADD CONSTRAINT optim_user_login_key UNIQUE (login);

ALTER TABLE ONLY public.optim_user
    ADD CONSTRAINT optim_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ppsid
    ADD CONSTRAINT ppsid_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.purchase_orders
    ADD CONSTRAINT purchase_orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.siglum
    ADD CONSTRAINT siglum_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.lever
    ADD CONSTRAINT uq_lever_employee UNIQUE (employee_id);

ALTER TABLE ONLY public.location
    ADD CONSTRAINT uq_location_cost_center UNIQUE (cost_center_id);

ALTER TABLE ONLY public.workload
    ADD CONSTRAINT workload_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT fk_employee_to_job_request FOREIGN KEY (job_request_id) REFERENCES public.job_request(id);

ALTER TABLE ONLY public.job_request
    ADD CONSTRAINT fk_employee_jobrequest FOREIGN KEY (id) REFERENCES public.employee(id);

ALTER TABLE ONLY public.siglum
    ADD CONSTRAINT fk_employee_siglum FOREIGN KEY (employee_id) REFERENCES public.employee(id);

ALTER TABLE ONLY public.lever
    ADD CONSTRAINT fk_lever_employee FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON DELETE SET NULL;

ALTER TABLE public.lever
    ADD CONSTRAINT fk_siglum_destination FOREIGN KEY (siglum_destination_id) REFERENCES public.siglum (id) ON DELETE SET NULL;
    
ALTER TABLE public.lever
    ADD CONSTRAINT fk_siglum_origin FOREIGN KEY (siglum_origin_id) REFERENCES public.siglum (id) ON DELETE SET NULL;
    
ALTER TABLE ONLY public.location
    ADD CONSTRAINT fk_location_cost_center FOREIGN KEY (cost_center_id) REFERENCES public.cost_center(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.location
    ADD CONSTRAINT fk_location_employee FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.location
    ADD CONSTRAINT fk_location_job_request FOREIGN KEY (job_request_id) REFERENCES public.job_request(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.location
    ADD CONSTRAINT fk_location_purchase_orders FOREIGN KEY (purchase_orders_id) REFERENCES public.purchase_orders(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.location
    ADD CONSTRAINT fk_location_workload FOREIGN KEY (workload_id) REFERENCES public.workload(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.ppsid
    ADD CONSTRAINT fk_ppsid_workload FOREIGN KEY (workload_id) REFERENCES public.workload(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.siglum
    ADD CONSTRAINT fk_siglum_head_count FOREIGN KEY (head_count_id) REFERENCES public.head_count(id);

ALTER TABLE ONLY public.siglum
    ADD CONSTRAINT fk_siglum_job_request FOREIGN KEY (job_request_id) REFERENCES public.job_request(id);

ALTER TABLE ONLY public.siglum
    ADD CONSTRAINT fk_siglum_purchase_orders FOREIGN KEY (purchase_orders_id) REFERENCES public.purchase_orders(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.siglum
    ADD CONSTRAINT fk_siglum_workload FOREIGN KEY (workload_id) REFERENCES public.workload(id) ON DELETE SET NULL;