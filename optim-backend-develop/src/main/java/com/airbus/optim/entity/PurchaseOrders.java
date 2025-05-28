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
 * A PurchaseOrders.
 */
@Entity
@Table(name = "purchase_orders")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PurchaseOrders implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "provider")
    private String provider;

    @Column(name = "order_request")
    private String orderRequest;

    @Column(name = "purchase_document")
    private String purchaseDocument;

    @Column(name = "hmg")
    private String hmg;

    @Column(name = "pep")
    private String pep;

    @Column(name = "quarter")
    private String quarter;

    @Column(name = "year")
    private String year;

    @Column(name = "k_eur")
    private Double kEur;

    @Column(name = "approved")
    private String approved;

    @Column(name = "location_id")
    private Long locationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "siglum_id")
    @JsonIgnoreProperties(value = { "jobRequests", "headCounts", "purchaseOrders" }, allowSetters = true)
    @ToString.Exclude
    private Siglum siglum;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "purchaseOrders")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "costCenter", "employee", "jobRequest", "purchaseOrders", "workloads" }, allowSetters = true)
    @ToString.Exclude
    private List<Location> locations = new ArrayList<>();
}
