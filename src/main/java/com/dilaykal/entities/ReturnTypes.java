package com.dilaykal.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Set;
@Data
@Entity
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@Table(name="return_types")
public class ReturnTypes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255)
    @NonNull
    private String description;

    @OneToMany(mappedBy = "returnTypes",cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FundReturns> fundReturns;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnTypes that = (ReturnTypes) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
