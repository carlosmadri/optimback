package com.airbus.optim.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * A Location.
 */
@Entity
@Table(name = "location")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Location implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "country")
    private String country;

    @Column(name = "site")
    private String site;

    @Column(name = "kapis_code")
    private String kapisCode;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "latitude")
    private Double latitude;

    @JsonIgnoreProperties(value = { "locations", "jobRequests", "levers", "location" }, allowSetters = true)
    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<CostCenter> costCenters = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "siglumHRS", "locations" }, allowSetters = true)
    @ToString.Exclude
    private PurchaseOrders purchaseOrders;

}
