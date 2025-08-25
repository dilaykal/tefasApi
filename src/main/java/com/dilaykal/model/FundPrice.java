package com.dilaykal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundPrice {
    public double todayPrice;
    public double yesterdayPrice;
}
