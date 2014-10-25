/**
 * 
 */
package com.duncanwestland.ec.client.live;

import java.nio.ByteBuffer;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * @author duncan
 * A wrapper for a CardChannel object that adds logging
 *
 */
public class WrappedCardChannel extends CardChannel {
	CardChannel tcvr;
	
	public WrappedCardChannel(CardChannel transceiver){
		this.tcvr = transceiver;
	}

	/* (non-Javadoc)
	 * @see javax.smartcardio.CardChannel#close()
	 */
	@Override
	public void close() throws CardException {
		tcvr.close();
	}

	/* (non-Javadoc)
	 * @see javax.smartcardio.CardChannel#getCard()
	 */
	@Override
	public Card getCard() {
		return tcvr.getCard();
	}

	/* (non-Javadoc)
	 * @see javax.smartcardio.CardChannel#getChannelNumber()
	 */
	@Override
	public int getChannelNumber() {
		return tcvr.getChannelNumber();
	}

	/* (non-Javadoc)
	 * @see javax.smartcardio.CardChannel#transmit(javax.smartcardio.CommandAPDU)
	 */
	@Override
	public ResponseAPDU transmit(CommandAPDU arg0) throws CardException {
		Crypto.logger("CommandAPDU data ", arg0.getData());
		ResponseAPDU response = tcvr.transmit(arg0);
		Crypto.logger("ResponseAPDU data ", response.getData());
		byte[] status = {(byte)response.getSW1(),(byte)response.getSW2()};
		Crypto.logger("Status bytes ", status);
		return response;
	}

	/* (non-Javadoc)
	 * @see javax.smartcardio.CardChannel#transmit(java.nio.ByteBuffer, java.nio.ByteBuffer)
	 */
	@Override
	public int transmit(ByteBuffer arg0, ByteBuffer arg1) throws CardException {
		// TODO Auto-generated method stub
		return tcvr.transmit(arg0, arg1);
	}

}
