package com.dilaykal.controller.impl;

import com.dilaykal.controller.IFundController;
import com.dilaykal.dto.FundInfoDTO;
import com.dilaykal.dto.FundReturnRequestDTO;
import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.entities.FundInfo;
import com.dilaykal.entities.FundReturns;
import com.dilaykal.service.IFundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/funds")
public class FundControllerImpl implements IFundController {
    @Autowired
    private IFundService fundService;
    @GetMapping("/list-all")
    @Override
    public ResponseEntity<List<FundInfoDTO>>  getFundReturns() {
        List<FundInfoDTO> funds =fundService.getAllFundReturnsFromDb();
        return ResponseEntity.ok(funds);
    }

    @Override
    @GetMapping("/list/{fundId}")
    public List<FundReturnsDTO> getFundReturnsById(@PathVariable String fundId,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){

       System.out.println("bugünün tarihi : "+ LocalDate.now());
       System.out.println("startDate : " + startDate);
        System.out.println("endDate : " + endDate);

        return fundService.getByFundId(fundId,startDate,endDate);
    }



}
