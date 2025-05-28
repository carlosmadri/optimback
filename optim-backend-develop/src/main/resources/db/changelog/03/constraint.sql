ALTER TABLE public.job_request_approval
    ADD CONSTRAINT fk_employee FOREIGN KEY (employee_id)
    REFERENCES public.employee (id)
    ON DELETE CASCADE;

ALTER TABLE public.job_request_approval
    ADD CONSTRAINT fk_job_request FOREIGN KEY (job_request_id)
    REFERENCES public.job_request (id)
    ON DELETE CASCADE;

ALTER TABLE ONLY public.job_request
    DROP CONSTRAINT IF EXISTS fk_employee_jobrequest;

ALTER TABLE ONLY public.siglum
    DROP CONSTRAINT IF EXISTS fk_employee_siglum,
    ADD CONSTRAINT fk_employee_siglum FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.siglum
    DROP CONSTRAINT IF EXISTS fk_siglum_job_request,
    ADD CONSTRAINT fk_siglum_job_request FOREIGN KEY (job_request_id) REFERENCES public.job_request(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.siglum
    DROP CONSTRAINT IF EXISTS fk_siglum_head_count,
    ADD CONSTRAINT fk_siglum_head_count FOREIGN KEY (head_count_id) REFERENCES public.head_count(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.siglum
    DROP CONSTRAINT IF EXISTS fk_siglum_purchase_orders,
    ADD CONSTRAINT fk_siglum_purchase_orders FOREIGN KEY (purchase_orders_id) REFERENCES public.purchase_orders(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.siglum
    DROP CONSTRAINT IF EXISTS fk_siglum_workload,
    ADD CONSTRAINT fk_siglum_workload FOREIGN KEY (workload_id) REFERENCES public.workload(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.ppsid
    DROP CONSTRAINT IF EXISTS fk_ppsid_workload,
    ADD CONSTRAINT fk_ppsid_workload FOREIGN KEY (workload_id) REFERENCES public.workload(id) ON DELETE SET NULL;