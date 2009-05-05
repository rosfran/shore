package com.codahale.shore;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Prints usage information for an application to an output stream.
 * 
 * @author coda
 * 
 */
public class HelpCommand implements Runnable {
	private final String text;
	private final OutputStream outputStream;

	/**
	 * Creates a new {@link HelpCommand}.
	 * 
	 * @param text the help text to display
	 * @param outputStream
	 *            the stream to print the text to
	 */
	public HelpCommand(String text, OutputStream outputStream) {
		this.text = text;
		this.outputStream = outputStream;
	}
	
	public String getText() {
		return text;
	}

	/**
	 * Returns the output stream.
	 * 
	 * @return the output stream.
	 */
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public void run() {
		final PrintWriter out = new PrintWriter(outputStream);
		out.println(text);
		out.flush();
	}
}
