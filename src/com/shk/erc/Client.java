package com.shk.erc;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.shk.js.data.Convert;
import com.shk.js.io.StreamReader;
import com.shk.js.log.Level;
import com.shk.js.log.Logger;
import com.shk.js.thread.ThreadUtil;

public class Client {
	private static final String HOST_WORD = "Host: ";

	private static class Request {
		public byte[] id;
		public byte[] content;

		public Request(int length) {
			id = new byte[4];
			content = new byte[length];
		}
	}
	
	private Socket mSocket;

	public String serverHost;
	public int serverPort;

	public String clientHost;
	public int clientPort;

	public void start() {
		try {
			mSocket = new Socket();
			mSocket.connect(new InetSocketAddress(serverHost, serverPort));
			
			ThreadUtil.getInstance().run(new Runnable() {
				@Override
				public void run() {
					try {
						OutputStream os = mSocket.getOutputStream();
						
						synchronized (mSocket) {
							os.write(Convert.n2bs(4, 4));
							os.write(Convert.n2bs(0, 4));
							os.flush();
						}
						
						Logger.print(Level.V, "heart beat");
					} catch (Exception e) {
						Logger.print(Level.E, e);
					}
				}
			}, 30000, 30000);

			while (true) {
				StreamReader sr = new StreamReader(mSocket.getInputStream());
				
				byte[] len = new byte[4];
				sr.readFull(len);
				int length = (int) Convert.bs2n(len) - 4;
				Logger.print(Level.V, "read length " + length);

				final Request request = new Request(length);

				sr.readFull(request.id);
				int id = (int) Convert.bs2n(request.id);
				Logger.print(Level.V, "read id " + id);
				
				if (length == 0) {
					if (id != 0) {
						try {
							OutputStream os = mSocket.getOutputStream();
							
							synchronized (mSocket) {
								os.write(Convert.n2bs(4, 4));
								os.write(request.id);
								os.flush();
							}
						} catch (Exception e) {
							Logger.print(Level.E, e);
						}
					}
					
					continue;
				}

				sr.readFull(request.content);
				String content = new String(request.content);
				Logger.print(Level.V, "read content " + content);

				int beginIndex = content.indexOf(HOST_WORD);
				if (beginIndex != -1) {
					int endIndex = content.indexOf("\r\n", beginIndex + HOST_WORD.length());

					StringBuilder sb = new StringBuilder();
					sb.append(content.substring(0, beginIndex + HOST_WORD.length())).append(clientHost).append(':')
							.append(clientPort).append(content.substring(endIndex));

					content = sb.toString();
					Logger.print(Level.V, "new content " + content);

					request.content = content.getBytes();
				}

				ThreadUtil.getInstance().run(new Runnable() {
					@Override
					public void run() {
						byte[] result;

						try {
							Socket socket = new Socket(clientHost, clientPort);
							socket.setSoTimeout(1000);

							OutputStream out = socket.getOutputStream();
							out.write(request.content);
							out.flush();

							InputStream in = socket.getInputStream();
							HttpReader hr = new HttpReader(in);
							result = hr.read();
							
							socket.close();
						} catch (Exception e) {
							Logger.print(Level.E, e);

							String content = "{\"code\": 404}";

							result = ("HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Length: " + content.length()
									+ "\r\nContent-type: application/json\r\nServer: WebSocket++/0.7.0\r\n\r\n"
									+ content).getBytes();
						}

						Logger.printf(Level.V, "write %d %d", id, result.length);

						try {
							String str = new String(result);
							Logger.print(Level.V, "write result", str);
						} catch (Exception e) {
						}

						try {
							OutputStream os = mSocket.getOutputStream();
							
							synchronized (mSocket) {
								os.write(Convert.n2bs(4 + result.length, 4));
								os.write(request.id);
								os.write(result);
								os.flush();
							}
						} catch (Exception e) {
							Logger.print(Level.E, e);
						}
						
						Logger.print(Level.V, "write finish");
					}
				});
			}
		} catch (Exception e) {
			Logger.print(Level.E, e);
		}
	}
}
