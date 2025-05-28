package com.airbus.optim.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A CostCenter.
 */
@Entity
@Table(name = "cost_center")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CostCenter implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "cost_center_code")
    private String costCenterCode;

    @Column(name = "cost_center_financial_code")
    private String costCenterFinancialCode;

    @Column(name = "efficiency")
    private Double efficiency;

    @Column(name = "rate_own")
    private Double rateOwn;

    @Column(name = "rate_sub")
    private Double rateSub;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id")
    @JsonIgnoreProperties(value = { "costCenters", "employees", "jobRequest", "purchaseOrders", "workloads", "levers" }, allowSetters = true)
    @ToString.Exclude
    private Location location;

    @OneToMany(mappedBy = "costCenter", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<JobRequest> jobRequests = new ArrayList<>();

    @OneToMany(mappedBy = "costCenter", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Lever> levers = new ArrayList<>();

    public CostCenter id(Long id) {
        this.setId(id);
        return this;
    }

    public CostCenter costCenterCode(String costCenterCode) {
        this.setCostCenterCode(costCenterCode);
        return this;
    }

    public CostCenter costCenterFinancialCode(String costCenterFinancialCode) {
        this.setCostCenterFinancialCode(costCenterFinancialCode);
        return this;
    }

    public CostCenter efficiency(Double efficiency) {
        this.setEfficiency(efficiency);
        return this;
    }

    public CostCenter rateOwn(Double rateOwn) {
        this.setRateOwn(rateOwn);
        return this;
    }

    public CostCenter rateSub(Double rateSub) {
        this.setRateSub(rateSub);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CostCenter that = (CostCenter) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
