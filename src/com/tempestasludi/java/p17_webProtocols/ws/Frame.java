package com.tempestasludi.java.p17_webProtocols.ws;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Fram represents a data frame in an web socket communication session.
 *
 * @author Tempestas Ludi
 */
public class Frame {

	/**
	 * Whether this frame is the last one or not.
	 */
	private boolean lastFrame;

	/**
	 * The opcode, the type, of the frame.
	 */
	private int opcode;

	/**
	 * Whether this frame is masked (encrypted) or not.
	 */
	private boolean masked;

	/**
	 * If masked, the mask with which it is masked.
	 */
	private byte[] mask;

	/**
	 * The actual data of the frame.
	 */
	private String payload;

	/**
	 * Class constructor.
	 *
	 * @param lastFrame
	 *            whether the frame is the last one or not
	 * @param opcode
	 *            the opcode of the frame
	 * @param masked
	 *            whether the frame is masked or not
	 * @param mask
	 *            if masked, the mask
	 * @param payload
	 *            the content of the frame
	 */
	public Frame(boolean lastFrame, int opcode, boolean masked, byte[] mask, String payload) {
		this.lastFrame = lastFrame;
		this.opcode = opcode;
		this.masked = masked;
		this.mask = mask;
		this.payload = payload;
	}

	/**
	 * Reads a frame from an input stream.
	 *
	 * @param in
	 *            the stream to read from
	 * @return a frame based on the data from the input stream
	 */
	public static Frame read(DataInputStream in) {
		try {
			int buffer = in.read();
			boolean lastFrame = buffer / 128 == 1;
			int opcode = (buffer % 16);
			buffer = in.read();
			boolean masked = buffer / 128 == 1;
			long length = (buffer % 128);
			if (length >= 126) {
				byte[] lengthBuffer;
				if (length == 126) {
					lengthBuffer = new byte[2];
					in.read(lengthBuffer);
					length = ByteBuffer.allocate(2).put(lengthBuffer).getShort();
				} else if (length == 127) {
					lengthBuffer = new byte[8];
					in.read(lengthBuffer);
					length = ByteBuffer.allocate(8).put(lengthBuffer).getLong();
				}
			}
			byte[] mask = new byte[4];
			if (masked) {
				for (int i = 0; i < 4; i++) {
					mask[i] = (byte) in.read();
				}
			}
			byte[] payload = new byte[(int) length];
			for (int i = 0; i < length; i++) {
				if (masked) {
					payload[i] = (byte) (in.read() ^ mask[i % 4]);
				} else {
					payload[i] = (byte) (in.read());
				}
			}
			return new Frame(lastFrame, opcode, masked, mask, new String(payload));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return new Frame(true, 1, false, new byte[0], "");
	}

	/**
	 * Writes the frame to a output stream.
	 *
	 * @param out
	 *            the stream to write to
	 */
	public void write(DataOutputStream out) {
		try {
			if (this.lastFrame) {
				out.write((byte) 128 + opcode);
			} else {
				out.write((byte) opcode);
			}
			byte[] dataBytes = this.payload.getBytes(StandardCharsets.UTF_8);
			long dataLength = dataBytes.length;
			int maskedValue = 0;
			if (this.masked) {
				maskedValue = 128;
			}
			if (dataLength < 126) {
				out.write((byte) dataLength + maskedValue);
			} else if (dataLength < Integer.MAX_VALUE) {
				out.write((byte) 126 + maskedValue);
				out.write(ByteBuffer.allocate(2).putShort((short) dataLength).array());
			} else {
				out.write((byte) 127 + maskedValue);
				out.write(ByteBuffer.allocate(8).putLong(dataLength).array());
			}
			if (this.masked) {
				out.write(this.mask);
			}
			out.write(dataBytes);
			out.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

}
