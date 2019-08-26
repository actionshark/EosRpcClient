package com.shk.erc;

import com.shk.js.log.FileLogger;
import com.shk.js.log.Level;
import com.shk.js.log.Logger;

public class Main {
	public static void main(String[] args) {
		FileLogger fl = new FileLogger();
		fl.setFiles("log1.txt", "log2.txt");

		Logger.setInstance(fl);
		Logger.setLevel(Level.D);

		String serverHost = "";
		int serverPort = 10002;

		String clientHost = "127.0.0.1";
		int clientPort = 10000;

		for (int i = 0; i < args.length; i++) {
			String str = args[i];

			if ("-server-host".equals(str)) {
				serverHost = args[i++];
			} else if ("-server-port".equals(str)) {
				serverPort = Integer.parseInt(args[i++]);
			} else if ("-client-host".equals(str)) {
				clientHost = args[i++];
			} else if ("-client-port".equals(str)) {
				clientPort = Integer.parseInt(args[i++]);
			}
		}

		Logger.printf(Level.I, "server is %s:%d, client is %s:%d", serverHost, serverPort, clientHost, clientPort);

		Client client = new Client();
		client.serverHost = serverHost;
		client.serverPort = serverPort;
		client.clientHost = clientHost;
		client.clientPort = clientPort;

		for (int i = 0; ; i++) {
			Logger.print(Level.I, "start " + i);
			
			client.start();
			
			Logger.print(Level.I, "error cased, restart after 3s");
			
			try {
				Thread.sleep(3000);
			} catch (Exception e) {
			}
		}
	}
}
