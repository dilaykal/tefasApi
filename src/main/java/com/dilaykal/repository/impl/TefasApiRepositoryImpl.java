package com.dilaykal.repository.impl;

import com.dilaykal.model.DateUtils;
import com.dilaykal.repository.ITefasApiRepository;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
@Repository
public class TefasApiRepositoryImpl implements ITefasApiRepository {
    private static final String URL_MONTHLY = "https://www.tefas.gov.tr/api/DB/BindComparisonFundReturns";
    private static final String URL_DAILY = "https://www.tefas.gov.tr/api/DB/BindHistoryInfo";
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> makeRequest(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    @Override
    public HttpResponse<String> getMonthlyReturnsData() throws Exception {
        String requestBodyComparison = "calismatipi=2&fontip=YAT&sfontur=&kurucukod=&fongrup=&bastarih=Ba%C5%9Flang%C3%A7&bittarih=Biti%C5%9F&fonturkod=&fonunvantip=&strperiod=1%2C1%2C1%2C1%2C1%2C1%2C1&islemdurum=1";
        return makeRequest(URL_MONTHLY,requestBodyComparison);
    }

    @Override
    public HttpResponse<String> getDailyReturnsData() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        //Bugün ve önceki son iş gününü al
        LocalDate today = LocalDate.now();
        LocalDate lastWorkingDay = today.minusDays(1);

        while (!DateUtils.isWorkingDay(lastWorkingDay)) {
            lastWorkingDay = lastWorkingDay.minusDays(1);
        }

        String bittarih = today.format(formatter);
        String bastarih = lastWorkingDay.format(formatter);

        String requestBodyHistory = "calismatipi=2&fontip=YAT&sfontur=&kurucukod=&fongrup=" +
                "&bastarih=" + bastarih + "&bittarih=" + bittarih + "&fonturkod=&fonunvantip=" +
                "&strperiod=1%2C1%2C1%2C1%2C1%2C1%2C1&islemdurum=1";
        return makeRequest(URL_DAILY,requestBodyHistory);
    }

}
