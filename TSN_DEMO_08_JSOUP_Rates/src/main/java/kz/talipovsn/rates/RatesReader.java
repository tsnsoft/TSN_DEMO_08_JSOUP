package kz.talipovsn.rates;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * Утилитный класс для получения котировок валют с официального фида НБ РК.
 */
public class RatesReader {

    // Стабильный официальный адрес с котировками Национального Банка РК
    private static final String BASE_URL = "https://nationalbank.kz/rss/rates_all.xml";

    /**
     * Парсинг котировок с сервера НБ РК. Возвращает отформатированную строку с данными
     * или null при ошибке.
     *
     * @return Строка с данными о курсах валют или null при ошибке.
     */
    public static String getRatesData() {
        StringBuilder data = new StringBuilder();
        try {
            // Загружаем документ с использованием XML-парсера Jsoup
            Document doc = Jsoup.connect(BASE_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(5000)
                    .parser(Parser.xmlParser())
                    .get();

            data.append("Курсы валют НБ РК:\n\n");
            data.append(String.format("%-8s %-18s %-12s %-10s\n", "Валюта", "Наименование", "Курс", "Изменение"));
            data.append("----------------------------------------------------\n");

            // Выбираем все элементы <item> с данными о валютах
            Elements items = doc.select("item");
            boolean found = false;

            for (Element item : items) {
                String code = getTagText(item, "title");       // Код валюты (например, USD)
                String rate = getTagText(item, "description"); // Курс (например, 495.20)
                String change = getTagText(item, "change");    // Изменение (например, +1.15)
                String index = getTagText(item, "index");      // Тренд (UP / DOWN)

                if (isTargetCurrency(code)) {
                    String name = getCurrencyName(code);
                    String arrow = "UP".equalsIgnoreCase(index) ? "↑ " : ("DOWN".equalsIgnoreCase(index) ? "↓ " : "");

                    data.append(String.format("%-8s %-18s %-12s %-10s\n",
                            code, name, rate + " тг.", arrow + change));
                    found = true;
                }
            }

            if (!found) {
                return null; // Данные о нужных валютах не найдены
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Ошибка загрузки или парсинга
        }
        return data.toString().trim(); // Возвращаем результат
    }

    /**
     * Безопасное получение текста из тега внутри элемента.
     */
    private static String getTagText(Element parent, String tagName) {
        Element el = parent.selectFirst(tagName);
        return (el != null) ? el.text().trim() : "";
    }

    /**
     * Проверяет, входит ли валюта в список отображаемых (чтобы не выводить все 39 валют подряд).
     *
     * @param code Трехбуквенный код валюты.
     * @return true, если валюту нужно отобразить.
     */
    private static boolean isTargetCurrency(String code) {
        // Здесь оставляем строго основные мировые и региональные валюты:
        return code.matches("USD|EUR|RUB|CNY|GBP|JPY|KGS|UZS|TRY");
    }

    /**
     * Возвращает русское наименование валюты по её коду.
     */
    private static String getCurrencyName(String code) {
        switch (code) {
            case "USD": return "Доллар США";
            case "EUR": return "Евро";
            case "RUB": return "Российский рубль";
            case "CNY": return "Китайский юань";
            case "GBP": return "Фунт стерлингов";
            case "JPY": return "Японская иена";
            case "KGS": return "Кыргызский сом";
            case "UZS": return "Узбекский сум";
            case "TRY": return "Турецкая лира";
            default:    return code;
        }
    }
}