package com.klein.btc;

import com.klein.btc.bitfinex.BitfinexBot;
import com.klein.btc.gdax.GdaxBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by mresc on 04.11.17.
 */
public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger LOG = LoggerFactory.getLogger(TelegramBot.class);
    private final Properties props;

    public TelegramBot() {
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
        SendMessage apiMsg=new SendMessage("@mrSmalll", "Test");
    }

    @Override
    public void onUpdateReceived(Update update) {
        LOG.debug("Telegram update from user: {}:{}", update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        LOG.debug("Telegram updates: {}", updates.size());
        for (Update update : updates) {
            LOG.debug("Telegram update from user: {}:{}", update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());
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
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();

        TelegramBot bot = new TelegramBot();

        try {
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
