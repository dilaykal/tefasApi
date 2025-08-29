package com.dilaykal.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Data
@Entity
@NoArgsConstructor(force = true)
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
