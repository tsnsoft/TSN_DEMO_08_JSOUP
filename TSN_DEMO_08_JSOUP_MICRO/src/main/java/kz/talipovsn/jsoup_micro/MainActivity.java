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
    private static final String BASE_URL = "https://tou.edu.kz/ru/"; // Адрес с котировками

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

                String selector = "#content > div:nth-child(18) > div > div > div > div:nth-child(1) > div:nth-child(2)";
                // String selector = "#content .phone__text";
                //String selector = "#content div div.phone__text";

                Element element = doc.selectFirst(selector);
                //Element element = doc.select(selector).first();

                data.append("Call Center:\n");
                data.append(element.text());
                return data.toString().trim();
            } catch (Exception e) {
                e.printStackTrace();
                return "Ошибка: Не удалось загрузить данные с сайта";
            }
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