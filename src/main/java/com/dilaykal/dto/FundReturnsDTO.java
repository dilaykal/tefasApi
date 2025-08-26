// FundReturnsDTO.java
package com.dilaykal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FundReturnsDTO {
    //private Integer id;
    private String fundId;
    private String longName;
    private String fund_desc;
    private LocalDate date;
    private BigDecimal getiri1A;
    private BigDecimal getiri3A;
    private BigDecimal getiri6A;
    private BigDecimal getiri1Y;
    private BigDecimal getiri_gunluk;
    //private BigDecimal returnValue;
    //private ReturnTypesDTO returnTypes;
}