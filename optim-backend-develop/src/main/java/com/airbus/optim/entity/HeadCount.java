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

/**
 * A HeadCount.
 */
@Entity
@Table(name = "head_count")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HeadCount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "year")
    private String year;

    @Column(name = "f_te")
    private Float fTE;

    @Column(name = "ref_count")
    private String refCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siglum_id")
    @JsonIgnoreProperties(value = { "jobRequests", "headCounts", "purchaseOrders" }, allowSetters = true)
    @ToString.Exclude
    private Siglum siglum;
}
