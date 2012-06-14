package org.atmosphere.gwt.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AtmosphereGwtMessageBuffer
{
	HashMap<Integer, AtmosphereGwtMessage> messageBuffer = new HashMap<Integer, AtmosphereGwtMessage>();
	
	Integer readIndex = -1;	// This variable keeps track of the index of the last message that was consumed successfully by the server
	
	Integer incomingMessageSize = null;	// When dealing with partial messages, this variable keeps track of the size of the message we are downloading
	Integer incomingMessageIndex = null;	// This variable keeps track of the index of the message we're downloading (so we can release the messages in order)
	String incomingMessage = "";	// When dealing with partial messages, this variable holds the message data
	String event = null;
	String action = null;
	
	GwtAtmosphereResource resource = null;

	public AtmosphereGwtMessageBuffer(GwtAtmosphereResource resource)
	{
		this.resource = resource;
	}

	/**
	 * This should be called by the AtmosphereGwtHandler.doServerMessage() method. This method will
	 * handle the incoming data sent by the client, buffering and re-ordering messages before allowing
	 * them to be released to the sever for processing.
	 * 
	 * This method processes the messages incoming from the client.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public void process(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		synchronized(resource)
		{
			final char[] buffer = new char[0x10000];
			// First collect the data into a StringBuilder....
			StringBuilder out = new StringBuilder();
			Reader in = null;
			try
			{
				in = new InputStreamReader(request.getInputStream(), "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				// shouldn't ever happen really
			}
			try
			{
				int read;
				do
				{
					read = in.read(buffer, 0, buffer.length);
					if (read > 0)
					{
						out.append(buffer, 0, read);
					}
				}
				while (read >= 0);
			}
			finally
			{
				in.close();
			}
			String rawData = out.toString();
			
			
			
			if (incomingMessageIndex==null)
			{
			
				// Now split out the collected data's header info (delimited) from the
				// actual data...
				event = rawData.charAt(0) + "";
				action = rawData.charAt(2) + "";
				String messageIndexStr = "";
				incomingMessageIndex = -1;
				String messageSizeStr = "";
				incomingMessageSize = -1;
				int field = 3;
				int index = 4;
				while (field < 5)
				{
					if (field == 3)
					{
						if (rawData.charAt(index) == '|')
						{
							field++;
							incomingMessageIndex = new Integer(messageIndexStr);
						}
						else
							messageIndexStr += rawData.charAt(index);
					}
					else if (field == 4)
					{
						if (rawData.charAt(index) == '|')
						{
							field++;
							incomingMessageSize = new Integer(messageSizeStr);
						}
						else
							messageSizeStr += rawData.charAt(index);
					}
		
					index++;
				}
	
				incomingMessage = rawData.substring(index);
			}
			else
			{
				incomingMessage += rawData;
			}
	
			if (incomingMessage.startsWith("null") || incomingMessageSize==null)
				System.out.println("hey!");
			
			// Now determine if we have a complete message...
			if (incomingMessage.length() == incomingMessageSize)
			{
				AtmosphereGwtMessage newMessage = new AtmosphereGwtMessage(event, action, incomingMessageIndex, incomingMessageSize, incomingMessage);
				messageBuffer.put(newMessage.getMessageIndex(), newMessage);
				
				// And reset the state back to the way it was when we started
				event = null;
				action = null;
				incomingMessageIndex = null;
				incomingMessageSize = null;
				incomingMessage = "";
			}
		}
	}
	
	/**
	 * This can be used to keep track of the size of the upload (if its a single large message).
	 * @return
	 */
	public int getBufferSize()
	{
		return incomingMessage.length();
	}
	
	/**
	 * Returns whether or not there are messages available to be consumed.
	 * @return
	 */
	public boolean hasMessages()
	{
		return messageBuffer.get(readIndex+1)!=null;
	}
	
	/**
	 * Returns the next available message in the buffer. This call will ensure the message order is maintained. If a
	 * message is dropped for whatever reason, this call will return null until the the dropped message has
	 * been received. 
	 * 
	 * Note: For dropped messages, the client should know when a mesage has been dropped. Clients sending posts
	 * to the server will receive a status message saying the message was delivered properly or not. The client 
	 * can then use this status to resend messages that didn't make it without the need for the server to
	 * request a new message to be sent.
	 * 
	 * @return
	 */
	public AtmosphereGwtMessage nextMessage()
	{
		AtmosphereGwtMessage msg = messageBuffer.get(readIndex+1);
		if (msg!=null)
		{
			readIndex++;
			messageBuffer.remove(readIndex);
			return msg;
		}

		return null;
	}
}
