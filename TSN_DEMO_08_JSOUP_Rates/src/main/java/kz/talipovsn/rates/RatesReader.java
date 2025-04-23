package kz.talipovsn.rates;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Утилитный класс для получения котировок валют с сайта kazfin.info.
 */
public class RatesReader {

    private static final String BASE_URL = "https://kazfin.info/"; // Адрес с котировками

    /**
     * Парсинг котировок из HTML-страницы сайта. Возвращает отформатированную строку с данными
     * или null при ошибке.
     * @return Строка с данными о курсах валют или null при ошибке.
     */
    public static String getRatesData() {
        // Настройка HTTPS для поддержки TLS 1.2
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }

        StringBuilder data = new StringBuilder();
        try {
            // Загружаем HTML-документ с сайта
            Document doc = Jsoup.connect(BASE_URL).timeout(5000).get();
            data.append("Курсы валюты:\n");
            data.append(String.format("%-12s %-20s %-12s %-12s\n", "Валюта", "Наименование", "Курс", "Изменение"));
            data.append("\n");

            // Ищем все таблицы на странице
            Elements tables = doc.select("table");
            boolean found = false;
            for (Element table : tables) {
                // Проверяем строки таблицы
                for (Element row : table.select("tr")) {
                    Elements cols = row.select("td");
                    if (cols.size() >= 4 && isCurrencyRow(cols)) { // Проверяем, что строка содержит валютные данные
                        String currency = cols.get(0).text(); // Валюта (например, USD)
                        String name = cols.get(1).text();     // Наименование (например, Доллар США)
                        String rate = cols.get(2).text();     // Курс (например, 472.14 тг.)
                        String change = cols.get(3).text();   // Изменение (например, ↓ −0,03)
                        // Форматируем строку таблицы
                        data.append(String.format("%-12s %-20s %-12s %-12s\n",
                                currency, name, rate, change));
                        found = true;
                    }
                }
                if (found) break; // Прерываем, если нашли подходящую таблицу
            }

            if (!found) {
                return null; // Таблица с курсами валют не найдена
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Ошибка загрузки или парсинга
        }
        return data.toString().trim(); // Возвращаем результат
    }

    /**
     * Проверяет, является ли строка таблицы строкой с данными о валюте.
     * @param cols Столбцы строки.
     * @return true, если строка содержит валюту (например, USD, EUR).
     */
    private static boolean isCurrencyRow(Elements cols) {
        if (cols.isEmpty()) return false;
        String firstCol = cols.get(0).text().toUpperCase();
        // Проверяем, содержит ли первый столбец код валюты (USD, EUR, RUR и т.д.)
        return firstCol.matches("USD|EUR|RUR|GBP|CNY|JPY|KZT|.*");
    }
}