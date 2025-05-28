package com.airbus.optim.utils;

public class Constants {
    public static final String LOCAL = "LOCAL";
    public static final String ENVIRONMENT = System.getenv("APP_ENV");

    public static final String REPORTS_MONTHLY = "monthly";
    public static final String REPORTS_END_OF_YEAR = "endOfYear";

    public static final String EXERCISE_FORECAST = "FCII";
    public static final String EXERCISE_OPERATION_PLANNING = "OP";
    public static final String WORKLOAD_STATUS_BOTTOM_UP = "BU";
    public static final String WORKLOAD_STATUS_WORK_IN_PROGRES = "WIP";
    public static final String WORKLOAD_STATUS_BOTTOM_UP_SUBMISSION = "First Submission";
    public static final String WORKLOAD_STATUS_QMC = "QMC";
    public static final String WORKLOAD_STATUS_HOT1Q = "HOT1Q";

    public static final String WORKLOAD_STATUS_INDIRECT = "Indirect";
    public static final String WORKLOAD_STATUS_DIRECT = "Direct";

    public static final String WORKLOAD_EVOLUTION_STATUS_OPENED = "opened";
    public static final String WORKLOAD_EVOLUTION_STATUS_CLOSED = "closed";
    public static final String WORKLOAD_EVOLUTION_STATUS_BU_SUBMIT = "submit";
    public static final String WORKLOAD_EVOLUTION_STATUS_PENDING = "pending";
    public static final String WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_HR = "first_submission_approved_siglum_hr";
    public static final String WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_6 = "first_submission_approved_siglum_6";
    public static final String WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_6 = "first_submission_rejected_siglum_6";
    public static final String WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_5 = "first_submission_approved_siglum_5";
    public static final String WORKLOAD_EVOLUTION_STATUS_REJECTED_BY_SIGLUM_5 = "first_submission_rejected_siglum_5";
    public static final String WORKLOAD_EVOLUTION_STATUS_SUBMIT_BY_SIGLUM_4 = "first_submission_approved_siglum_4";
    public static final String WORKLOAD_EVOLUTION_STATUS_QMC_REJECTED = "qmc_rejected";
    public static final String WORKLOAD_EVOLUTION_STATUS_QMC_APPROVED = "qmc_approved";
    public static final String WORKLOAD_EVOLUTION_STATUS_QMC_PENDING = "qmc_pending";
    public static final String WORKLOAD_EVOLUTION_STATUS_HOT1Q_REJECTED = "ho_t1q_rejected";
    public static final String WORKLOAD_EVOLUTION_STATUS_HOT1Q_APPROVED = "ho_t1q_approved";
    public static final String WORKLOAD_EVOLUTION_STATUS_HOT1Q_PENDING = "ho_t1q_pending";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_SUBMIT = "SUBMIT";
    public static final String STATUS_OPENED = "OPENED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ALL = "ALL";
    public static final String WORKLOAD_STATUS_OWN = "OWN";
    public static final String WORKLOAD_STATUS_SUB = "SUB";

    public static final String USER_ROLE_HO_SIGLUM = "ho.";
    public static final String USER_ROLE_QMC = "QMC member";
    public static final String USER_ROLE_HO_T1Q = "HO T1Q";
    public static final String USER_ROLE_WL_DELEGATE = "WL Delegate";
    public static final String USER_ROLE_ADMIN = "admin";
    public static final String USER_ROLE_HR_SUPERBOSS = "hrBoss.superboss";
    public static final String USER_ROLE_HR_COLLEAGUE = "hr.colleague";
    public static final String USER_ROLE_FINANCE_SUPERBOSS = "finance.superboss";
    public static final String USER_ROLE_FINANCE_COLLEAGUE = "finance.colleague";

    public static final String USER_SIGLUM_HR = "siglum_hr";
    public static final String USER_SIGLUM_6 = "siglum_6";
    public static final String USER_SIGLUM_5 = "siglum_5";
    public static final String USER_SIGLUM_4 = "siglum_4";
    public static final String USER_SIGLUM_3 = "siglum_3";

    public static final String JOB_REQUEST_STATUS_CLOSED = "Closed";
    public static final String JOB_REQUEST_STATUS_ON_HOLD = "On hold";
    public static final String JOB_REQUEST_STATUS_OPPENED = "Opened";
    public static final String JOB_REQUEST_STATUS_FILLED = "Filled";
    public static final String JOB_REQUEST_STATUS_VALIDATION_REQUEST = "Validation Required";
    public static final String JOB_REQUEST_STATUS_QMC_APPROVED = "QMC Approved";
    public static final String JOB_REQUEST_STATUS_SHRBP_T1Q_APPROVED = "SHRBP T1Q Approved";
    public static final String JOB_REQUEST_STATUS_HO_T1Q_APPROVED = "HO T1Q Approved";
    public static final String JOB_REQUEST_STATUS_COO_APPROVED = "COO Approved";

}
