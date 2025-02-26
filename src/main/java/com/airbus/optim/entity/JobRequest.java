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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A JobRequest.
 */
@Entity
@Table(name = "job_request")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class JobRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "workday_number")
    private String workdayNumber;

    @Column(name = "type")
    private String type;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "candidate")
    private String candidate;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "release_date")
    private Instant releaseDate;

    @Column(name = "posting_date")
    private Instant postingDate;

    @Column(name = "external")
    private Boolean external;

    @Column(name = "early_career")
    private Boolean earlyCareer;

    @Column(name = "on_top_hct")
    private Boolean onTopHct;

    @Column(name = "is_critical")
    private Boolean isCritical;

    @Column(name = "active_workforce")
    private String activeWorkforce;

    @Column(name = "approved_qmc")
    private Boolean approvedQMC;

    @Column(name = "approved_shrbph1q")
    private Boolean approvedSHRBPHOT1Q;

    @Column(name = "approved_hocoohohrcoo")
    private Boolean approvedHOCOOHOHRCOO;

    @Column(name = "approved_employment_commitee")
    private Boolean approvedEmploymentCommitee;

    @Column(name = "direct")
    private String direct;

    @Column(name = "collar", length = 10)
    private String collar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siglum_id")
    @JsonIgnoreProperties(value = { "jobRequests", "headCounts", "purchaseOrders" }, allowSetters = true)
    @ToString.Exclude
    private Siglum siglum;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "jobRequest", orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnore
    @ToString.Exclude
    private List<Employee> employees = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    @JsonIgnoreProperties(value = { "jobRequests", "levers" }, allowSetters = true)
    @ToString.Exclude
    private CostCenter costCenter;

    public JobRequest addEmployee(Employee employee) {
        this.employees.add(employee);
        employee.setJobRequest(this);
        return this;
    }

    public JobRequest removeEmployee(Employee employee) {
        this.employees.remove(employee);
        employee.setJobRequest(null);
        return this;
    }
}
