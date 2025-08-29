package com.dilaykal.service.impl;

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
import org.springframework.cglib.core.Local;
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
    public void fetchAndSaveAllFundReturns() {
        try{
            LocalDate currentDate = LocalDate.now();

            HttpResponse<String> monthlyResponse =tefasApiRepository.getMonthlyReturnsData();
            HttpResponse<String> dailyResponse  =tefasApiRepository.getDailyReturnsData();

            //Günlük verilerin tutulduğu map
            Map<String, Double> dailyData = getFundDaily(dailyResponse);

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

            JSONObject mainObject = new JSONObject(monthlyResponse.body());
            JSONArray dataArray = mainObject.getJSONArray("data");

            for(int i=0; i<dataArray.length();i++){
                JSONObject fundObject = dataArray.getJSONObject(i);

                String fonKodu = fundObject.getString("FONKODU");
                String fonUnvan = fundObject.getString("FONUNVAN");
                String fonTurAciklama = fundObject.getString("FONTURACIKLAMA");

                //Fon koduna sahip FundInfo kaydı var mı veritabanında
                Optional<FundInfo> existingFundInfo= fundInfoRepository.findByFundCode(fonKodu);
                FundInfo fundInfoData;
                //Fon veritabanına mevcutsa mevcut kayıt alınır ve değişkene atanır
                if(existingFundInfo.isPresent()){
                    fundInfoData = existingFundInfo.get();
                }else{//fon koduna ait kayıt yoksa yeni nesne oluşturup veritabanına kaydedilir
                    fundInfoData = new FundInfo(fonKodu,fonUnvan,fonTurAciklama);
                    //veritabanına kaydedildikten sonra oluşan id değerine erişebilir
                    fundInfoData = fundInfoRepository.save(fundInfoData);
                }

                //-------- Getiri değerlerini bir diziye al-------
                //günlük getiri değerinin null ve 0 olma durumunu kontrol et
                Double dailyValue = dailyData.get(fonKodu);
                BigDecimal dailyReturn;
                if(dailyValue == null){
                    dailyReturn = null;
                }else{
                    dailyReturn = BigDecimal.valueOf(dailyValue);
                }
                //diziye ekle
                BigDecimal[] getiriler = {
                        fundReturnControl(fundObject,"GETIRI1A"),
                        fundReturnControl(fundObject,"GETIRI3A"),
                        fundReturnControl(fundObject,"GETIRI6A"),
                        fundReturnControl(fundObject,"GETIRI1Y"),
                        dailyReturn
                };

                //FundReturns nesnelerini oluştururken ilişkileri kurun
                for (int j = 0; j < types.size(); j++) {
                    String typeName = types.get(j);
                    BigDecimal returnValue = getiriler[j];

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

            fundReturnRepository.saveAll(returnDataList);
            System.out.println("Tüm veriler başarıyla kaydedildi.");

        } catch (Exception e) {
            System.err.println("API'den veri alınırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Veri kaydetme işlemi başarısız oldu.", e); // Hatayı yukarı fırlatın
        }
    }

    private BigDecimal fundReturnControl(JSONObject jsonObject, String key){
        if(jsonObject.has(key)){

            //anahtar varsa o değerin null kotrolü
            if(jsonObject.isNull(key)){
                return null;
            }
            try{
                double doubleValue = jsonObject.getDouble(key);
                return BigDecimal.valueOf(doubleValue);
            }catch (Exception e){
                return null; //değer sayı değilse null atanacak
            }
        }
        return null; //anahtar yoksa nulll döndür
    }

    @Override
    public List<FundReturnsDTO> getAllFundReturns() {
        System.out.println("Veritabanından fon getirileri listeleniyor.");
        List<FundReturns> fundReturns = fundReturnRepository.findAllWithAllDetails();

        // Verileri fon koduna göre grupla
        Map<String, List<FundReturns>> groupedByFund = fundReturns.stream()
                .collect(Collectors.groupingBy(fr -> fr.getFundInfo().getFund_code()));

        // Her fon grubu için tek bir DTO oluştur
        List<FundReturnsDTO> dtoList = new ArrayList<>();

        groupedByFund.forEach((fundId, returnsForFund) -> {
            // convertToDTO metodu, bir fonun tüm getirilerini birleştirir
            dtoList.add(convertToDTO(returnsForFund));
        });

        return dtoList;
    }

    @Override
    public List<FundReturnsDTO> getByFundCode(String fundCode, LocalDate startDate, LocalDate endDate) {
        List<FundReturns> fundReturns;
        if (startDate != null && endDate != null) {
            // Tarih aralığı belirtilmişse, bu metodu çağır
            fundReturns = fundReturnRepository.findByFundCodeAndDate(fundCode, startDate, endDate);
        } else {
            // Tarih aralığı belirtilmemişse, sadece bugünün verilerini getir
            fundReturns = fundReturnRepository.findByFundCode(fundCode, LocalDate.now());
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
        ArrayList<ReturnTypesDTO> returnDataList = new ArrayList<>();

        int index=0;
        for (FundReturns fr : fundReturnsList){
            // genel bilgiler aynı olduğu için tek sefer alınıyor
            if(index==0){
                if (fr.getFundInfo() != null) {
                    dto.setFundCode(fr.getFundInfo().getFund_code());
                    dto.setLongName(fr.getFundInfo().getLongName());
                    dto.setFund_desc(fr.getFundInfo().getFund_desc());
                    dto.setDate(fr.getDate());
                }
            }
            // getiri bilgileri
            if(fr.getReturnTypes()!=null) {
                ReturnTypesDTO returnTypeData = new ReturnTypesDTO();
                returnTypeData.setDescription(fr.getReturnTypes().getDescription());
                returnTypeData.setValue(fr.getReturnValue());

                returnDataList.add(returnTypeData);
            }
            index++;
        }
        dto.setReturns(returnDataList);
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

            Double dailyReturn;
            if(yesterdayPrice == 0){
                dailyReturn=null;
            }else{
                dailyReturn = ((todayPrice-yesterdayPrice)/ yesterdayPrice)*100;
            }

            dailyReturnMap.put(fonKodu,dailyReturn);

        }
        return dailyReturnMap;
    }

}

