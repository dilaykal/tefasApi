
package com.dilaykal.staj_proje;

import com.dilaykal.service.IFundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {
    @Autowired
    private IFundService fundService;

    @Scheduled(cron = "0 0 18 * * *")
    public void scheduledDataFetch(){
        System.out.println("Zamanlanmış veri yükleme işlemi başlatılıyor..");
        try {
            // API'den veri çekme ve veritabanına kaydetme işlemi
            fundService.fetchAndSaveAllFundReturns();
            System.out.println("Veri yükleme işlemi tamamlandı.");
        } catch (Exception e) {
            System.err.println("Veri yükleme işlemi sırasında bir hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }
}