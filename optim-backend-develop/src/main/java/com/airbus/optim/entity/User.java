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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A user.
 */
@Entity
@Table(name = "optim_user")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    @Email
    @Size(min = 5, max = 254)
    @Column(length = 254, unique = true)
    private String email;

    @NotNull
    @Column(nullable = false)
    private boolean activated = false;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "siglum_visible")
    private String siglumVisible;

    @Size(max = 50)
    @Column(name = "roles", length = 50)
    private String roles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siglum_id")
    @JsonIgnoreProperties(value = { "jobRequests", "headCounts", "purchaseOrders" }, allowSetters = true)
    @ToString.Exclude
    private Siglum siglum;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
