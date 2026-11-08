package kz.talipovsn.jsoup_micro;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * Главная активность приложения для отображения курсов валют НБ РК через Jsoup.
 */
public class MainActivity extends AppCompatActivity {

    private TextView textView; // Компонент для отображения данных
    // Официальный стабильный фид курсов валют Национального Банка РК
    private static final String BASE_URL = "https://nationalbank.kz/rss/rates_all.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
     * AsyncTask для выполнения парсинга в фоновом потоке.
     */
    private class FetchCurrencyTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                // Загружаем документ, явно указывая XML-парсер и User-Agent
                Document doc = Jsoup.connect(urls[0])
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(5000)
                        .parser(Parser.xmlParser())
                        .get();

                StringBuilder data = new StringBuilder("Курсы валют НБ РК:\n\n");

                // Выбираем все элементы <item> с валютами
                Elements items = doc.select("item");

                // Проходим по каждому элементу <item> и извлекаем название валюты и курс
                for (Element item : items) {
                    Element titleElement = item.selectFirst("title");
                    Element descElement = item.selectFirst("description");

                    if (titleElement != null && descElement != null) {
                        String title = titleElement.text(); // Код валюты (USD, EUR, RUB и т.д.)
                        String rate = descElement.text();   // Курс в тенге

                        // Фильтруем основные валюты для наглядного вывода
                        if (title.equals("USD") || title.equals("EUR") || title.equals("RUB") || title.equals("CNY")) {
                            data.append("1 ").append(title).append(" = ").append(rate).append(" KZT\n");
                        }
                    }
                }

                return data.toString().trim();
            } catch (Exception e) {
                e.printStackTrace();
                return "Ошибка: Не удалось загрузить данные с сайта";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                textView.setText(result); // Отображаем полученные курсы или текст ошибки
            } else {
                textView.setText(R.string.error); // Ошибка
            }
        }
    }
}