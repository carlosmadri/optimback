ALTER TABLE public.job_request
    DROP CONSTRAINT IF EXISTS fk_job_request_siglum;
ALTER TABLE public.head_count
    DROP CONSTRAINT IF EXISTS fk_head_count_siglum;
ALTER TABLE public.purchase_orders
    DROP CONSTRAINT IF EXISTS fk_purchase_orders_siglum;

ALTER TABLE job_request ADD COLUMN IF NOT EXISTS siglum_id BIGINT;
ALTER TABLE head_count ADD COLUMN IF NOT EXISTS siglum_id BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS siglum_id BIGINT;

ALTER TABLE job_request ADD CONSTRAINT fk_job_request_siglum FOREIGN KEY (siglum_id) REFERENCES siglum(id);
ALTER TABLE head_count ADD CONSTRAINT fk_head_count_siglum FOREIGN KEY (siglum_id) REFERENCES siglum(id);
ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_siglum FOREIGN KEY (siglum_id) REFERENCES siglum(id);
