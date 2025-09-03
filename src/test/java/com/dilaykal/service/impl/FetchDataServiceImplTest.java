package com.dilaykal.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FetchDataServiceImplTest {
    @InjectMocks
    private FetchDataServiceImpl fetchDataService;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;
    @BeforeEach
    void setUp() throws Exception{
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockHttpResponse);
    }

    @Test
    void getMonthlyReturnsData_shouldMakeCorrectRequest() throws Exception {
        // ACT
        HttpResponse<String> response = fetchDataService.getMonthlyReturnsData();

        // ASSERT
        Assertions.assertNotNull(response);

        // HttpClient'ın doğru URL ve body ile çağrıldığını kontrol et
        Mockito.verify(mockHttpClient).send(
                argThat(request -> request.uri().toString().contains("BindComparisonFundReturns") &&
                        "POST".equals(request.method()) &&
                        request.bodyPublisher().isPresent()
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

}
