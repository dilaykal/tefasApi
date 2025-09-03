package com.dilaykal.service.impl;

import com.dilaykal.dto.FundReturnsDTO;
import com.dilaykal.dto.ReturnDataDTO;
import com.dilaykal.entities.ReturnTypes;
import com.dilaykal.entities.FundInfo;
import com.dilaykal.entities.FundReturns;
import com.dilaykal.model.DateUtils;

import com.dilaykal.repository.IFundInfoRepository;
import com.dilaykal.repository.IFundReturnRepository;
import com.dilaykal.repository.IReturnTypeRepository;
import com.dilaykal.service.IFetchDataService;
import com.dilaykal.service.IFundService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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
    private IFetchDataService tefasApiRepository;
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
            HttpResponse<String> monthlyResponse =tefasApiRepository.getMonthlyReturnsData();
            HttpResponse<String> dailyResponse  =tefasApiRepository.getDailyReturnsData();
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            //Günlük verilerin tutulduğu map
            Map<String, Double> dailyData = getFundDaily(dailyResponse,today);
            //ReturnTypes'ı kontrol etmek için dönen map
            Map<String, ReturnTypes> returnTypesMap = initializeReturnTypes();

            JSONObject mainObject = new JSONObject(monthlyResponse.body());
            JSONArray dataArray = mainObject.getJSONArray("data");

            //FundReturns listesini kaydetmek için liste oluştur
            List<FundReturns> returnsToSave = new ArrayList<>();
            //Json yanıtını dolaş ve verileri al
            for(int i=0; i<dataArray.length();i++){
                JSONObject fundObject = dataArray.getJSONObject(i);
                //FundInfo ile ilgili bilfileri alır ve kaydeder
                FundInfo fundInfo = processFundInfo(fundObject);

                //getiri verilerini FundReturns listesine dönüştürür
                List<FundReturns> fundReturns = processFundReturns(fundObject, fundInfo, returnTypesMap, dailyData);
                returnsToSave.addAll(fundReturns);

            }
            // verileri kaydet
            fundReturnRepository.saveAll(returnsToSave);
            System.out.println("Tüm veriler başarıyla kaydedildi.");

        } catch (Exception e) {
            System.err.println("API'den veri alınırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Veri kaydetme işlemi başarısız oldu.", e);
        }
    }

    protected Map<String, ReturnTypes> initializeReturnTypes(){
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
        return returnTypesMap;
    }

    //FundInfo ile ilgili bilgileri json yanıtından alır ve kaydeder
    protected FundInfo processFundInfo(JSONObject fundObject){
        String fonKodu = fundObject.getString("FONKODU");
        String fonUnvan = fundObject.getString("FONUNVAN");
        String fonTurAciklama = fundObject.getString("FONTURACIKLAMA");

        //Fon koduna sahip FundInfo kaydı var mı veritabanında
        Optional<FundInfo> existingFundInfo= fundInfoRepository.findByFundCode(fonKodu);
        //Fon veritabanına mevcutsa mevcut kayıt alınır ve değişkene atanır
        if(existingFundInfo.isPresent()){
            return existingFundInfo.get();
        }else{//fon koduna ait kayıt yoksa yeni nesne oluşturup veritabanına kaydedilir
            FundInfo newfundInfo = new FundInfo(fonKodu,fonUnvan,fonTurAciklama);
            //veritabanına kaydedildikten sonra oluşan id değerine erişebilir
            return fundInfoRepository.save(newfundInfo);
        }
    }

    //FUND_RETURNS tablosu diğer tablolarla bağlantılı bilgiler içerdiği için bu fonksiyonda parametre olarak fundınfo ve returnTypesMap i alıyoruz.
    //Veritabanında bu tabloları kontrol edip doldurma işlemini yapacak.
    //Bu fonksiyon hem getiri değerlerini json yanıtından alıp kaydediyor.
    //Aynı zamanda tabloya kaydederken fund_ınfo ve return_types tablolarını ile de ilişki kuruyor.
    protected List<FundReturns> processFundReturns(JSONObject fundObject, FundInfo fundInfo, Map<String, ReturnTypes> returnTypesMap, Map<String, Double> dailyData){
        LocalDate currentDate = LocalDate.now();
        //FundReturns listelerini döngü öncesinde oluşturun
        List<FundReturns> returnsList = new ArrayList<>();
        List<String> types = Arrays.asList("1 Aylık Getiri", "3 Aylık Getiri", "6 Aylık Getiri", "1 Yıllık Getiri", "Günlük Getiri");

        //-- Getiri değerlerini bir diziye al--
        //günlük getiri değerinin null ve 0 olma durumunu kontrol et
        Double dailyValue = dailyData.get(fundObject.getString("FONKODU"));
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
                    fundInfo,
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
                fundReturnData.setFundInfo(fundInfo);
                fundReturnData.setReturnTypes(returnTypesMap.get(typeName));
            }

            returnsList.add(fundReturnData);
        }
        return returnsList;
    }

    protected BigDecimal fundReturnControl(JSONObject jsonObject, String key){
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
        return null; //anahtar olmadığı durum
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
            // Tarih aralığı belirtilmişse
            fundReturns = fundReturnRepository.findByFundCodeAndDate(fundCode, startDate, endDate);
        } else {
            // Tarih aralığı belirtilmemişse
            fundReturns = fundReturnRepository.findByFundCode(fundCode, LocalDate.now());
        }

        // Gelen verileri tarihe göre grupla
        Map<LocalDate, List<FundReturns>> groupedByDate = fundReturns.stream()
                .collect(Collectors.groupingBy(FundReturns::getDate));

        // Her bir tarih grubu için tek bir DTO oluştur
        List<FundReturnsDTO> dtoList = new ArrayList<>();

        groupedByDate.forEach((date, returnsForDate) -> {
            // Her bir tarih grubu için convertToDTO metodunu çağır
            dtoList.add(convertToDTO(returnsForDate));
        });

        return dtoList;
    }

    @Override
    public void updatedFundReturns(String fundCode, LocalDate date, List<ReturnDataDTO> updatedReturns) {
        for(ReturnDataDTO updatedReturn : updatedReturns){
            FundReturns existingFund = fundReturnRepository.findByFundCodeAndReturnType(fundCode,date,updatedReturn.getDescription()).orElseThrow(()-> new RuntimeException("Belitrilen fona ait bilgiler bulunamadı." + updatedReturn.getDescription()));
            existingFund.setReturnValue(updatedReturn.getValue());
            fundReturnRepository.save(existingFund);

        }
    }

    protected FundReturnsDTO convertToDTO(List<FundReturns> fundReturnsList) {
        FundReturnsDTO dto = new FundReturnsDTO();
        ArrayList<ReturnDataDTO> returnDataList = new ArrayList<>();

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
                ReturnDataDTO returnTypeData = new ReturnDataDTO();
                returnTypeData.setDescription(fr.getReturnTypes().getDescription());
                returnTypeData.setValue(fr.getReturnValue());

                returnDataList.add(returnTypeData);
            }
            index++;
        }
        dto.setReturns(returnDataList);
        return dto;
    }

    protected Map<String,Double> getFundDaily(HttpResponse<String> response, LocalDate today){
        System.out.println("API'den gelen günlük getiri ham verisi: " + response.body());

        JSONObject mainObject = new JSONObject(response.body());

        //array dizisine ulaş
        JSONArray dataArray = mainObject.getJSONArray("data");

        Map<String, Double> dailyReturnMap = new HashMap<>();
        // Geçici olarak tüm fonları ve tarihe göre fiyatlarını tutacak bir map
        Map<String, Map<LocalDate, Double>> pricesByDate = new HashMap<>();

        //son işlem gününü bul
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

