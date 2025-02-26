package com.airbus.optim.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.time.Instant;

/**
 * A Workload.
 */
@Entity
@Table(name = "workload")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Workload implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "direct")
    private String direct;

    @Column(name = "collar")
    private String collar;

    @Column(name = "own")
    private String own;

    @Column(name = "core")
    private String core;

    @Column(name = "scenario")
    private String scenario;

    @Column(name = "go")
    private Boolean go;

    @Column(name = "description")
    private String description;

    @Column(name = "exercise")
    private String exercise;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "k_hrs")
    private Double kHrs;

    @Column(name = "f_te")
    private Double fTE;

    @Column(name = "k_eur")
    private Double kEur;

    @Column(name = "eac")
    private Boolean eac;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id", unique = true)
    @JsonIgnoreProperties(value = { "workloads", "employees", "jobRequests", "levers" }, allowSetters = true)
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siglum_id", nullable = true)
    @JsonIgnoreProperties(value = { "employees", "jobRequest", "headCount", "purchaseOrders", "workloads" }, allowSetters = true)
    @ToString.Exclude
    private Siglum siglum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ppsid_id", nullable = true)
    @JsonIgnoreProperties(value = { "workloads" }, allowSetters = true)
    @ToString.Exclude
    private PPSID ppsid;
}
