package com.dilaykal.controller.impl;

import com.dilaykal.controller.IFundController;
import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.service.IFundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/funds")
public class FundControllerImpl implements IFundController {
    @Autowired
    private IFundService fundService;

    @GetMapping("/list-all")
    @Override
    public ResponseEntity<List<FundReturnsDTO>>  getFundReturns() {
        List<FundReturnsDTO> funds =fundService.getAllFundReturns();
        return ResponseEntity.ok(funds);
    }
    @Override
    @GetMapping("/list/{fundCode}")
    public List<FundReturnsDTO> getFundReturnsByFundCode(@PathVariable String fundCode,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return fundService.getByFundCode(fundCode,startDate,endDate);
    }



}
