package com.dilaykal.controller;

import com.dilaykal.dto.FundReturnsDTO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface IFundController {
   ResponseEntity<List<FundReturnsDTO>> getFundReturns();

    List<FundReturnsDTO> getFundReturnsByFundCode(String fundCode, LocalDate startDate, LocalDate endDate);
}
