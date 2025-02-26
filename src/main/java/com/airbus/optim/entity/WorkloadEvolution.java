package com.airbus.optim.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.Instant;

@Entity
@Table(name = "workload_evolution")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WorkloadEvolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise", nullable = false)
    private String exercise;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "f_te", nullable = false)
    private Float fTE;

    @Column(name = "k_hrs_own_direct", nullable = false)
    private Double kHrsOwnDirect;

    @Column(name = "k_hrs_own_indirect", nullable = false)
    private Double kHrsOwnIndirect;

    @Column(name = "k_hrs_sub_direct", nullable = false)
    private Double kHrsSubDirect;

    @Column(name = "k_hrs_sub_indirect", nullable = false)
    private Double kHrsSubIndirect;

    @Column(name = "submit_date", nullable = false)
    private Instant submitDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siglum_id", nullable = false)
    @JsonIgnoreProperties(value = { "siglumHRS", "locations", "employees" }, allowSetters = true)
    @ToString.Exclude
    private Siglum siglum;
}
