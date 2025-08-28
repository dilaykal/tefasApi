package com.dilaykal.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;

@Data
@Entity
@NoArgsConstructor(force = true)
//@RequiredArgsConstructor
@Table(name= "fund_returns")
public class FundReturns {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="fund_id")
    private FundInfo fundInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="return_type_id")
    private ReturnTypes returnTypes;

    //@NonNull
    @Column(name = "return_value",precision = 18, scale = 10)
    private BigDecimal returnValue;

    @Temporal(TemporalType.DATE)
    private LocalDate date;

    public FundReturns(BigDecimal returnValue, LocalDate date) {
        this.returnValue = returnValue;
        this.date = date;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FundReturns that = (FundReturns) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
