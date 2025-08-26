
package com.dilaykal.staj_proje;

import com.dilaykal.service.IFundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private IFundService fundService;

    @Override
    public void run(String... args) {
        System.out.println("Uygulama başladığında veri yükleme işlemi başlatılıyor...");
        try {
            // API'den veri çekme ve veritabanına kaydetme işlemi burada başlar.
            //fundService.fetchAndSaveAllFundReturns();
            System.out.println("Veri yükleme işlemi tamamlandı.");
        } catch (Exception e) {
            System.err.println("Veri yükleme işlemi sırasında bir hata oluştu: " + e.getMessage());
            // Hata detayları için
            e.printStackTrace();
        }
    }
}