package com.dilaykal.controller;

import com.dilaykal.dto.FundInfoDTO;
import com.dilaykal.dto.FundReturnRequestDTO;
import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.entities.FundReturns;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface IFundController {
    ResponseEntity<List<FundInfoDTO>> getFundReturns();

    List <FundReturnsDTO> getFundReturnsById(String fundId,LocalDate startDate, LocalDate endDate);
}
