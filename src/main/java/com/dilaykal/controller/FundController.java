package com.dilaykal.controller;

import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.dto.ReturnDataDTO;
import com.dilaykal.service.IFundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/funds")
public class FundController {
    @Autowired
    private IFundService fundService;

    @GetMapping("")
    public ResponseEntity<List<FundReturnsDTO>>  getFundReturns() {
        List<FundReturnsDTO> funds =fundService.getAllFundReturns();
        return ResponseEntity.ok(funds);
    }

    @GetMapping("/{fundCode}")
    public ResponseEntity<List<FundReturnsDTO>> getFundReturnsByFundCode(@PathVariable String fundCode,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<FundReturnsDTO> fundList = fundService.getByFundCode(fundCode,startDate,endDate);
        return ResponseEntity.ok(fundList);
    }
    @PutMapping("/{fundCode}")
    public ResponseEntity<Void> updatedReturns(
          @PathVariable String fundCode,
          @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
          @RequestBody List<ReturnDataDTO> updatedReturns
    ){
        try{
            fundService.updatedFundReturns(fundCode,date,updatedReturns);
            return ResponseEntity.ok().build();
        }catch (RuntimeException e){
            return ResponseEntity.notFound().build();
        }
    }



}
