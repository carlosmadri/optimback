package com.airbus.optim.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * A Siglum.
 */
@Entity
@Table(name = "siglum")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Siglum implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "siglum_hr")
    private String siglumHR;

    @Column(name = "siglum_6")
    private String siglum6;

    @Column(name = "siglum_5")
    private String siglum5;

    @Column(name = "siglum_4")
    private String siglum4;

    @Column(name = "siglum_3")
    private String siglum3;

    @OneToMany(mappedBy = "siglum", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Employee> employees = new ArrayList<>();

    @OneToMany(mappedBy = "siglum", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private List<JobRequest> jobRequests = new ArrayList<>();

    @OneToMany(mappedBy = "siglum", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private List<HeadCount> headCounts = new ArrayList<>();

    @OneToMany(mappedBy = "siglum", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private List<PurchaseOrders> purchaseOrders = new ArrayList<>();

    @OneToMany(mappedBy = "siglum", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Workload> workloads = new ArrayList<>();
}
