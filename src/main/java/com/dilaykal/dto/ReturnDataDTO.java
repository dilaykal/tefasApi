package com.dilaykal.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReturnDataDTO {
    private String description;
    private BigDecimal value;
}