package de.maboware.clipexchange.model;

import java.io.Serializable;

public class Request implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2912536926175902537L;
	public static final short COPY_CLIPBOARD_TO_SERVER = 1;
	public static final short COPY_CLIPBOARD_FROM_SERVER = 2;
	public static final short COPY_FILE_TO_SERVER = 3;
	public static final short COPY_FILE_FROM_SERVER = 4;
	public static final short REMOVE_FILE_FROM_SERVER = 5;
	public static final short GET_CLIENT_NAME = 6;
	public static final short GET_CLIENT_LIST = 7;
	
	public short request = -1;
	
	public Object payload = null;
	
	public String clientName = null;
	
	public Request(short request) {
		this(request, null);
	}

	public Request(short request, Object payload) {
		//
		this.request = request;
		this.payload = payload;
	}
	
	
}
