INSERT INTO public.job_request (
    workday_number, type, status, description, candidate,
    start_date, end_date, posting_date, external, early_career,
    on_top_hct, is_critical, f_te
) VALUES (
    NULL,
    'Creation',
    'Validation Required',
    'TEMP - Within HC Target => Needs approval by SHRBP / HO T1Q',
    NULL,
    '2024-08-01 00:00:00',
    '2024-12-01 00:00:00',
    NULL,
    True,
    True,
    False,
    False,
    1
);

INSERT INTO public.job_request (
    workday_number, type, status, description, candidate,
    start_date, end_date, posting_date, external, early_career,
    on_top_hct, is_critical, f_te
) VALUES (
    'JR1025558',
    'Creation',
    'Opened',
    'AWF - Within HC Target - External - Critical => Needs approval by COO',
    NULL,
    '2024-08-01 00:00:00',
    '2099-12-01 00:00:00',
    '2024-01-28 00:00:00',
    True,
    False,
    False,
    True,
    1
);

INSERT INTO public.job_request (
    workday_number, type, status, description, candidate,
    start_date, end_date, posting_date, external, early_career,
    on_top_hct, is_critical, f_te
) VALUES (
    'JR1025649',
    'Replacement',
    'Filled',
    'AWF - Within HC Target - External - No Critical => Needs approval by Employment Committee',
    'Sebastian Vettel',
    '2024-08-01 00:00:00',
    '2099-12-01 00:00:00',
    '2024-01-08 00:00:00',
    True,
    True,
    False,
    False,
    1
);
