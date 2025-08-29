// FundReturnsDTO.java
package com.dilaykal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import lombok.Data;

@Data
public class FundReturnsDTO {
    private String fundCode;
    private String longName;
    private String fund_desc;
    private LocalDate date;
    private ArrayList<ReturnTypesDTO> returns;
}