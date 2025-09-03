# TEFAS Fon Veri Analiz Uygulaması

Bu proje, Türkiye Elektronik Fon Alım Satım Platformu (TEFAS) üzerinden günlük fon verilerini otomatik olarak çeken, bu verileri işleyerek fon getirilerini hesaplayan ve sonuçları belirli zaman aralıkları için ayrı biçimde MSSQL veritabanında depolayan bir Spring Boot API uygulamasıdır.

## İçindekiler

- [Teknolojiler](#-teknolojiler)
- [Özellikler](#-özellikler)
- [Ön Koşullar](#-ön-koşullar)
- [Kurulum](#-kurulum)
- [API Uç Noktaları](#-api-uç-noktaları)
- [Testler](#-testler)

---

## Teknolojiler

Bu proje aşağıdaki temel teknolojiler kullanılarak geliştirilmiştir:

- **Spring Boot:** Uygulamanın temel çatısıdır.
- **Java:** Projenin programlama dilidir.
- **Gradle:** Bağımlılık yönetimi ve derleme aracı olarak kullanılmıştır.
- **MS SQL Server:** Uygulamanın veritabanıdır.
- **Spring Data JPA:** Veritabanı işlemleri için ORM (Object-Relational Mapping) katmanı sağlar.
- **TEFAS API:** Fon verilerini almak için kullanılan harici veri kaynağıdır.
- **Lombok:** Tekrar eden standart kodları azaltmak için kullanılmıştır.
- **Mockito & JUnit 5:** Birim testleri yazmak için kullanılmıştır.

---

## Özellikler

Uygulamanın sağladığı temel işlevler:

-   **Günlük Veri Çekme:** TEFAS API'sinden fon kodlarına ait günlük verileri otomatik olarak çeker.
-   **Getiri Hesaplama:** Fonların çekilen fiyat verilerini kullanarak günlük getirilerini hesaplar.
-   **Veritabanı Depolama:** Hesaplanan fon getirilerini kalıcı olarak bir veritabanına kaydeder.
-   **RESTful API:** Hesaplanan getirilere erişim için RESTful API uç noktaları sunar.

---

## Ön Koşullar

Projeyi çalıştırmadan önce aşağıdaki yazılımların sisteminizde kurulu olduğundan emin olun:

-   **Java Development Kit (JDK):** Proje, **Java 23** veya üstü bir sürüm gerektirir. [Open JDK](https://openjdk.org/install/) resmi sitesinden indirebilirsiniz.
-   **MS SQL Server:** Veritabanı işlemleri için MS SQL Server'ın yerel veya uzak bir kurulumuna ihtiyacınız vardır.
-   **SQL Server Management Studio (SSMS):** Veritabanınızı yönetmek, tabloları görüntülemek ve bağlantı bilgilerini kontrol etmek için SSMS'i [resmi Microsoft sitesinden](https://docs.microsoft.com/en-us/sql/ssms/download-sql-server-management-studio-ssms?view=sql-server-ver16) indirebilirsiniz.

---

## Kurulum

Projeyi yerel makinenizde çalıştırmak için aşağıdaki adımları takip edin:

1.  Bu depoyu klonlayın:
    ```bash
    git clone https://github.com/dilaykal/tefasApi.git
    ```
2.  Proje dizinine gidin:
    ```bash
    cd tefasApi
    ```
3.  `application.properties` veya `application.yml` dosyasını `src/main/resources` klasörü altında oluşturun ve veritabanı bağlantı bilgilerinizi girin:

    ```properties
    # MS SQL Server Veritabanı Ayarları
    spring.datasource.url=jdbc:sqlserver://[sunucu-adresi]:[port];database=[veritabanı-adı]
    spring.datasource.username=[kullanıcı-adı]
    spring.datasource.password=[şifre]

    # JPA Ayarları
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect
    ```

    *([sunucu-adresi] ve [port] bilgilerini MS SQL Server Management Studio (SSMS) üzerinden edinebilirsiniz.)*

4.  Projeyi Gradle ile derleyin ve çalıştırın:
    ```bash
    ./gradlew bootRun
    ```

---

## API Uç Noktaları

Uygulama tarafından sağlanan temel API uç noktaları aşağıdadır.

-   **[GET] /api/funds/{fonKodu}:** Belirli bir fon koduna ait günlük getirileri listeler.
    -   **Örnek:** `/api/funds/ABC`
-   **[GET] /api/funds:** Veritabanında kayıtlı olan tüm fon getirilerini getirir.
    -   **Örnek:** `/api/funds`
-   **[PUT] /api/funds/{fonKodu}** Belirli bir fon koduna ait getiri değerlerini günceller.
    -   **Örnek:** `/api/funds/ABC`

---

## Testler

Proje, birim testleri içermektedir. Testleri çalıştırmak için Gradle komutunu kullanın:

```bash
./gradlew test