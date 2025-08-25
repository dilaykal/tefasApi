package com.dilaykal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundDailyReturn {
    private String fonKodu;
    private double dailyReturn;
}
