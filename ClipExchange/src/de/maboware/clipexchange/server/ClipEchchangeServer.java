package de.maboware.clipexchange.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

import de.maboware.clipexchange.model.Request;
import de.maboware.clipexchange.model.Response;

public class ClipEchchangeServer {

	String clipBoard = "empty";
	Map<String, Object> payload = new HashMap<>();

	public static void main(String[] args) {
		int p = 1512;
		if (args != null && args.length > 0) {
			try {
				p = Integer.valueOf(args[0]);
			} catch (NumberFormatException ex) {
				System.err.println("Usage: java -jar ClipEchangeServer.jar [portNumber] \nDefault Port is 1512");
			}
		}

		new ClipEchchangeServer().startServer(p);
	}

	public void startServer(int port) {
		final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);

		Runnable serverTask = new Runnable() {
			@Override
			public void run() {
				try {
					ServerSocket serverSocket = new ServerSocket(port);
					System.out.println("Waiting for clients to connect...");
					while (true) {
						Socket clientSocket = serverSocket.accept();
						clientProcessingPool.submit(new ClientTask(clientSocket));
					}
				} catch (IOException e) {
					System.err.println("Unable to process client request");
					e.printStackTrace();
				}
			}
		};
		Thread serverThread = new Thread(serverTask);
		serverThread.start();

	}

	private class ClientTask implements Runnable {
		private final Socket clientSocket;

		ObjectInputStream in;
		ObjectOutputStream out;

		private ClientTask(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			System.out.println("Got a client !");

			try {
				in = new ObjectInputStream(clientSocket.getInputStream());
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				while (true) {
					Request request = (Request) in.readObject();
					out.writeObject(processRequest(request));
				}

			} catch (IOException | ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Response processRequest(Request request) {
		//
		Response res = new Response("no request detected");
		switch (request.request) {
		case Request.COPY_CLIPBOARD_FROM_SERVER: {
			res = copyFromServer();
			break;
		}
		case Request.COPY_CLIPBOARD_TO_SERVER: {
			res = copyToServer(request);
			break;
		}
		case Request.COPY_FILE_FROM_SERVER: {
			res = copyFileFromServer(request);
			break;
		}
		case Request.REMOVE_FILE_FROM_SERVER: {
			res = removeFileFromServer(request);
			break;
		}
		case Request.COPY_FILE_TO_SERVER: {
			res = copyFileToServer(request);
			break;
		}
		}

		return res;
	}

	private Response copyFileToServer(Request request) {
		//
		File f = (File) request.payload;
		payload.put(f.getName(), f);
		System.out.println("Successfully copied " + request.payload + " to Server");
		return new Response("Successfully copied " + request.payload + " to Server");
	}

	private Response copyFileFromServer(Request request) {
		//
		if (request.payload != null) {
			String file = (String) request.payload;
			if (payload.containsKey(file)) {
				System.out.println("Returning: " + payload.get(file));
				return new Response(payload.get(file));
			} else {
				System.out.println("File " + file + " not found");
				return new Response("File " + file + " not found");
			}
		} else {
			List<String> list = new ArrayList<>();
			payload.keySet().forEach(file -> list.add(file));
			System.out.println("Returning: " + list);
			return new Response(list);
		}
	}

	private Response removeFileFromServer(Request request) {
		//
		if (request.payload != null) {
			String file = (String) request.payload;
			if (payload.containsKey(file)) {
				payload.remove(file);
				return new Response("File " + file + " removed");
			} else {
				System.out.println("File " + file + " not found");
				return new Response("File " + file + " not found");
			}
		} else {
			List<String> list = new ArrayList<>();
			payload.keySet().forEach(file -> list.add(file));
			System.out.println("Returning: " + list);
			return new Response(list);
		}
	}

	private Response copyToServer(Request request) {
		//
		clipBoard = (String) request.payload;
		return new Response(request.payload + " copied to clipboard!");
	}

	private Response copyFromServer() {
		//
		return new Response(clipBoard);
	}
}
