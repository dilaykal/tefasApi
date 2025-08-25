// FundReturnsDTO.java
package com.dilaykal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class FundReturnsDTO {
    private Integer id;
    private String fundId;
    private String longName;
    private String fund_desc;
    private BigDecimal returnValue;
    private LocalDate date;
    private ReturnTypesDTO returnTypes;
}