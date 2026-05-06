package com.example.pi_dev;

import com.example.pi_dev.Entities.Messaging.Conversation;
import com.example.pi_dev.Entities.Messaging.Message;
import com.example.pi_dev.Repositories.Messaging.ConversationRepository;
import com.example.pi_dev.Repositories.Messaging.MessageRepository;

public class MainTest {
    public static void main(String[] args) throws Exception {

        ConversationRepository cr = new ConversationRepository();
        MessageRepository mr = new MessageRepository();

        Conversation c = new Conversation();
        c.setType("PERSONAL");
        c.setContextType("EVENT");
        c.setContextId(1);

        cr.create(c);

        Message m = new Message();
        m.setConversationId(1);
        //m.setSenderId(1);
        m.setContent("Hello from JavaFX!");

        mr.create(m);

        System.out.println("OK 🚀");
    }
}
