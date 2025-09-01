package com.dilaykal.service;

import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.dto.ReturnDataDTO;

import java.time.LocalDate;
import java.util.List;

public interface IFundService {
    void fetchAndSaveAllFundReturns();
    List<FundReturnsDTO> getAllFundReturns();

    List<FundReturnsDTO> getByFundCode(String fundCode, LocalDate startDate, LocalDate endDate);

    void updatedFundReturns(String fundCode,LocalDate date, List<ReturnDataDTO> updatedReturns);
}
