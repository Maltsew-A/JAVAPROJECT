package com.example.projectjavaflauwa;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Простая игра-тренажёр устного счёта для учеников начальных классов.
 *
 * Возможности:
 *  - Случайная генерация примеров по уровням сложности.
 *  - Примеры с вводом числового ответа и задания на "да/нет".
 *  - Вопросы на проверку истинности выражений вида "5 + 6 < 10 ?".
 *  - Три уровня сложности: от простых сложения/вычитания до выражений с несколькими действиями.
 *  - Ограничение по времени на каждый пример (чем выше уровень, тем меньше времени).
 *  - При неверном ответе показывается правильный.
 *  - После трёх ошибок игра завершается.
 */
public class MainActivity extends AppCompatActivity {

    // Текущий уровень сложности (1..3)
    private TextView textLevel;
    // Счётчик правильных ответов
    private TextView textScore;
    // Счётчик ошибок
    private TextView textMistakes;
    // Отображение таймера обратного отсчёта
    private TextView textTimer;
    // Сам текст вопроса/примера
    private TextView textQuestion;
    // Дополнительная информация: верно/неверно, подсказки и итоги
    private TextView textInfo;
    // Поле ввода числового ответа
    private EditText editAnswer;
    // Кнопка отправки числового ответа
    private Button buttonSubmit;
    // Кнопка «Да» для булевых вопросов
    private Button buttonYes;
    // Кнопка «Нет» для булевых вопросов
    private Button buttonNo;
    // Кнопка перезапуска игры
    private Button buttonRestart;

    // Один объект Random на всё Activity для генерации чисел и выбора шаблонов
    private final Random random = new Random();
    // Таймер обратного отсчёта для текущего вопроса
    private CountDownTimer countDownTimer;

    // Текущий уровень сложности
    private int level = 1;
    // Общее количество правильных ответов за игру
    private int correctAnswers = 0;
    // Общее количество ошибок (при достижении 3 — игра заканчивается)
    private int mistakes = 0;
    // Сколько вопросов уже задано на текущем уровне (для перехода на следующий)
    private int questionsOnCurrentLevel = 0;
    // Список строк с информацией о неверных ответах (для вывода по завершении игры)
    private List<String> wrongAnswersList = new ArrayList<>();

    // Текст последнего ответа пользователя (для сохранения в подробную статистику ошибок)
    private String lastUserAnswerText = "";

