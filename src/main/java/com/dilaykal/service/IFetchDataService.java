package com.dilaykal.service;

import org.springframework.stereotype.Repository;

import java.net.http.HttpResponse;
@Repository
public interface IFetchDataService {
    HttpResponse<String> getMonthlyReturnsData() throws Exception;
    HttpResponse<String> getDailyReturnsData() throws Exception;
}
