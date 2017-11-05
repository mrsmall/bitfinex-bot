package com.klein.btc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by mresc on 04.11.17.
 */
public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramBot.class);
    private final Properties props;
    private Set<Long> subsciptions=new HashSet<>();

    public TelegramBot() {
        super();
        props=new Properties();
        FileInputStream fis= null;
        try {
            fis = new FileInputStream("api_key.properties");
            props.load(fis);
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(){
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Integer senderId = update.getMessage().getFrom().getId();
        String senderUsername = update.getMessage().getFrom().getUserName();
        String text=update.getMessage().getText();
        LOG.debug("Update from user: {}:{} - {}", senderId, senderUsername, text);
        if (update.getMessage().hasText()){
            if (update.getMessage().isCommand()){
                if (text.startsWith("/subscribe_opportunities")){
                    LOG.debug("Received subscription commnad from: {}:{}", senderId, senderUsername);
                    processSubscriptionCommand(update.getMessage().getChatId());
                } else if (text.startsWith("/unsubscribe_opportunities")){
                    LOG.debug("Received unsubscribe commnad from: {}:{}", senderId, senderUsername);
                    processUnsubscriptionCommand(update.getMessage().getChatId());
                } else if (text.startsWith("/help")){
                    LOG.debug("Received help commnad from: {}:{}", senderId, senderUsername);
                    sendHelp(update.getMessage().getChatId());
                }
            }
        }
    }

    private void sendHelp(Long chatId) {
        sendResponse(chatId, "" +
                "/help - show me this help =)\n" +
                "/subscribe_opportunities - send me trade opportunities\n" +
                "/unsubscribe_opportunities - stop sending me trade opportunities\n" +
                "");
    }

    private void processSubscriptionCommand(Long chatId) {
        if (!subsciptions.contains(chatId)){
            subsciptions.add(chatId);
            sendResponse(chatId, "You are subscribed now!");
        } else {
            sendResponse(chatId, "You are already subscribed");
        }
    }

    private void processUnsubscriptionCommand(Long chatId) {
        if (subsciptions.contains(chatId)){
            subsciptions.remove(chatId);
            sendResponse(chatId, "You are unsubscribed!");
        } else {
            sendResponse(chatId, "You are not subscribed");
        }
    }

    private void sendResponse(Long chatId, String s) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(chatId)
                .setText(s);
        try {
            execute(message); // Sending our message object to user
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        LOG.debug("Got updates: {}", updates.size());
        for (Update update : updates) {
            onUpdateReceived(update);
        }
    }


    @Override
    public String getBotUsername() {
        return "SafeHarborBot";
    }

    @Override
    public String getBotToken() {
        return props.getProperty("TELEGRAM_API")    ;
    }

    @Override
    public void onClosing() {
        LOG.info("Closing telegram bot");
    }


    public static void main(String[] args){
        TelegramBot bot = new TelegramBot();
    }

    public void notifyOpportunity(String product, String exchange1, String exchange2, float diff) {
        String message="Opportunity to trade "+product+" between "+exchange1+" and "+exchange2+", diffrence "+diff+"%";
        for (Long chatId : subsciptions) {
            sendResponse(chatId, message);
        }
    }
}
