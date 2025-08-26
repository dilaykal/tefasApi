package com.dilaykal.service.impl;

import com.dilaykal.dto.FundInfoDTO;
import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.dto.ReturnTypesDTO;
import com.dilaykal.entities.ReturnTypes;
import com.dilaykal.entities.FundInfo;
import com.dilaykal.model.FundPrice;
import com.dilaykal.entities.FundReturns;
import com.dilaykal.model.DateUtils;

import com.dilaykal.repository.IFundInfoRepository;
import com.dilaykal.repository.IFundReturnRepository;
import com.dilaykal.repository.IReturnTypeRepository;
import com.dilaykal.repository.ITefasApiRepository;
import com.dilaykal.service.IFundService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FundServiceImpl implements IFundService {
    @Autowired
    private ITefasApiRepository tefasApiRepository;
    @Autowired
    private IFundInfoRepository fundInfoRepository;
    @Autowired
    private IFundReturnRepository fundReturnRepository;
    @Autowired
    private IReturnTypeRepository returnTypeRepository;

    @Override
    @Transactional
    //@Scheduled(cron = "0 0 18 * * ?")
    public void fetchAndSaveAllFundReturns() {
        try{
            LocalDate currentDate = LocalDate.now();

            HttpResponse<String> monthlyResponse =tefasApiRepository.getMonthlyReturnsData();
            HttpResponse<String> dailyResponse  =tefasApiRepository.getDailyReturnsData();

            //Günlük verilerin tutulduğu map
            Map<String, Double> dailyData = getFundDaily(dailyResponse);

            JSONObject mainObject = new JSONObject(monthlyResponse.body());
            JSONArray dataArray = mainObject.getJSONArray("data");

            //ReturnTypes objelerini veritabanında var mı diye kontrol et
            List<String> types = Arrays.asList("1 Aylık Getiri", "3 Aylık Getiri", "6 Aylık Getiri", "1 Yıllık Getiri", "Günlük Getiri");
            //Açıklamaları ile kaydedilecek returnTypes mapi
            Map<String, ReturnTypes> returnTypesMap = new HashMap<>();

            // Eğer veritabanında ReturnTypes yoksa, onları oluştur ve kaydet
            if (returnTypeRepository.count() == 0) {
                System.out.println("ReturnTypes tablosu boş, veriler ekleniyor.");
                List<ReturnTypes> returnTypesToSave = new ArrayList<>();
                for (String type_desc : types) {
                    ReturnTypes returnTypesData = new ReturnTypes(type_desc);
                    returnTypesToSave.add(returnTypesData);
                    returnTypesMap.put(type_desc, returnTypesData);
                }
                returnTypeRepository.saveAll(returnTypesToSave);
            } else {
                // Eğer veriler zaten varsa, veritabanından çekip map'e doldur
                System.out.println("ReturnTypes tablosu dolu, mevcut veriler kullanılıyor.");
                List<ReturnTypes> existingTypes = returnTypeRepository.findAll();
                for (ReturnTypes type : existingTypes) {
                    returnTypesMap.put(type.getDescription(), type);
                }
            }

            //FundReturns listelerini döngü öncesinde oluşturun
            List<FundReturns> returnDataList = new ArrayList<>();

            for(int i=0; i<dataArray.length();i++){
                JSONObject fundObject = dataArray.getJSONObject(i);

                String fonKodu = fundObject.getString("FONKODU");
                String fonUnvan = fundObject.getString("FONUNVAN");
                String fonTurAciklama = fundObject.getString("FONTURACIKLAMA");

                //Fon koduna sahip FundInfo kaydı var mı veritabanında
                Optional<FundInfo> existingFundInfo= fundInfoRepository.findById(fonKodu);
                FundInfo fundInfoData;
                //Fon veritabanına mevcutsa mevcut kayıt alınır ve değişkene atanır
                if(existingFundInfo.isPresent()){
                    fundInfoData = existingFundInfo.get();
                }else{//fon koduna ait kayıt yoksa yeni nesne oluşturup veritabanına kaydedilir
                    fundInfoData = new FundInfo(fonKodu,fonUnvan,fonTurAciklama);
                    fundInfoRepository.save(fundInfoData);
                }

                // Getiri değerlerini bir diziye al
                double[] getiriler = {
                        fundObject.optDouble("GETIRI1A"),
                        fundObject.optDouble("GETIRI3A"),
                        fundObject.optDouble("GETIRI6A"),
                        fundObject.optDouble("GETIRI1Y"),
                        dailyData.getOrDefault(fonKodu, 0.0) //dailyData mapinden günlük getiri değerleri alınır

                };
                //FundReturns nesnelerini oluştururken ilişkileri kurun
                for (int j = 0; j < types.size(); j++) {
                    String typeName = types.get(j);
                    double returnValueDouble = getiriler[j];
                    //Sayısal değeri kontrol etme
                    if (Double.isNaN(returnValueDouble) || Double.isInfinite(returnValueDouble)) {
                        returnValueDouble = 0.0;
                    }
                    BigDecimal returnValue = BigDecimal.valueOf(returnValueDouble);

                    Optional<FundReturns> existingRecord = fundReturnRepository.findByFundInfoAndReturnTypesAndDate(
                            fundInfoData,
                            returnTypesMap.get(typeName),
                            currentDate
                    );

                    FundReturns fundReturnData;
                    if (existingRecord.isPresent()) {
                        //Eğer kayıt varsa, mevcut olanı güncelle
                        fundReturnData = existingRecord.get();
                        fundReturnData.setReturnValue(returnValue);
                    } else {
                        //Eğer kayıt yoksa, yeni bir FundReturns nesnesi oluştur
                        fundReturnData = new FundReturns(returnValue, currentDate);
                        fundReturnData.setFundInfo(fundInfoData);
                        fundReturnData.setReturnTypes(returnTypesMap.get(typeName));
                    }

                    // Listeye ekle
                    returnDataList.add(fundReturnData);
                }

            }
            //fundInfoRepository.saveAll(infoDataList);
            fundReturnRepository.saveAll(returnDataList);
            System.out.println("Tüm veriler başarıyla kaydedildi.");

        } catch (Exception e) {
            System.err.println("API'den veri alınırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Veri kaydetme işlemi başarısız oldu.", e); // Hatayı yukarı fırlatın
        }
    }

    @Override
    public List<FundReturnsDTO> getAllFundReturns() {
        System.out.println("Veritabanından fon getirileri listeleniyor.");
        List<FundReturns> fundReturns = fundReturnRepository.findAllWithAllDetails();

        // Verileri fon koduna göre grupla
        Map<String, List<FundReturns>> groupedByFund = fundReturns.stream()
                .collect(Collectors.groupingBy(fr -> fr.getFundInfo().getFund_id()));

        // Her fon grubu için tek bir DTO oluştur
        List<FundReturnsDTO> dtoList = new ArrayList<>();

        groupedByFund.forEach((fundId, returnsForFund) -> {
            // convertToDTO metodu, bir fonun tüm getirilerini birleştirir
            dtoList.add(convertToDTO(returnsForFund));
        });

        return dtoList;

    }

    @Override
    @Transactional(readOnly = true)//metot içindeki veritabanı işlemlerinin sadece okuma amaçlı olduğunu belirtir. Bu, performans iyileştirmesi sağlar
    public List<FundReturnsDTO> getByFundId(String fundId, LocalDate startDate, LocalDate endDate) {
        List<FundReturns> fundReturns;
        if (startDate != null && endDate != null) {
            // Tarih aralığı belirtilmişse, bu metodu çağır
            fundReturns = fundReturnRepository.findByFundIdAndDate(fundId, startDate, endDate);
        } else {
            // Tarih aralığı belirtilmemişse, sadece bugünün verilerini getir
            fundReturns = fundReturnRepository.findByFundId(fundId, LocalDate.now());
        }

        // Gelen verileri tarihe göre gruplar
        Map<LocalDate, List<FundReturns>> groupedByDate = fundReturns.stream()
                .collect(Collectors.groupingBy(FundReturns::getDate));

        // 3. Her bir tarih grubu için tek bir DTO oluşturur
        List<FundReturnsDTO> dtoList = new ArrayList<>();

        groupedByDate.forEach((date, returnsForDate) -> {
            // Her bir tarih grubu için convertToDTO metodunu çağırır
            dtoList.add(convertToDTO(returnsForDate));
        });

        return dtoList;
    }

    // DTO'ya dönüştürme metodu
    private FundReturnsDTO convertToDTO(List<FundReturns> fundReturnsList) {
        FundReturnsDTO dto = new FundReturnsDTO();
        int index=0;
        for (FundReturns fr : fundReturnsList){
            if(index==0){
                if (fr.getFundInfo() != null) {
                    dto.setFundId(fr.getFundInfo().getFund_id());
                    dto.setLongName(fr.getFundInfo().getLongName());
                    dto.setFund_desc(fr.getFundInfo().getFund_desc());
                    dto.setDate(fr.getDate());
                }
            }
            if(fr.getReturnTypes()!=null) {
                Integer returnTypeId = fr.getReturnTypes().getId();
                BigDecimal returnValue = fr.getReturnValue();
                if (returnTypeId.equals(1)) {
                    dto.setGetiri1A(returnValue);
                } else if (returnTypeId.equals(2)) {
                    dto.setGetiri3A(returnValue);
                } else if (returnTypeId.equals(3)) {
                    dto.setGetiri6A(returnValue);
                } else if (returnTypeId.equals(4)) {
                    dto.setGetiri1Y(returnValue);
                } else if (returnTypeId.equals(5)) {
                    dto.setGetiri_gunluk(returnValue);
                }
            }
            index++;
        }
        return dto;
    }

    private Map<String,Double> getFundDaily(HttpResponse<String> response){
        System.out.println("API'den gelen günlük getiri ham verisi: " + response.body());

        JSONObject mainObject = new JSONObject(response.body());

        //data anahtarı ile array dizisine ulaş
        JSONArray dataArray = mainObject.getJSONArray("data");
        // Fon verilerini geçici olarak saklamak için Map kullan
        Map<String, FundPrice> fundDataMap = new HashMap<>();
        Map<String, Double> dailyReturnMap = new HashMap<>();
        // Geçici olarak tüm fonları ve tarihe göre fiyatlarını tutacak bir harita
        Map<String, Map<LocalDate, Double>> pricesByDate = new HashMap<>();

        // Bugün ve son işlem gününü bul
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate lastWorkingDay = today.minusDays(1);

        while (!DateUtils.isWorkingDay(lastWorkingDay)) {
            lastWorkingDay = lastWorkingDay.minusDays(1);
        }


        for(int i=0; i<dataArray.length();i++) {
            JSONObject fundObject = dataArray.getJSONObject(i);

            String fonKodu = fundObject.getString("FONKODU");
            long timestamp = fundObject.getLong("TARIH");
            double fiyat = fundObject.getDouble("FIYAT");

            LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();

            // Her fon için tarih ve fiyat haritasını oluştur veya al
            pricesByDate.putIfAbsent(fonKodu, new HashMap<>());
            pricesByDate.get(fonKodu).put(date, fiyat);

        }
        // Getiri hesapla
        for (Map.Entry<String, Map<LocalDate, Double>> entry : pricesByDate.entrySet()){
            String fonKodu = entry.getKey();
            Map<LocalDate, Double> prices = entry.getValue();
            double todayPrice = prices.getOrDefault(today, 0.0);
            double yesterdayPrice = prices.getOrDefault(lastWorkingDay, 0.0);

            double dailyReturn;
            if(yesterdayPrice == 0){
                dailyReturn=0;
            }else{
                dailyReturn = ((todayPrice-yesterdayPrice)/ yesterdayPrice)*100;
            }

            dailyReturnMap.put(fonKodu,dailyReturn);

        }
        return dailyReturnMap;
    }

}

