package com.dilaykal.repository;

import org.springframework.stereotype.Repository;

import java.net.http.HttpResponse;
@Repository
public interface ITefasApiRepository {
    HttpResponse<String> getMonthlyReturnsData() throws Exception;
    HttpResponse<String> getDailyReturnsData() throws Exception;
}