    // Текущий сгенерированный вопрос (числовой или булевый)
    private Question currentQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Обработка системных inset'ов (статус-бар, навигация) для корректного отображения верстки
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Находим все View по id
        initViews();
        // Подписываемся на клики по кнопкам
        initListeners();
        // Запускаем новую игру при старте Activity
        startNewGame();
    }

    /**
     * Связываем поля класса с элементами интерфейса по их id.
     */
    private void initViews() {
        textLevel = findViewById(R.id.textLevel);
        textScore = findViewById(R.id.textScore);
        textMistakes = findViewById(R.id.textMistakes);
        textTimer = findViewById(R.id.textTimer);
        textQuestion = findViewById(R.id.textQuestion);
        textInfo = findViewById(R.id.textInfo);
        editAnswer = findViewById(R.id.editAnswer);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonYes = findViewById(R.id.buttonYes);
        buttonNo = findViewById(R.id.buttonNo);
        buttonRestart = findViewById(R.id.buttonRestart);
    }

    /**
     * Установка обработчиков нажатий на кнопки.
     */
    private void initListeners() {
        // Обработка отправки числового ответа
        buttonSubmit.setOnClickListener(v -> onNumericAnswer());
        // Обработка ответа "Да"
        buttonYes.setOnClickListener(v -> onBooleanAnswer(true));
        // Обработка ответа "Нет"
        buttonNo.setOnClickListener(v -> onBooleanAnswer(false));
        // Перезапуск игры
        buttonRestart.setOnClickListener(v -> startNewGame());
    }

    /**
     * Сброс игры в начальное состояние.
     */
    private void startNewGame() {
        // Начинаем с первого уровня
        level = 1;
        // Обнуляем статистику
        correctAnswers = 0;
        mistakes = 0;
        questionsOnCurrentLevel = 0;
        // Очищаем список неправильных ответов
        wrongAnswersList.clear();
        // Сбрасываем последний ответ пользователя
        lastUserAnswerText = "";

        // Разрешаем взаимодействие с основными элементами управления
        setGameControlsEnabled(true);
        // Очищаем информационный текст
        textInfo.setText("");
        // Обновляем статистику в UI
        updateStatViews();
        // Генерируем первый вопрос
        generateNewQuestion();
    }

    /**
     * Включение/отключение основных контролов игры (при Game Over блокируем их).
     */
    private void setGameControlsEnabled(boolean enabled) {
        buttonSubmit.setEnabled(enabled);
        buttonYes.setEnabled(enabled);
        buttonNo.setEnabled(enabled);
        editAnswer.setEnabled(enabled);
    }

    /**
     * Обновление текстовых полей статистики (уровень, счёт, ошибки).
     */
    private void updateStatViews() {
        textLevel.setText("Уровень: " + level);
        textScore.setText("Правильных: " + correctAnswers);
        textMistakes.setText("Ошибок: " + mistakes + "/3");
    }

    /**
     * Время на пример для каждого уровня (в миллисекундах).
     * Чем выше уровень, тем меньше времени даётся.
     */
    private long getTimeForCurrentLevel() {
        switch (level) {
            case 1:
            default:
                return 20_000; // 20 секунд
            case 2:
                return 15_000; // 15 секунд
            case 3:
                return 10_000; // 10 секунд
        }
    }

    /**
     * Генерация нового вопроса и запуск таймера.
     */
    private void generateNewQuestion() {
        // Останавливаем таймер для предыдущего вопроса, если он ещё тикает
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Если уже набрано 3 ошибки — не генерируем новые вопросы
        if (mistakes >= 3) {
            // Игра уже завершена.
            return;
        }

        // Очищаем подсказки и поле ввода
        textInfo.setText("");
        editAnswer.setText("");

        // Выбираем тип вопроса: с числовым ответом или "да/нет"
        boolean numericQuestion;
        if (level == 1) {
            // На первом уровне чаще обычные числовые задания.
            numericQuestion = random.nextInt(4) != 0; // 3 из 4 будут с числовым ответом.
        } else {
            // На других уровнях примерное равенство по типам
            numericQuestion = random.nextBoolean();
        }

        // Генерируем конкретный вопрос в соответствии с уровнем и типом
        if (numericQuestion) {
            currentQuestion = generateNumericQuestion(level);
            showNumericInput(); // Показываем поле ввода и кнопку "Ответить"
        } else {
            currentQuestion = generateBooleanQuestion(level);
            showBooleanInput(); // Показываем кнопки "Да/Нет"
        }

        // Отображаем текст вопроса
        textQuestion.setText(currentQuestion.text);
        // Запускаем таймер для этого вопроса
        startTimerForQuestion();
    }

    /**
     * Переключает интерфейс в режим числового ответа.
     */
    private void showNumericInput() {
        editAnswer.setVisibility(View.VISIBLE);
        buttonSubmit.setVisibility(View.VISIBLE);

        buttonYes.setVisibility(View.GONE);
        buttonNo.setVisibility(View.GONE);
    }

    /**
     * Переключает интерфейс в режим ответа "да/нет".
     */
    private void showBooleanInput() {
        editAnswer.setVisibility(View.GONE);
        buttonSubmit.setVisibility(View.GONE);

        buttonYes.setVisibility(View.VISIBLE);
        buttonNo.setVisibility(View.VISIBLE);
    }

    /**
     * Настройка и запуск CountDownTimer для текущего вопроса.
     */
    private void startTimerForQuestion() {
        long time = getTimeForCurrentLevel();
        // Инициализируем отображение таймера в секундах
        textTimer.setText("Время: " + (time / 1000));

        countDownTimer = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Обновляем оставшееся время каждую секунду
                textTimer.setText("Время: " + (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                // Таймер закончился — считаем это ошибкой
                textTimer.setText("Время: 0");

                // Пользователь не успел ответить
                lastUserAnswerText = "нет ответа";

                // Показываем, что время вышло, и обрабатываем как неправильный ответ
                handleWrongAnswer("Время вышло!");
            }
        }.start();
    }

    /**
     * Обработка ответа с числом.
     */
    private void onNumericAnswer() {
        // Проверяем, что текущий вопрос числовой
        if (currentQuestion == null || !currentQuestion.numeric) {
            return;
        }

        String text = editAnswer.getText().toString().trim();
        // Не даём отправить пустой ответ
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Введите ответ", Toast.LENGTH_SHORT).show();
            return;
        }

        int value;
        try {
            // Пытаемся преобразовать введённый текст к числу
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            // Если формат неверный — информируем пользователя
            Toast.makeText(this, "Нужно ввести целое число", Toast.LENGTH_SHORT).show();
            return;
        }

        // Сохраняем ответ пользователя для последующей статистики
        lastUserAnswerText = String.valueOf(value);

        // Останавливаем таймер, так как ответ уже дан
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Сравниваем ответ с правильным
        if (value == currentQuestion.numericAnswer) {
            handleCorrectAnswer();
        } else {
            // Сообщение без "Правильный ответ", чтобы не дублировать текст
            handleWrongAnswer("Неверно.");
        }
    }

    /**
     * Обработка ответа "да/нет".
     *
     * @param userAnswer true — «да», false — «нет».
     */
    private void onBooleanAnswer(boolean userAnswer) {
        // Проверяем, что текущий вопрос именно булевый
        if (currentQuestion == null || currentQuestion.numeric) {
            return;
        }

        // Сохраняем ответ пользователя как текст
        lastUserAnswerText = userAnswer ? "да" : "нет";

        // Останавливаем таймер перед проверкой
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Если ответ совпадает с правильным булевым значением
        if (userAnswer == currentQuestion.booleanAnswer) {
            handleCorrectAnswer();
        } else {
            // Сообщение без "Правильный ответ", чтобы не дублировать текст
            handleWrongAnswer("Неверно.");
        }
    }

    /**
     * Унифицированная обработка правильного ответа.
     */
    private void handleCorrectAnswer() {
        // Увеличиваем счётчик правильных ответов
        correctAnswers++;
        // Увеличиваем количество вопросов на текущем уровне
        questionsOnCurrentLevel++;
        // Показываем сообщение пользователю
        textInfo.setText("Верно!");
        // Обновляем статистику в UI
        updateStatViews();
        // Проверяем, можно ли перейти на следующий уровень
        checkLevelUp();
        // Генерируем следующий вопрос
        generateNewQuestion();
    }

    /**
     * Унифицированная обработка неправильного ответа.
     *
     * @param info текстовое сообщение, которое будет показано пользователю
     *             (например: "Неверно." или "Время вышло!").
     */
    private void handleWrongAnswer(String info) {
        // Увеличиваем количество ошибок
        mistakes++;

        // Подставляем сохранённый ответ, если он пустой — показываем прочерк
        String userAnswerText = (lastUserAnswerText == null || lastUserAnswerText.isEmpty())
                ? "—"
                : lastUserAnswerText;

        // Формируем строку с подробной информацией о вопросе и ответе пользователя
        String wrongAnswer = "Вопрос: " + currentQuestion.text + "\n" +
                "Ваш ответ: " + userAnswerText + "\n" +
                currentQuestion.getCorrectAnswerText();
        // Добавляем этот строковый отчёт в общий список неверных ответов
        wrongAnswersList.add(wrongAnswer);

        // Выводим информационное сообщение + правильный ответ
        textInfo.setText(info + " " + currentQuestion.getCorrectAnswerText());
        // Обновляем статистику
        updateStatViews();

        // При достижении трёх ошибок — завершаем игру
        if (mistakes >= 3) {
            gameOver();
        } else {
            // Иначе — продолжаем и генерируем следующий вопрос
            generateNewQuestion();
        }
    }

    /**
     * Переход на новый уровень после нескольких правильных ответов.
     * Здесь условие: 5 вопросов на уровне => переход на следующий.
     */
    private void checkLevelUp() {
        if (level < 3 && questionsOnCurrentLevel >= 5) {
            level++;
            // Сбрасываем счётчик вопросов для нового уровня
            questionsOnCurrentLevel = 0;
            // Краткое уведомление о повышении уровня
            Toast.makeText(this, "Новый уровень: " + level, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Завершение игры после трёх ошибок.
     */
    private void gameOver() {
        // На всякий случай останавливаем таймер
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        // Блокируем основные элементы управления, чтобы нельзя было продолжать
        setGameControlsEnabled(false);

        // Показываем финальное сообщение
        textQuestion.setText("Игра окончена");
        textInfo.setText("Вы сделали 3 ошибки. Правильных ответов: " + correctAnswers);

        // Формируем текст со всеми неправильными ответами
        StringBuilder wrongAnswersText = new StringBuilder("Неправильные ответы:\n");
        for (String wrongAnswer : wrongAnswersList) {
            wrongAnswersText.append(wrongAnswer).append("\n\n");
        }

        // Обнуляем таймер в UI
        textTimer.setText("Время: -");
        // Добавляем список неверных ответов к итоговому сообщению
        textInfo.setText(textInfo.getText().toString() + "\n\n" + wrongAnswersText.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // При уничтожении Activity обязательно останавливаем таймер,
        // чтобы избежать утечек и продолжения работы в фоне
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    /**
     * Генерация примера с числовым ответом.
     * В зависимости от уровня создаются разные шаблоны задач.
     */
    private Question generateNumericQuestion(int level) {
        Question q = new Question();
        q.numeric = true;

        int a;
        int b;
        int c;

        switch (level) {
            case 1:
                // Первый уровень: простые выражения на + и -, иногда в виде задачки про фрукты.
                if (random.nextBoolean()) {
                    // Задачка с фруктами.
                    a = random.nextInt(5) + 1; // 1..5
                    b = random.nextInt(5) + 1; // 1..5
                    q.numericAnswer = a + b;
                    q.text = a + " груши + " + b + " яблока. Сколько всего фруктов?";
                } else {
                    // Обычные примеры на сложение/вычитание
                    a = random.nextInt(20) + 1; // 1..20
                    b = random.nextInt(20) + 1; // 1..20

                    if (random.nextBoolean()) {
                        // Сложение.
                        q.numericAnswer = a + b;
                        q.text = a + " + " + b + " = ?";
                    } else {
                        // Вычитание (делаем так, чтобы результат был неотрицательным).
                        if (b > a) {
                            int tmp = a;
                            a = b;
                            b = tmp;
                        }
                        q.numericAnswer = a - b;
                        q.text = a + " - " + b + " = ?";
                    }
                }
                break;

            case 2:
                // Второй уровень: одно действие, но уже +, -, * или /.
                int opIndex = random.nextInt(4); // 0:+ 1:- 2:* 3:/

                if (opIndex == 0) {
                    // Сложение.
                    a = random.nextInt(50) + 1;
                    b = random.nextInt(50) + 1;
                    q.numericAnswer = a + b;
                    q.text = a + " + " + b + " = ?";
                } else if (opIndex == 1) {
                    // Вычитание.
                    a = random.nextInt(50) + 1;
                    b = random.nextInt(50) + 1;
                    if (b > a) {
                        int tmp = a;
                        a = b;
                        b = tmp;
                    }
                    q.numericAnswer = a - b;
                    q.text = a + " - " + b + " = ?";
                } else if (opIndex == 2) {
                    // Умножение.
                    a = random.nextInt(10) + 1; // 1..10
                    b = random.nextInt(10) + 1;
                    q.numericAnswer = a * b;
                    q.text = a + " × " + b + " = ?";
                } else {
                    // Деление нацело.
                    b = random.nextInt(9) + 2; // 2..10
                    int result = random.nextInt(10) + 1; // 1..10
                    a = b * result;
                    q.numericAnswer = result;
                    q.text = a + " ÷ " + b + " = ?";
                }
                break;

            case 3:
            default:
                // Третий уровень: выражения с несколькими действиями.
                int template = random.nextInt(3);
                if (template == 0) {
                    // a + b + c
                    a = random.nextInt(50) + 1;
                    b = random.nextInt(50) + 1;
                    c = random.nextInt(50) + 1;
                    q.numericAnswer = a + b + c;
                    q.text = a + " + " + b + " + " + c + " = ?";
                } else if (template == 1) {
                    // a + b - c (стараемся избежать отрицательных результатов).
                    a = random.nextInt(60) + 20; // 20..79
                    b = random.nextInt(40) + 1;  // 1..40
                    c = random.nextInt(40) + 1;  // 1..40
                    q.numericAnswer = a + b - c;
                    q.text = a + " + " + b + " - " + c + " = ?";
                } else {
                    // (a + b) × c
                    a = random.nextInt(10) + 1;
                    b = random.nextInt(10) + 1;
                    c = random.nextInt(5) + 2; // 2..6
                    q.numericAnswer = (a + b) * c;
                    q.text = "(" + a + " + " + b + ") × " + c + " = ?";
                }
                break;
        }

        return q;
    }

    /**
     * Генерация задания на проверку истинности выражения (вопросы "да/нет").
     */
    private Question generateBooleanQuestion(int level) {
        Question q = new Question();
        q.numeric = false;

        int a;
        int b;
        int c;
        int leftValue;
        String leftText;

        // Генерируем левую часть выражения (арифметическую).
        if (level == 1) {
            // Уровень 1: только + и - с небольшими числами
            a = random.nextInt(20) + 1;
            b = random.nextInt(20) + 1;

            if (random.nextBoolean()) {
                leftValue = a + b;
                leftText = a + " + " + b;
            } else {
                if (b > a) {
                    int tmp = a;
                    a = b;
                    b = tmp;
                }
                leftValue = a - b;
                leftText = a + " - " + b;
            }
        } else if (level == 2) {
            // Уровень 2: +, -, *, / (деление нацело)
            int opIndex = random.nextInt(4);
            if (opIndex == 0) {
                // +
                a = random.nextInt(50) + 1;
                b = random.nextInt(50) + 1;
                leftValue = a + b;
                leftText = a + " + " + b;
            } else if (opIndex == 1) {
                // -
                a = random.nextInt(50) + 1;
                b = random.nextInt(50) + 1;
                if (b > a) {
                    int tmp = a;
                    a = b;
                    b = tmp;
                }
                leftValue = a - b;
                leftText = a + " - " + b;
            } else if (opIndex == 2) {
                // *
                a = random.nextInt(10) + 1;
                b = random.nextInt(10) + 1;
                leftValue = a * b;
                leftText = a + " × " + b;
            } else {
                // деление нацело
                b = random.nextInt(9) + 2;
                int result = random.nextInt(10) + 1;
                a = b * result;
                leftValue = result;
                leftText = a + " ÷ " + b;
            }
        } else {
            // Уровень 3: простые выражения с двумя действиями (+ и -),
            // чтобы запись оставалась понятной для ребёнка
            a = random.nextInt(50) + 1;
            b = random.nextInt(50) + 1;
            c = random.nextInt(30) + 1;

            if (random.nextBoolean()) {
                leftValue = a + b + c;
                leftText = a + " + " + b + " + " + c;
            } else {
                leftValue = a + b - c;
                leftText = a + " + " + b + " - " + c;
            }
        }

        // Выбираем знак сравнения и решаем, будет ли выражение истинным или ложным.
        int compIndex = random.nextInt(3); // 0:"<" 1:">" 2:"="
        boolean shouldBeTrue = random.nextBoolean();

        String comparator;
        int rightValue;
        boolean expressionIsTrue;

        if (compIndex == 0) {
            comparator = "<";
            if (shouldBeTrue) {
                // Делаем выражение однозначно истинным: rightValue > leftValue
                rightValue = leftValue + (random.nextInt(10) + 1);
                expressionIsTrue = true; // left < right
            } else {
                // делаем выражение ложным: right <= left
                if (random.nextBoolean()) {
                    // Случай right == left
                    rightValue = leftValue;
                } else {
                    // Случай right < left (но не уходим сильно в минус)
                    rightValue = leftValue - (random.nextInt(10) + 1);
                    if (rightValue < 0) {
                        rightValue = leftValue;
                    }
                }
                expressionIsTrue = false;
            }
        } else if (compIndex == 1) {
            comparator = ">";
            if (shouldBeTrue) {
                // Делаем выражение истинным: rightValue < leftValue
                rightValue = leftValue - (random.nextInt(10) + 1);
                if (rightValue < 0) {
                    rightValue = 0;
                }
                expressionIsTrue = true; // left > right
            } else {
                // делаем выражение ложным: right >= left
                if (random.nextBoolean()) {
                    rightValue = leftValue;
                } else {
                    rightValue = leftValue + (random.nextInt(10) + 1);
                }
                expressionIsTrue = false;
            }
        } else {
            comparator = "=";
            if (shouldBeTrue) {
                // Истинное равенство: rightValue == leftValue
                rightValue = leftValue;
                expressionIsTrue = true;
            } else {
                // Ложное "равенство": специально смещаем значение
                int delta = random.nextInt(5) + 1;
                if (random.nextBoolean()) {
                    rightValue = leftValue + delta;
                } else {
                    rightValue = leftValue - delta;
                    if (rightValue < 0) {
                        rightValue = leftValue + delta;
                    }
                }
                expressionIsTrue = false;
            }
        }

        // Сохраняем правильный булевый ответ (истинно или ложно выражение)
        q.booleanAnswer = expressionIsTrue;
        // Формируем текст вопроса, который увидит пользователь
        q.text = leftText + " " + comparator + " " + rightValue + " ?";
        return q;
    }

    /**
     * Внутренний класс, описывающий задание (вопрос).
     * Может быть двух типов: числовой (numeric == true) или булевый (numeric == false).
     */
    private static class Question {
        // Текст вопроса, который показывается пользователю
        String text;
        // Тип вопроса: true — числовой ответ, false — «да/нет»
        boolean numeric;
        // Числовой правильный ответ (используется, если numeric == true)
        int numericAnswer;
        // Правильный булевый ответ (используется, если numeric == false)
        boolean booleanAnswer;

        /**
         * Возвращает готовую строку с правильным ответом
         * (используется для подсказки/обратной связи пользователю).
         */
        String getCorrectAnswerText() {
            if (numeric) {
                return "Правильный ответ: " + numericAnswer;
            } else {
                return "Правильный ответ: " + (booleanAnswer ? "да" : "нет");
            }
        }
    }
}
