package com.dilaykal.service.impl;
import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.dto.ReturnDataDTO;
import com.dilaykal.entities.FundReturns;
import com.dilaykal.entities.ReturnTypes;
import com.dilaykal.entities.FundInfo;
import com.dilaykal.repository.IFundInfoRepository;
import com.dilaykal.repository.IFundReturnRepository;
import com.dilaykal.repository.IReturnTypeRepository;
import com.dilaykal.service.IFetchDataService;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) //JUnit 5
public class FundServiceImplTest {

    @Mock
    private IFetchDataService tefasApiRepository;   //Api çağrısı yapan servis davranışı taklit edecek
    //DB işlemleri için, gerçek veritabanına bağlanmayacak
    @Mock
    private IFundInfoRepository fundInfoRepository;
    @Mock
    private IFundReturnRepository fundReturnRepository;
    @Mock
    private IReturnTypeRepository returnTypeRepository;
    @InjectMocks
    private FundServiceImpl fundService;  //Gerçek sınıf mocklanmış veriler buraya enjekte edilecek

    @Test
    void fetchAndSaveAllFundReturns_shouldSaveNewData() throws Exception{
        //ARRANGE
        //Apı den gelen yanıtları mockla
        HttpResponse<String> monthlyResponse = mock(HttpResponse.class);
        HttpResponse<String> dailyResponse = mock(HttpResponse.class);

        //monthlyresponse ve dailyresponse için örnek bir json verisi
        String monthlyJson = "{\"data\": [" +
                "{\"FONKODU\": \"ABC\", \"FONUNVAN\": \"ABC Fonu\", \"FONTURACIKLAMA\": \"Hisse Senedi\"}, " +
                "{\"FONKODU\": \"XYZ\", \"FONUNVAN\": \"XYZ Fonu\", \"FONTURACIKLAMA\": \"Tahvil\"}" +
                "]}";
        String dailyJson = "{\"data\": []}";

        when(monthlyResponse.body()).thenReturn(monthlyJson);
        when(dailyResponse.body()).thenReturn(dailyJson);

        when(tefasApiRepository.getMonthlyReturnsData()).thenReturn(monthlyResponse);
        when(tefasApiRepository.getDailyReturnsData()).thenReturn(dailyResponse);

        // ACT
        fundService.fetchAndSaveAllFundReturns();

        // ASSERT
        // 1. Dış bağımlılıkların doğru şekilde çağrıldığını doğrula
        verify(tefasApiRepository, times(1)).getMonthlyReturnsData();
        verify(tefasApiRepository, times(1)).getDailyReturnsData();

        // 2. Veritabanına kaydetme işleminin doğru şekilde çağrıldığını doğrula
        verify(fundReturnRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testgetAllFundReturns(){
        //ARRANGE
        //sahte veri oluştur
        FundInfo fundInfo1 = new FundInfo("ABC", "ABC Fonu", "Hisse Senedi Fonu");
        ReturnTypes returnType1 = new ReturnTypes("1 Aylık Getiri");
        ReturnTypes returnType2 = new ReturnTypes("3 Aylık Getiri");
        LocalDate date = LocalDate.of(2023, 10, 26);

        FundReturns fr1 = new FundReturns(BigDecimal.valueOf(10.5),date);
        fr1.setFundInfo(fundInfo1);
        fr1.setReturnTypes(returnType1);

        FundReturns fr2 = new FundReturns(BigDecimal.valueOf(20.3),date);
        fr2.setFundInfo(fundInfo1);
        fr2.setReturnTypes(returnType2);

        when(fundReturnRepository.findAllWithAllDetails()).thenReturn(List.of(fr1,fr2));

        //ACT
        List<FundReturnsDTO> result = fundService.getAllFundReturns();

        //ASSERT
        //dönen listenin doğru boyutta olduğunu kontrol et
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1,result.size());

        //DTO'nun içeriği doğru şekilde dönüştürülüyor mu ?
        FundReturnsDTO dto = result.get(0);
        Assertions.assertEquals("ABC", dto.getFundCode());
        Assertions.assertEquals("ABC Fonu", dto.getLongName());
        Assertions.assertEquals(date, dto.getDate());
        Assertions.assertEquals(2, dto.getReturns().size());
        Assertions.assertEquals("1 Aylık Getiri", dto.getReturns().get(0).getDescription());
        Assertions.assertEquals(BigDecimal.valueOf(10.5), dto.getReturns().get(0).getValue());
    }
    @Test
    void testgetByFundCode() {
        //ARRANGE
        String fundCode = "ABC";
        LocalDate startDate = LocalDate.of(2023, 10, 25);
        LocalDate endDate = LocalDate.of(2023, 10, 26);

        FundInfo fundInfo = new FundInfo(fundCode, "ABC Fonu", "Hisse Senedi Fonu");
        ReturnTypes returnType = new ReturnTypes("Günlük Getiri");

        FundReturns fr1 = new FundReturns(BigDecimal.valueOf(1.2), startDate);
        fr1.setFundInfo(fundInfo);
        fr1.setReturnTypes(returnType);

        FundReturns fr2 = new FundReturns(BigDecimal.valueOf(1.5), endDate);
        fr2.setFundInfo(fundInfo);
        fr2.setReturnTypes(returnType);


        when(fundReturnRepository.findByFundCodeAndDate(fundCode, startDate, endDate))
                .thenReturn(List.of(fr1, fr2));

        //ACT
        List<FundReturnsDTO> result = fundService.getByFundCode(fundCode, startDate, endDate);

        //ASSERT
        Assertions.assertNotNull(result);
        //Her tarihe bir DTO oluşacağı için 2 tane dönüyor
        Assertions.assertEquals(2, result.size());

        //Dönüş listesinden beklenen tarihleri içeren DTO'ları bul
        Optional<FundReturnsDTO> dto1 = result.stream()
                .filter(dto -> dto.getDate().isEqual(startDate))
                .findFirst();

        Optional<FundReturnsDTO> dto2 = result.stream()
                .filter(dto -> dto.getDate().isEqual(endDate))
                .findFirst();

        Assertions.assertTrue(dto1.isPresent(), "Başlangıç tarihine sahip DTO bulunamadı.");
        Assertions.assertTrue(dto2.isPresent(), "Bitiş tarihine sahip DTO bulunamadı.");

        Assertions.assertEquals(fundCode, dto1.get().getFundCode());
        Assertions.assertEquals(fundCode, dto2.get().getFundCode());
    }

    @Test
    void testupdatedFundReturns() {
        // ARRANGE
        String fundCode = "ABC";
        LocalDate date = LocalDate.now();

        //Güncellenecek sahte veriyi oluştur
        ReturnTypes returnType = new ReturnTypes("Günlük Getiri");
        FundInfo fundInfo = new FundInfo(fundCode, "ABC Fonu", "Hisse Senedi Fonu");
        FundReturns existingRecord = new FundReturns(BigDecimal.valueOf(1.2), date);
        existingRecord.setFundInfo(fundInfo);
        existingRecord.setReturnTypes(returnType);

        //Güncelleme DTO'sunu oluştur
        ReturnDataDTO updateData = new ReturnDataDTO();
        updateData.setDescription("Günlük Getiri");
        updateData.setValue(BigDecimal.valueOf(1.8));

        //Repository'nin find metodu ile kayıt varmış gibi göster
        when(fundReturnRepository.findByFundCodeAndReturnType(fundCode, date, "Günlük Getiri"))
                .thenReturn(Optional.of(existingRecord));

        // ACT
        fundService.updatedFundReturns(fundCode, date, List.of(updateData));

        // ASSERT
        // Repository'deki save metodunun çağrıldığını kontrol et
        verify(fundReturnRepository, times(1)).save(any(FundReturns.class));

        //Kaydın güncellendiğini kontrol et
        Assertions.assertEquals(BigDecimal.valueOf(1.8), existingRecord.getReturnValue());
    }

    @Test
    void processFundInfo_shouldReturnExistingFund_whenFound(){
        //ARRANGE
        JSONObject fundObject = new JSONObject().put("FONKODU","ABC").put("FONUNVAN","ABC Fonu").put("FONTURACIKLAMA","Hisse Senedi");
        FundInfo existingFund= new FundInfo("ABC","ABC Fonu","Hisse Senedi");
        when(fundInfoRepository.findByFundCode("ABC")).thenReturn(Optional.of(existingFund));

        //ACT
        FundInfo result = fundService.processFundInfo(fundObject);

        //ASSERT
        Assertions.assertEquals(existingFund,result);
        //kayıt varken çağrılmadığını kontrol et
        verify(fundInfoRepository,never()).save(any());
    }
    @Test
    void processFundInfo_shouldSaveNewFund_whenNotFound(){
        //ARRANGE
        JSONObject fundObject = new JSONObject().put("FONKODU","XYZ").put("FONUNVAN","XYZ Fonu").put("FONTURACIKLAMA","Tahvil");
        when(fundInfoRepository.findByFundCode("XYZ")).thenReturn(Optional.empty());
        when(fundInfoRepository.save(any(FundInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //ACT
        FundInfo result = fundService.processFundInfo(fundObject);

        //ASSERT
        Assertions.assertEquals("XYZ",result.getFund_code());
        verify(fundInfoRepository, times(1)).save(any(FundInfo.class));
    }
    @Test
    void fundReturnControl_shouldReturnBigDecimalForValidValue(){
        //Arrange
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("GETIRI1A",10.5);
        //ACT
        BigDecimal result = fundService.fundReturnControl(jsonObject,"GETIRI1A");
        //ASSERT
        Assertions.assertEquals(BigDecimal.valueOf(10.5),result);
    }
    @Test
    void fundReturnControl_shouldReturnNullForNullValue(){
        //ARRANGE
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("GETIRI1A", JSONObject.NULL);
        //ACT
        BigDecimal result = fundService.fundReturnControl(jsonObject,"GETIRI1A");
        //ASSERT
        Assertions.assertNull(result);
    }
    @Test
    void testconvertToDTO(){
        //ARRANGE
        FundInfo fundInfo = new FundInfo("ABC","ABC Fonu","Hisse Senedi Fonu");
        ReturnTypes returnType1 = new ReturnTypes("1 Aylık Getiri");
        ReturnTypes returnType2 = new ReturnTypes("Günlük Getiri");
        LocalDate date = LocalDate.of(2025,04,05);

        FundReturns fr1 = new FundReturns(BigDecimal.valueOf(5.5), date);
        fr1.setFundInfo(fundInfo);
        fr1.setReturnTypes(returnType1);

        FundReturns fr2 = new FundReturns(BigDecimal.valueOf(1.2), date);
        fr2.setFundInfo(fundInfo);
        fr2.setReturnTypes(returnType2);

        List<FundReturns> fundReturnsList = List.of(fr1, fr2);

        // ACT
        FundReturnsDTO result = fundService.convertToDTO(fundReturnsList);

        //ASSERT
        Assertions.assertNotNull(result);
        Assertions.assertEquals("ABC",result.getFundCode());
        Assertions.assertEquals(date,result.getDate());
        Assertions.assertEquals(2,result.getReturns().size());
        Assertions.assertEquals("1 Aylık Getiri",result.getReturns().get(0).getDescription());
        Assertions.assertEquals(BigDecimal.valueOf(5.5),result.getReturns().get(0).getValue());
        Assertions.assertEquals("Günlük Getiri", result.getReturns().get(1).getDescription());
        Assertions.assertEquals(BigDecimal.valueOf(1.2),result.getReturns().get(1).getValue());

    }

    @Test
    void testgetFundDaily() throws Exception{
        //ARRANGE
        LocalDate today = LocalDate.of(2025, 6, 10);
        LocalDate lastWorkingDay = LocalDate.of(2025, 6, 9);

        //Mock veri
        HttpResponse<String> dailyResponse = mock(HttpResponse.class);
        String dailyJson = "{\"data\": [" +
               // JSON'daki tarihleri, test için belirlediğin tarihlerin timestamp'leriyle doldur
            "{\"FONKODU\": \"ABC\", \"TARIH\": " + lastWorkingDay.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli() + ", \"FIYAT\": 100.0}," +
            "{\"FONKODU\": \"ABC\", \"TARIH\": " + today.atStartOfDay(ZoneId.of("Europe/Istanbul")).toInstant().toEpochMilli() + ", \"FIYAT\": 110.0}" +
            "]}";
        when(dailyResponse.body()).thenReturn(dailyJson);

        //ACT
        Map<String,Double> dailyReturns = fundService.getFundDaily(dailyResponse,today);

        //ASSERT
        Assertions.assertNotNull(dailyReturns);
        Assertions.assertEquals(1,dailyReturns.size());
        Assertions.assertEquals(10.0,dailyReturns.get("ABC"));
    }

    @Test
    void processFundReturns_shouldCreateNewRecords_whenRecordsNotFound() {
        // ARRANGE
        JSONObject fundObject = new JSONObject();
        fundObject.put("FONKODU", "ABC");
        fundObject.put("GETIRI1A", 10.5);
        fundObject.put("GETIRI3A", 12.0);
        fundObject.put("GETIRI6A", 15.0);
        fundObject.put("GETIRI1Y", 20.0);

        FundInfo fundInfo = new FundInfo();
        fundInfo.setId(1);
        fundInfo.setFund_code("ABC");

        Map<String, ReturnTypes> returnTypesMap = new HashMap<>();
        returnTypesMap.put("1 Aylık Getiri", new ReturnTypes("1 Aylık Getiri"));
        returnTypesMap.put("3 Aylık Getiri", new ReturnTypes("3 Aylık Getiri"));
        returnTypesMap.put("6 Aylık Getiri", new ReturnTypes("6 Aylık Getiri"));
        returnTypesMap.put("1 Yıllık Getiri", new ReturnTypes("1 Yıllık Getiri"));
        returnTypesMap.put("Günlük Getiri", new ReturnTypes("Günlük Getiri"));

        Map<String, Double> dailyData = new HashMap<>();
        dailyData.put("ABC", 0.5);

        // Repository'den kayıt bulunamamış gibi davranacak
        when(fundReturnRepository.findByFundInfoAndReturnTypesAndDate(any(), any(), any()))
                .thenReturn(Optional.empty());

        // ACT
        List<FundReturns> result = fundService.processFundReturns(fundObject, fundInfo, returnTypesMap, dailyData);

        // ASSERT
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.size());

        FundReturns dailyReturn = result.stream()
                .filter(fr -> "Günlük Getiri".equals(fr.getReturnTypes().getDescription()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(dailyReturn);
        Assertions.assertEquals(BigDecimal.valueOf(0.5), dailyReturn.getReturnValue());
        Assertions.assertEquals(fundInfo, dailyReturn.getFundInfo());
        Assertions.assertEquals(LocalDate.now(), dailyReturn.getDate());
    }

    @Test
    void processFundReturns_shouldUpdateExistingRecords_whenRecordsFound() {
        // ARRANGE
        JSONObject fundObject = new JSONObject();
        fundObject.put("FONKODU", "ABC");
        fundObject.put("GETIRI1A", 10.5);
        fundObject.put("GETIRI3A", 12.0);
        fundObject.put("GETIRI6A", 15.0);
        fundObject.put("GETIRI1Y", 20.0);

        FundInfo fundInfo = new FundInfo();
        fundInfo.setId(1);
        fundInfo.setFund_code("ABC");

        Map<String, ReturnTypes> returnTypesMap = new HashMap<>();
        returnTypesMap.put("1 Aylık Getiri", new ReturnTypes("1 Aylık Getiri"));
        returnTypesMap.put("3 Aylık Getiri", new ReturnTypes("3 Aylık Getiri"));
        returnTypesMap.put("6 Aylık Getiri", new ReturnTypes("6 Aylık Getiri"));
        returnTypesMap.put("1 Yıllık Getiri", new ReturnTypes("1 Yıllık Getiri"));
        returnTypesMap.put("Günlük Getiri", new ReturnTypes("Günlük Getiri"));

        Map<String, Double> dailyData = new HashMap<>();
        dailyData.put("ABC", 0.5);

        //Günlük Getiri için bir veri oluştur
        FundReturns existingDailyRecord = new FundReturns(BigDecimal.valueOf(0.4), LocalDate.now());
        existingDailyRecord.setFundInfo(fundInfo);
        existingDailyRecord.setReturnTypes(returnTypesMap.get("Günlük Getiri"));

        //repository çağırılıdığında var olan günlük getiri dönsün
        when(fundReturnRepository.findByFundInfoAndReturnTypesAndDate(
                any(FundInfo.class),
                eq(returnTypesMap.get("Günlük Getiri")),
                any(LocalDate.class)))
                .thenReturn(Optional.of(existingDailyRecord));

        // ACT
        List<FundReturns> result = fundService.processFundReturns(fundObject, fundInfo, returnTypesMap, dailyData);

        // ASSERT
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.size());

        FundReturns updatedDailyReturn = result.stream()
                .filter(fr -> "Günlük Getiri".equals(fr.getReturnTypes().getDescription()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(updatedDailyReturn);
        Assertions.assertEquals(BigDecimal.valueOf(0.5), updatedDailyReturn.getReturnValue());
        Assertions.assertEquals(existingDailyRecord, updatedDailyReturn);
    }

}
