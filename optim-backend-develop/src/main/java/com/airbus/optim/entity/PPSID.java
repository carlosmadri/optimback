package com.airbus.optim.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

/**
 * A PPSID.
 */
@Entity
@Table(name = "ppsid")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PPSID implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "ppsid")
    private String ppsid;

    @Column(name = "ppsid_name")
    private String ppsidName;

    @Column(name = "mu_code")
    private String muCode;

    @Column(name = "mu_text")
    private String muText;

    @Column(name = "business_line")
    private String businessLine;

    @Column(name = "program_line")
    private String programLine;

    @Column(name = "production_center")
    private String productionCenter;

    @Column(name = "business_activity")
    private String businessActivity;

    @Column(name = "backlog_order_intake")
    private String backlogOrderIntake;

    @Column(name = "scenario")
    private String scenario;

    @OneToMany(mappedBy = "ppsid", fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "ppsid" }, allowSetters = true)
    @ToString.Exclude
    private List<Workload> workloads = new ArrayList<>();
}
