/*
* Copyright 2012 Jeanfrancois Arcand
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/

package org.atmosphere.gwt.client.impl;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.SerializationException;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author p.havelaar
 */
abstract public class ServerTransportProtocol implements ServerTransport {

	int sentMessageCounter = 0;
	
	
    abstract void send(String message, AsyncCallback<Void> callback);

    abstract String serialize(Object message) throws SerializationException;

    private Logger logger = Logger.getLogger(getClass().getName());

    private AsyncCallback<Void> defaultCallback = new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
            logger.log(Level.SEVERE, "Failed send", caught);
        }

        @Override
        public void onSuccess(Void result) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Send succesfull");
            }
        }
    };

    @Override
    public void disconnect() {
        send(pack(MessageType.CONNECTION, ActionType.DISCONNECT), defaultCallback);
    }

    @Override
    public void broadcast(Object message) {
        if (message instanceof String) {
            send(pack(MessageType.STRING, ActionType.BROADCAST, message.toString()), defaultCallback);
            
        } else {
            try {
                send(pack(MessageType.OBJECT, ActionType.BROADCAST, serialize(message)), defaultCallback);
                
            } catch (SerializationException ex) {
                logger.log(Level.SEVERE, "Failed to serialize message", ex);
            }
        }
    }

    @Override
    public void broadcast(List messages) {
        StringBuilder packet = new StringBuilder();
        for (Object message : messages) {
            if (message instanceof String) {
                packet.append(pack(MessageType.STRING, ActionType.BROADCAST, message.toString()));
            } else {
                try {
                    packet.append(pack(MessageType.OBJECT, ActionType.BROADCAST, serialize(message)));
                } catch (SerializationException ex) {
                    logger.log(Level.SEVERE, "Failed to serialize message", ex);
                }
            }
        }
        if (packet.length() > 0) {
            send(packet.toString(), defaultCallback);
            
        }
    }

    @Override
    public void post(Object message, AsyncCallback<Void> callback) {
        if (message instanceof String) {
            send(pack(MessageType.STRING, ActionType.POST, message.toString()), callback);
            
        } else {
            try {
                send(pack(MessageType.OBJECT, ActionType.POST, serialize(message)), callback);
                
            } catch (SerializationException ex) {
                logger.log(Level.SEVERE, "Failed to serialize message", ex);
            }
        }
    }

    @Override
    public void post(List messages, AsyncCallback<Void> callback) {
        StringBuilder packet = new StringBuilder();
        for (Object message : messages) {
            if (message instanceof String) {
                packet.append(pack(MessageType.STRING, ActionType.POST, message.toString()));
            } else {
                try {
                    packet.append(pack(MessageType.OBJECT, ActionType.POST, serialize(message)));
                } catch (SerializationException ex) {
                    logger.log(Level.SEVERE, "Failed to serialize message", ex);
                }
            }
        }
        if (packet.length() > 0) {
            send(packet.toString(), callback);
            
        }
    }
    
    private enum MessageType {
        STRING("s"),
        OBJECT("o"),
        CONNECTION("c");
        
        String type;
        MessageType(String str) {
            type = str;
        }
    }
    private enum ActionType {
        BROADCAST("b"),
        POST("p"),
        DISCONNECT("d");
        
        String type;
        ActionType(String act) {
            type = act;
        }
    }
    protected String pack(MessageType msgType, ActionType actType) {
        return pack(msgType, actType, "");
    }
    
    
    /**
     * This method adds a header to the message the end-user wants to send to the server. 
     * 
     * WARNING: Only call this method once per message, this method includes a counter
     * that keeps track of the number of messages sent to the server and this counter increases
     * by 1 every time this method is called. 
     * (Its not the best place for the counter but it ensures the counter is never missed, which
     * would cause the server to silently stop delivering messages)
     * 
     * @param msgType
     * @param actType
     * @param message
     * @return
     */
    protected String pack(MessageType msgType, ActionType actType, String message) {
    	String result = msgType.type + "|" + actType.type + "|" + sentMessageCounter + "|" + message.length() + "|" + message;
        sentMessageCounter++;
        return result;
    }
}
