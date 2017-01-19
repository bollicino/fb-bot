package com.mondora.facebook.sending;

import com.fasterxml.jackson.databind.JsonNode;
import com.mondora.facebook.postback.PBElement;
import com.mondora.facebook.postback.PBPayload;
import com.mondora.facebook.postback.TwoChoicePostback;

/**
 * Created by mmondora on 12/01/2017.
 */
public class Help extends Sender implements Strategy {
    @Override
    public void run(JsonNode node) {
        TwoChoicePostback o = createPostBack(node);
        PBPayload payload = o.message.attachment.payload;
        PBElement element = payload.addElement("Agyo, help", "I comandi disponibili sono", "https://app.agyo.io/");
        element.addPostbackButton("Simula notifica", "fattura");
        element.addPostbackButton("Lista di oggi", "listToday");
        element.addPostbackButton("Statistiche", "stats");
        sendPostback(o);
    }
}
