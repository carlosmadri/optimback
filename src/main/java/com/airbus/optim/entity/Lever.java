package com.airbus.optim.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
 * A Lever.
 */
@Entity
@Table(name = "lever")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Lever implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "lever_type", nullable = false)
    private String leverType;

    @Column(name = "highlights")
    private String highlights;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "f_te")
    private Float fTE;

    @Column(name = "direct")
    private String direct;

    @Column(name = "active_workforce")
    private String activeWorkforce;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siglum_destination_id", nullable = true)
    @JsonIgnoreProperties(value = { "employee", "employees" }, allowSetters = true)
    private Siglum siglumDestination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonIgnoreProperties(value = { "levers", "siglumHRS", "locations", "jobRequest", "costCenter" }, allowSetters = true)
    @ToString.Exclude
    private Employee employee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id", unique = true)
    @JsonIgnoreProperties(value = { "employees", "jobRequests", "workloads", "levers" }, allowSetters = true)
    private CostCenter costCenter;

    public Lever id(Long id) {
        this.setId(id);
        return this;
    }

    public Lever leverType(String leverType) {
        this.setLeverType(leverType);
        return this;
    }

    public Lever highlights(String highlights) {
        this.setHighlights(highlights);
        return this;
    }

    public Lever startDate(Instant startDate) {
        this.setStartDate(startDate);
        return this;
    }

    public Lever endDate(Instant endDate) {
        this.setEndDate(endDate);
        return this;
    }

    public Lever employee(Employee employee) {
        this.setEmployee(employee);
        return this;
    }
}
