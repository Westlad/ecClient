package com.duncanwestland.ec.client.live;

/**
 * @author Duncan Westland
 * @version 1.0.0
 * Class to allow a ValidationException to be created
 */
public class ValidationException extends Exception {
	private static final long serialVersionUID = 1L;
	
	ValidationException(String message){
		super(message);
	}
}
