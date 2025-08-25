package com.dilaykal.service.impl;

import com.dilaykal.dto.FundInfoDTO;
import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.dto.ReturnTypesDTO;
import com.dilaykal.entities.ReturnTypes;
import com.dilaykal.entities.FundInfo;
import com.dilaykal.model.FundPrice;
import com.dilaykal.entities.FundReturns;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
            // Zaman bilgisini bir kez al
            LocalDate currentDate = LocalDate.now(); // Saniye bilgisi dahil, ancak getirende sıfırlanabilir

            HttpResponse<String> monthlyResponse =tefasApiRepository.getMonthlyReturnsData();
            HttpResponse<String> dailyResponse  =tefasApiRepository.getDailyReturnsData();

            Map<String, Double> dailyData = getFundDaily(dailyResponse);
            JSONObject mainObject = new JSONObject(monthlyResponse.body());
            JSONArray dataArray = mainObject.getJSONArray("data");

            // 1. DÜZELTME: ReturnTypes objelerini veritabanında var mı diye kontrol edin
            List<String> types = Arrays.asList("1 Aylık Getiri", "3 Aylık Getiri", "6 Aylık Getiri", "1 Yıllık Getiri", "Günlük Getiri");
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

            // 2. FundInfo ve FundReturns listelerini döngü öncesinde oluşturun
            List<FundInfo> infoDataList = new ArrayList<>();
            List<FundReturns> returnDataList = new ArrayList<>();

            for(int i=0; i<dataArray.length();i++){
                JSONObject fundObject = dataArray.getJSONObject(i);

                String fonKodu = fundObject.getString("FONKODU");
                String fonUnvan = fundObject.getString("FONUNVAN");
                String fonTurAciklama = fundObject.getString("FONTURACIKLAMA");

                Optional<FundInfo> existingFundInfo= fundInfoRepository.findById(fonKodu);
                FundInfo fundInfoData;

                if(existingFundInfo.isPresent()){
                    fundInfoData = existingFundInfo.get();
                }else{
                    fundInfoData = new FundInfo(fonKodu,fonUnvan,fonTurAciklama);
                    fundInfoRepository.save(fundInfoData);
                }

                //Fund info bilgilerinin entities list olarak oluşturulması
                //FundInfo fundInfoData = new FundInfo(fonKodu,fonUnvan,fonTurAciklama);
                //infoDataList.add(fundInfoData);

                // Getiri değerlerini bir diziye al
                double[] getiriler = {
                        fundObject.optDouble("GETIRI1A"),
                        fundObject.optDouble("GETIRI3A"),
                        fundObject.optDouble("GETIRI6A"),
                        fundObject.optDouble("GETIRI1Y"),
                        dailyData.getOrDefault(fonKodu, 0.0)
                };
                // 3. FundReturns nesnelerini oluştururken ilişkileri kurun
                for (int j = 0; j < types.size(); j++) {
                    String typeName = types.get(j);
                    double returnValueDouble = getiriler[j];
                    // ÖNEMLİ DÜZELTME: Sayısal değeri kontrol etme
                    if (Double.isNaN(returnValueDouble) || Double.isInfinite(returnValueDouble)) {
                        // Eğer değer NaN veya Infinity ise, kaydetmeyebilir veya 0.0 atayabilirsiniz.
                        // Bu örnekte, 0.0 atıyoruz
                        returnValueDouble = 0.0;
                        // Alternatif olarak, ilgili FundReturn nesnesini oluşturmaktan vazgeçebilirsiniz.
                        // continue;
                    }
                    // double'ı BigDecimal'a güvenli bir şekilde çevirin
                    BigDecimal returnValue = BigDecimal.valueOf(returnValueDouble);
                    System.out.println("-----------value değer-------- : " + returnValue);
                    Optional<FundReturns> existingRecord = fundReturnRepository.findByFundInfoAndReturnTypesAndDate(
                            fundInfoData,
                            returnTypesMap.get(typeName),
                            currentDate
                    );
                    FundReturns fundReturnData;
                    if (existingRecord.isPresent()) {
                        // 2. Eğer kayıt varsa, mevcut olanı güncelle
                        fundReturnData = existingRecord.get();
                        fundReturnData.setReturnValue(returnValue);
                    } else {
                        // 3. Eğer kayıt yoksa, yeni bir FundReturns nesnesi oluştur
                        fundReturnData = new FundReturns(returnValue, currentDate);
                        fundReturnData.setFundInfo(fundInfoData);
                        fundReturnData.setReturnTypes(returnTypesMap.get(typeName));
                    }

                    returnDataList.add(fundReturnData);

                    // @RequiredArgsConstructor'a uygun constructor kullanımı (varsayım: returnValue ve date gereklidir)
                   // FundReturns fundReturnData = new FundReturns(returnValue,currentDate);

                    // FundReturns nesnesine ilişkileri bağlayın
                    //fundReturnData.setFundInfo(fundInfoData);
                    //fundReturnData.setReturnTypes(returnTypesMap.get(typeName));

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
    public List<FundInfoDTO> getAllFundReturnsFromDb() {
        System.out.println("Veritabanından fon getirileri listeleniyor.");
        List<FundInfo> fundInfos = fundInfoRepository.findAllWithAllDetails();
        // Entity'leri DTO'lara dönüştürme
        return fundInfos.stream().map(this::convertToDto).collect(Collectors.toList());
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
        //fundReturns = fundReturnRepository.findByFundId(fundId);
        return fundReturns.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    // DTO'ya dönüştürme metodu
    private FundReturnsDTO convertToDTO(FundReturns fundReturns) {
        FundReturnsDTO dto = new FundReturnsDTO();
        dto.setId(fundReturns.getId());
        dto.setReturnValue(fundReturns.getReturnValue());
        dto.setDate(fundReturns.getDate());

        // longName'i FundInfo'dan alıp DTO'ya atayın
        if (fundReturns.getFundInfo() != null) {
            dto.setFundId(fundReturns.getFundInfo().getFund_id());
            dto.setLongName(fundReturns.getFundInfo().getLongName());
            dto.setFund_desc(fundReturns.getFundInfo().getFund_desc());
        }

        // FundReturns'ları DTO'lara dönüştürme
        if (fundReturns.getReturnTypes() != null) {

            ReturnTypesDTO rtDto= new ReturnTypesDTO();
            rtDto.setId(fundReturns.getReturnTypes().getId());
            rtDto.setDescription(fundReturns.getReturnTypes().getDescription());
            dto.setReturnTypes(rtDto);
        }

        return dto;
    }

    /*
        @Override
        public List<FundInfo> getFundReturnsByFundId(String fundId, LocalDate startDate, LocalDate endDate) {
            System.out.println("---------date------------ : "+ startDate);
            return fundInfoRepository.findByFundIdAndDate(fundId, startDate, endDate);
        }
    */
    private FundInfoDTO convertToDto(FundInfo fundInfo) {
        FundInfoDTO dto = new FundInfoDTO();
        dto.setFundId(fundInfo.getFund_id());
        dto.setLongName(fundInfo.getLongName());
        dto.setFundDesc(fundInfo.getFund_desc());

        // FundReturns'ları DTO'lara dönüştürme
        if (fundInfo.getFundReturns() != null) {
            dto.setFundReturns(fundInfo.getFundReturns().stream()
                    .map(fr -> {
                        FundReturnsDTO frDto = new FundReturnsDTO();
                        frDto.setId(fr.getId());
                        frDto.setReturnValue(fr.getReturnValue());
                        frDto.setDate(fr.getDate());

                        // ReturnTypes'ı DTO'ya dönüştürme
                        if (fr.getReturnTypes() != null) {
                            ReturnTypesDTO rtDto = new ReturnTypesDTO();
                            rtDto.setId(fr.getReturnTypes().getId());
                            rtDto.setDescription(fr.getReturnTypes().getDescription());
                            frDto.setReturnTypes(rtDto);
                        }
                        return frDto;
                    })
                    .collect(Collectors.toSet()));
        }
        return dto;
    }
    private Map<String,Double> getFundDaily(HttpResponse<String> response){

        JSONObject mainObject = new JSONObject(response.body());

        //data anahtarı ile array dizisine ulaş
        JSONArray dataArray = mainObject.getJSONArray("data");
        // Fon verilerini geçici olarak saklamak için Map kullan
        Map<String, FundPrice> fundDataMap = new HashMap<>();
        Map<String, Double> dailyReturnMap = new HashMap<>();

        // Bugün ve dün için tarih nesnelerini bir kez oluştur
        ZonedDateTime today = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime yesterday = today.minusDays(1);

        for(int i=0; i<dataArray.length();i++){
            JSONObject fundObject = dataArray.getJSONObject(i);

            String fonKodu = fundObject.getString("FONKODU");
            long timestamp = fundObject.getLong("TARIH");
            double fiyat = fundObject.getDouble("FIYAT");

            if(!fundDataMap.containsKey(fonKodu)){
                fundDataMap.put(fonKodu, new FundPrice());
            }

            // Unix zaman damgasını milisaniyeden saniyeye dönüştürmek önemli
            ZonedDateTime date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());

            // Tarihin bugüne ait olup olmadığını kontrol et
            if (date.getYear() == today.getYear() &&
                    date.getMonth() == today.getMonth() &&
                    date.getDayOfMonth() == today.getDayOfMonth()) {
                fundDataMap.get(fonKodu).todayPrice = fiyat;
            }
            // Tarihin düne ait olup olmadığını kontrol et
            else if (date.getYear() == yesterday.getYear() &&
                    date.getMonth() == yesterday.getMonth() &&
                    date.getDayOfMonth() == yesterday.getDayOfMonth()) {
                fundDataMap.get(fonKodu).yesterdayPrice = fiyat;
            }
        }
        // Getiri hesapla
        for (Map.Entry<String, FundPrice> entry : fundDataMap.entrySet()){
            String fonKodu = entry.getKey();
            FundPrice prices = entry.getValue();

            double dailyReturn = ((prices.todayPrice-prices.yesterdayPrice)/ prices.yesterdayPrice)*100;
            dailyReturnMap.put(fonKodu,dailyReturn);

        }
        return dailyReturnMap;
    }

}

