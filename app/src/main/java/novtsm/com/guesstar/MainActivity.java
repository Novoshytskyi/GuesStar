package novtsm.com.guesstar;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private ImageView imageViewStar;
    private Button button0;
    private Button button1;
    private Button button2;
    private Button button3;
    //private String url = "https://cinewest.ru/amerikanskie-aktery-top-50-gollivudskih-muzhchin/"; // Адрес сайта
    private String url = "https://ruskino.ru/art/groups/actors"; // Адрес сайта
    private ArrayList<String> urlsImg; // Массив с адресами картинок
    private ArrayList<String> names; // Массив с именами для картинок
    private int numberOfQuestion; // Номер вопроса
    private  int numberOfRightAnswer; // Номер правильного ответа
    private ArrayList<Button> buttons; // Массив всех кнопок

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageViewStar = findViewById(R.id.imageViewStar);
        button0 = findViewById(R.id.button0);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        urlsImg = new ArrayList<>();
        names = new ArrayList<>();
        buttons = new ArrayList<>();
        buttons.add(button0);
        buttons.add(button1);
        buttons.add(button2);
        buttons.add(button3);
        getContent(); // Вызов метода получения контента.
        playGame();
    }

    // Метод, который будет получать контент
    private void getContent (){
        DownloadContentTask task = new DownloadContentTask();
        try {
            String content = task.execute(url).get(); // Получение контента из DownloadContentTask, в качестве параметра url страницы
            String start = "<div class=\"iso_item_list grid\">";
            String finish = "<div class=\"navigation text-center\">";
            Pattern pattern = Pattern.compile(start + "(.*?)" + finish);
            Matcher matcher = pattern.matcher(content);
            String splitContent = ""; // Создание переменной для обрезанного контента
            while (matcher.find()){
                splitContent = matcher.group(1);
            }
            // Log.i("MyResult", content); // Проверка. Вывод контента в консоль
            // Log.i("MyResult", splitContent); // Проверка. Вывод обрезанного контента в консоль
            // Создание паттернов
            Pattern patternImage = Pattern.compile("/media/portret/(.*?)\""); // Паттерн для получения ссылки на картинку
            Pattern patternName = Pattern.compile("<h4>" + "(.*?)" + "</h4>"); // Паттерн для получения текста для подписи картинки
            Matcher matcherImg = patternImage.matcher(splitContent);
            Matcher matcherName = patternName.matcher(splitContent);
            // Заполнение массива urlsImg
            while(matcherImg.find()){
                urlsImg.add("https://ruskino.ru/media/portret/" + matcherImg.group(1));
            }
            // Заполнение массива names
            while (matcherName.find()){
                names.add(matcherName.group(1));
            }
            //-------------------------------------------------
            // Проверка. Цикл для вывода имен
            for(String s : names){
                Log.i("MyResult", s);
            }
            // Проверка. Цикл для вывода ссылок на картинки
            for(String s : urlsImg){
                Log.i("MyResult", s);
            }
            //-------------------------------------------------
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Метод наугад выбирает фото звезды для показа пользователю. Для этого он будет вызывать метод
    private  void playGame (){
        generateQuestion();
        DownloadImageTask task = new DownloadImageTask();
        try {
            Bitmap bitmap = task.execute(urlsImg.get(numberOfQuestion)).get();
            if(bitmap != null){
                imageViewStar.setImageBitmap(bitmap); // Установка картинки в imageViewStar
                // Цикл для установки текста на кнопках
                for(int i = 0; i < buttons.size(); i++){
                    if(i == numberOfRightAnswer){
                        buttons.get(i).setText(names.get(numberOfQuestion));
                    } else {
                        int wrongAnswer = generateWrongAnswer();
                        buttons.get(i).setText(names.get(wrongAnswer));
                    }
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Метод случайным образом генерирует номер вопроса и правильный вариант (1 из 4)
    private void generateQuestion(){
        numberOfQuestion = (int) (Math.random() * names.size());
        // Номер кнопки соответствующей правильному ответу
        numberOfRightAnswer = (int) (Math.random() * buttons.size());  // buttons.size() = 4, В нашем случае
    }
    // Метод, генерирующий номер неправильного ответа
    private int generateWrongAnswer(){
        return (int) (Math.random() * names.size());
    }

    public void onClickAnswer(View view) {

        Button button = (Button) view;
        String tag = button.getTag().toString();
        if(Integer.parseInt(tag) == numberOfRightAnswer){
            Toast.makeText(this, "Верно", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Не верно.\nПравильный ответ: " + names.get(numberOfQuestion), Toast.LENGTH_SHORT).show();
        }
        playGame();
    }

    // Класс для загрузки контента
    private static class DownloadContentTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... strings) {
            URL url = null;
            HttpURLConnection urlConnection = null;
            StringBuilder result = new StringBuilder();
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection(); // Открытие соединения
                InputStream inputStream = urlConnection.getInputStream(); // Получение потока ввода
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream); // Создание ридера
                BufferedReader reader = new BufferedReader(inputStreamReader); // Для чтения данных построчно
                String line = reader.readLine(); // Начало чтения построчно
                while (line != null){ // Чтение до тех пор, пока строка не будет равна null
                    result.append(line); // Добавление строки в result
                    line = reader.readLine(); // Присвоение значения строчке
                }
                return result.toString(); // Когда чтение закончено, возвращается StringBuilder приведенный к строке
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(urlConnection != null){
                    urlConnection.disconnect(); // Закрытие соединения
                }
            }
            return null;
        }
    }

    // Класс для загрузки изображений
    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap>{
        @Override
        protected Bitmap doInBackground(String... strings) {
            URL url = null;
            // HttpURLConnection urlConnection = null;
            HttpURLConnection urlConnection = null;
            StringBuilder result = new StringBuilder();
            try {
                url = new URL(strings[0]);
                // urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream); // Создание Bitmap из inputStream
                return bitmap; // Возврат Bitmap
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(urlConnection != null){
                    urlConnection.disconnect();
                }
            }
            return null;
        }
    }

}