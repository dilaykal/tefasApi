package com.dilaykal.service;

import com.dilaykal.dto.FundInfoDTO;
import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.entities.FundInfo;
import com.dilaykal.entities.FundReturns;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IFundService {
    void fetchAndSaveAllFundReturns();
    List<FundInfoDTO> getAllFundReturnsFromDb();

    List<FundReturnsDTO> getByFundId(String fundId,LocalDate startDate,LocalDate endDate);
}
