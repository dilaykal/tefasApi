package com.dilaykal.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.Set;
@Data
@Entity
@NoArgsConstructor(force = true)
@Table(name="fund_info")
@RequiredArgsConstructor
public class FundInfo {
    @Id
    @NonNull
    private String fund_id;
    @Column(length = 255)
    @NonNull
    private String longName;
    @Column(length = 255)
    @NonNull
    private String fund_desc;
    @OneToMany(mappedBy = "fundInfo",cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FundReturns> fundReturns;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FundInfo fundInfo = (FundInfo) o;
        return Objects.equals(fund_id, fundInfo.fund_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fund_id);
    }
}
