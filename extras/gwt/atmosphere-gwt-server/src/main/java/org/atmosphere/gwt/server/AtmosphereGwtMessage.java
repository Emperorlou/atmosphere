package org.atmosphere.gwt.server;


public class AtmosphereGwtMessage
{
	private String event = null;
	private String action = null;
	private Integer messageIndex = null;
	private Integer messageSize = null;
	private String message = null;
	
	public AtmosphereGwtMessage(String event, String action, Integer messageIndex, Integer messageSize, String message)
	{
		this.event = event;
		this.action = action;
		this.messageIndex = messageIndex;
		this.messageSize = messageSize;
		this.message = message;
	}

	public String getEvent()
	{
		return event;
	}

	public String getAction()
	{
		return action;
	}

	public Integer getMessageIndex()
	{
		return messageIndex;
	}

	public Integer getMessageSize()
	{
		return messageSize;
	}

	public String getMessage()
	{
		return message;
	}
	
	
}
