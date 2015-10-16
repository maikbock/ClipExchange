package de.maboware.clipexchange.model;

import java.io.Serializable;

public class Response implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1557024434519678084L;
	public Response(String string) {
		this((Object)string);
	}
	
	public Response(Object payload) {
		this.payload = payload;
	}
	
	public Response() {
		//
	}

	public Object payload;
	public String response;

	@Override
	public String toString() {
	
		return response;
	}
}
