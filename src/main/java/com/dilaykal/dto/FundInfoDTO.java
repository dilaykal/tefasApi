package com.dilaykal.dto;

import java.util.Set;
import lombok.Data;

@Data
public class FundInfoDTO {
    private String longName;
    private String fundDesc;
    private Set<FundReturnsDTO> fundReturns;
}