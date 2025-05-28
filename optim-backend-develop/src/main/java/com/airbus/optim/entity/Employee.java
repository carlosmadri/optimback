package com.airbus.optim.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
 * A Employee.
 */
@Entity
@Table(name = "employee")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "direct")
    private String direct;

    @Column(name = "job")
    private String job;

    @Column(name = "collar")
    private String collar;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "active_workforce")
    private String activeWorkforce;

    @Column(name = "availability_reason")
    private String availabilityReason;

    @Column(name = "contract_type")
    private String contractType;

    @Column(name = "f_te")
    private Float fTE;

    @Column(name = "impersonal")
    private Boolean impersonal = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnoreProperties(value = { "employees", "jobRequest", "headCount", "purchaseOrders", "workloads" }, allowSetters = true)
    private Siglum siglum;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cost_center_id", unique = true)
    @JsonIgnoreProperties(value = { "employees", "jobRequests", "workloads", "levers" }, allowSetters = true)
    private CostCenter costCenter;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "employee")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "employee", "levers", "costCenter" }, allowSetters = true)
    @ToString.Exclude
    private List<Lever> levers = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JsonIgnoreProperties(value = { "siglumHRS", "locations", "employees" }, allowSetters = true)
    @ToString.Exclude
    private JobRequest jobRequest;

    public Employee jobRequest(JobRequest jobRequest) {
        this.setJobRequest(jobRequest);
        return this;
    }

    public Employee addLever(Lever lever) {
        this.levers.add(lever);
        lever.setEmployee(this);
        return this;
    }

    public Employee removeLever(Lever lever) {
        this.levers.remove(lever);
        lever.setEmployee(null);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Employee employee = (Employee) o;
        return id != null && Objects.equals(id, employee.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
