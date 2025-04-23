package kz.talipovsn.jsoup_micro;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Главная активность приложения для отображения курсов валют с сайта kazfin.info.
 */
public class MainActivity extends AppCompatActivity {

    private TextView textView; // Компонент для отображения данных
    private static final String BASE_URL = "https://kazfin.info/"; // Адрес с котировками

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настройка HTTPS для поддержки TLS 1.2 на старых устройствах
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

        textView = findViewById(R.id.textView);
        fetchCurrencyData(); // Загружаем данные при запуске приложения
    }

    /**
     * Обработчик нажатия кнопки "Обновить".
     */
    public void onClick(View view) {
        fetchCurrencyData();
    }

    /**
     * Запускает загрузку данных о курсах валют в фоновом потоке.
     */
    private void fetchCurrencyData() {
        textView.setText(R.string.not_data); // Устанавливаем текст "Нет данных"
        new FetchCurrencyTask().execute(BASE_URL); // Запускаем AsyncTask
    }

    /**
     * AsyncTask для выполнения парсинга сайта в фоновом потоке.
     */
    private class FetchCurrencyTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                // Загружаем HTML-документ с сайта
                Document doc = Jsoup.connect(urls[0]).timeout(5000).get();
                StringBuilder data = new StringBuilder();

                // Формируем заголовок таблицы
                data.append("Курсы валюты:\n");
                //data.append(String.format("%-12s %-20s %-12s %-12s\n", "Валюта", "Наименование", "Курс", "Изменение"));
                data.append(String.format("%-20s %20s\n", "Валюта", "Курс"));
                //data.append("\n");

                // Ищем все таблицы на странице
                Elements tables = doc.select("table");
                boolean found = false;
                for (Element table : tables) {
                    // Проверяем строки таблицы
                    for (Element row : table.select("tr")) {
                        Elements cols = row.select("td");
                        if (cols.size() >= 4 && isCurrencyRow(cols)) { // Проверяем, что строка содержит данные о валюте
                            String currency = cols.get(0).text(); // Валюта (например, USD)
                            String name = cols.get(1).text();     // Наименование (например, Доллар США)
                            String rate = cols.get(2).text();     // Курс (например, 472.14 тг.)
                            String change = cols.get(3).text();   // Изменение (например, ↓ −0,03)
                            // Форматируем строку таблицы
                            //data.append(String.format("%-12s %-20s %-12s %-12s\n", currency, name, rate, change));
                            data.append(String.format("%-20s %20s\n", currency, rate));
                            found = true;
                        }
                    }
                    if (found) break; // Прерываем, если нашли подходящую таблицу
                }

                if (!found) {
                    return "Ошибка: Таблица с курсами валют не найдена";
                }
                return data.toString().trim();
            } catch (Exception e) {
                e.printStackTrace();
                return "Ошибка: Не удалось загрузить данные с сайта";
            }
        }

        /**
         * Проверяет, является ли строка таблицы строкой с данными о валюте.
         * @param cols Столбцы строки.
         * @return true, если строка содержит валюту (например, USD, EUR).
         */
        private boolean isCurrencyRow(Elements cols) {
            if (cols.isEmpty()) return false;
            String firstCol = cols.get(0).text().toUpperCase();
            // Проверяем, содержит ли первый столбец код валюты (USD, EUR, RUR и т.д.)
            return firstCol.matches("USD|EUR|RUR|GBP|CNY|JPY|KZT|.*");
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                textView.setText(result); // Отображаем данные или сообщение об ошибке
            } else {
                textView.setText(R.string.error); // Ошибка
            }
        }
    }
}