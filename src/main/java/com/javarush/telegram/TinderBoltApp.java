package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.Optional;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "hahacom-bot"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "6319229397:AAFgdw9G8ZMWkwHLe73Yx9rxkzSHzVs1_rc"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "gpt:6MZuruLWYMt7BFAYy33hJFkblB3TrOQSkF7WUgsEFs26dToB"; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private UserInfo me;
    private int questionCount;
    private UserInfo she;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();
        if(message.equals("/start")){
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("avatar_main");
            String main = loadMessage("main");
            sendTextMessage(main);

            showMainMenu("главное меню бота","/start",
                    "генерация Tinder-профля \uD83D\uDE0E","/profile",
                    "сообщение для знакомства \uD83E\uDD70","/opener",
                    "переписка от вашего имени \uD83D\uDE08","/message",
                    "переписка со звездами \uD83D\uDD25","/date",
                    "задать вопрос чату GPT \uD83E\uDDE0","/gpt");

            return;
        }

        if(message.equals("/gpt")){
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if(currentMode == DialogMode.GPT && !isMessageCommand()){
            String prompt = loadPrompt("gpt");
            Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
            String answer = chatGPT.sendMessage(prompt, message);
            updateTextMessage(msg,answer);
            return;
        }

        if(message.equals("/date")){
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Ариана Гранде","date_grande",
                    "Марго Робби","date_robbie",
                    "Зендея","date_zendaya",
                    "Райан Гослинг","date_gosling",
                    "Том Харди","date_hardy");
            return;
        }

        if(currentMode == DialogMode.DATE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if(query.startsWith("date_")){
                sendPhotoMessage(query);

                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
            }
            Message msg = sendTextMessage("Подождите собеседник(ца) печатает...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg,answer);
            return;
        }

        if(message.equals("/message")){
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            String text = loadMessage("message");
            sendTextButtonsMessage(text,
                    "Следующее сообщение","message_next",
                    "Пригласить на свидание","message_date");
            return;
        }

        if(currentMode == DialogMode.MESSAGE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();

            if(query.startsWith("message_")){
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n",list);

                Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                String answer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg,answer);
            }
            list.add(message);


            return;
        }

        if(message.equals("/profile")){
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");
            String text = loadMessage("profile");
            sendTextMessage(text);

            me = new UserInfo();
            questionCount = 1;

            sendTextMessage("СКолько вам лет?");
            return;
        }

        if(currentMode == DialogMode.PROFILE && !isMessageCommand()){
            switch (questionCount){
                case 1:
                    me.age = message;
                    questionCount = 2;

                    sendTextMessage("Кем вы работаете?");
                    return;
                case 2:
                    me.occupation = message;
                    questionCount++;
                    sendTextMessage("Какое у вас хобби?");
                    return;
                case 3:
                    me.hobby = message;
                    questionCount++;
                    sendTextMessage("Что вам не нравится в людях");
                    return;
                case 4:
                    me.annoys = message;
                    questionCount++;
                    sendTextMessage("Цель знакомств?");
                    return;
                case 5:
                    me.goals = message;
                    String aboutMyself = me.toString();
                    String prompt = loadPrompt("profile");

                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutMyself);
                    updateTextMessage(msg,answer);

                    return;
            }
        }

        if(message.equals("/opener")){
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            she = new UserInfo();
            questionCount = 1;
            String text = loadMessage("opener");
            sendTextMessage(text);
            sendTextMessage("Имя девушки?");
            return;
        }

        if(currentMode == DialogMode.OPENER && !isMessageCommand()){
            switch (questionCount){
                case 1:
                    she.name = message;
                    questionCount++;
                    sendTextMessage("Сколько ей лет?");
                    return;
                case 2:
                    she.age = message;
                    questionCount++;
                    sendTextMessage("Есть ли у нее хобби и какие?");
                    return;
                case 3:
                    she.hobby = message;
                    questionCount++;
                    sendTextMessage("Кем она работает?");
                    return;
                case 4:
                    she.occupation = message;
                    questionCount++;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    she.goals = message;
                    questionCount++;
                    String aboutFriend = me.toString();
                    String prompt = loadPrompt("opener");

                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg,answer);
                    return;
            }
            return;
        }

        sendTextMessage("*Hello*");

        sendTextMessage("Вы написали " + message);

        sendTextButtonsMessage("Выберите режим работы:", "Старт","start","Стоп","stop");
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
