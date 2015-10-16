package de.maboware.clipexchange.server;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.maboware.clipexchange.model.Request;
import de.maboware.clipexchange.model.Response;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ClipExchangeClient extends Application {

	static Socket s = null;
	private static ObjectOutputStream out;
	private static ObjectInputStream in;

	private Stage stage;

	public static void main(String[] args) {
		Options opts = new Options();
		opts.addOption("url", true, "serverURL");
		opts.addOption("port", true, "portNumber");
		CommandLineParser parser = new DefaultParser();
		CommandLine commandline = null;
		try {
			commandline = parser.parse(opts, args);
		} catch (ParseException e) {
			System.err.println("Usage as Client: java -jar ClipEchange.jar -url=<serverURL> -port=<portNumber>");
			System.err
					.println("Usage as Client: java -jar ClipEchange.jar -url=<serverURL> -- using default port 1512");
			System.err.println("Usage as Server (and Client): java -jar ClipEchange.jar -port=<portNumber>");
			System.exit(1);
		}
		String serverURL = commandline.getOptionValue("url");
		int port = commandline.hasOption("port") ? Integer.valueOf(commandline.getOptionValue("port")) : 1512;
		if (serverURL == null) {
			startServer(port);
		}
		ClipExchangeClient client = new ClipExchangeClient();
		client.connectToServer(serverURL, port);
		Application.launch(args);
	}

	private static void startServer(int port) {
		//
		new ClipExchangeServer().startServer(port);

	}

	private void connectToServer(String serverURL, int port) {
		//

		try {
			s = new Socket(serverURL, port);
			out = new ObjectOutputStream(s.getOutputStream());
			in = new ObjectInputStream(s.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void start(Stage stage) throws Exception {
		//
		this.stage = stage;
		VBox vbox = new VBox();
		HBox hboxClipboard = new HBox();
		hboxClipboard.setPadding(new Insets(10, 10, 10, 10));
		hboxClipboard.setSpacing(10);
		hboxClipboard.setStyle("-fx-background-color: #336699;");
		Button buttonPush = new Button("push clipboard");
		buttonPush.setOnAction(e -> System.out.println(pushClipboard()));
		Button buttonCopy = new Button("copy clipoard");
		buttonCopy.setOnAction(e -> System.out.println(copyClipboard()));

		hboxClipboard.getChildren().add(buttonPush);
		hboxClipboard.getChildren().add(buttonCopy);

		HBox hboxFile = new HBox();
		hboxFile.setPadding(new Insets(10, 10, 10, 10));
		hboxFile.setSpacing(10);
		hboxFile.setStyle("-fx-background-color: #336699;");
		Button buttonPushFile = new Button("push file");
		buttonPushFile.setOnAction(e -> System.out.println(pushFile()));
		Button buttonCopyFile = new Button("get file");
		buttonCopyFile.setOnAction(e -> System.out.println(copyFile()));
		Button buttonRemoveFile = new Button("remove file");
		buttonRemoveFile.setOnAction(e -> System.out.println(removeFile()));

		hboxFile.getChildren().add(buttonPushFile);
		hboxFile.getChildren().add(buttonCopyFile);
		hboxFile.getChildren().add(buttonRemoveFile);

		vbox.getChildren().add(hboxClipboard);
		vbox.getChildren().add(hboxFile);
		Scene scene = new Scene(vbox);
		stage.setTitle("server: " + s.getInetAddress().getHostAddress());
		stage.setScene(scene);
		stage.setWidth(320);
		stage.setHeight(130);
		stage.show();

	}

	private Object copyFile() {

		stage.getScene().setCursor(Cursor.WAIT);
		Request req = new Request(Request.COPY_FILE_FROM_SERVER);
		Response res = null;
		try {
			out.writeObject(req);
			res = (Response) in.readObject();

			@SuppressWarnings("unchecked")
			List<String> choices = (List<String>) res.payload;

			if (checkNoFile(choices)) {
				return null;
			}

			ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
			dialog.setTitle("Auswahl Dialog");
			dialog.setHeaderText("Server-Files");
			dialog.setContentText("Bitte wählen:");

			// Traditional way to get the response value.
			Optional<String> result = dialog.showAndWait();

			req = new Request(Request.COPY_FILE_FROM_SERVER, result.get());
			System.out.println("Requesting copy of " + req.payload);
			out.writeObject(req);
			res = (Response) in.readObject();

			File f = (File) res.payload;

			try {
				Desktop.getDesktop().open(f);
			} catch (Exception ex) {
				File newFile = new File(System.getProperty("user.home") + "/" + f.getName());
				FileOutputStream fout = new FileOutputStream(newFile);
				FileInputStream fin = new FileInputStream(f);
				int read = fin.read();
				while (read > -1) {
					fout.write(read);
					read = fin.read();
				}
				fin.close();
				fout.close();
				stage.getScene().setCursor(Cursor.DEFAULT);

				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Information Dialog");
				alert.setHeaderText("Meldung vom Client");
				alert.setContentText(
						"Keine Standardanwendung für " + f.getName() + " gefunden.\n Es wurde eine lokale Kopie in "
								+ System.getProperty("user.home") + " erstellt.");
				alert.show();

			}

		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stage.getScene().setCursor(Cursor.DEFAULT);
		return res;
	}

	private boolean checkNoFile(List<String> choices) {
		if (choices == null || choices.size() == 0) {
			stage.getScene().setCursor(Cursor.DEFAULT);
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Information Dialog");
			alert.setHeaderText("Meldung vom Server");
			alert.setContentText("Mein File-Speicher ist leer");
			alert.show();
			return true;
		}
		return false;
	}

	private Object removeFile() {

		Request req = new Request(Request.REMOVE_FILE_FROM_SERVER);
		Response res = null;
		try {
			out.writeObject(req);
			res = (Response) in.readObject();

			@SuppressWarnings("unchecked")
			List<String> choices = (List<String>) res.payload;

			if (checkNoFile(choices)) {
				return null;
			}

			ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
			dialog.setTitle("Auswahl Dialog");
			dialog.setHeaderText("Server-Files");
			dialog.setContentText("Bitte wählen:");

			// Traditional way to get the response value.
			Optional<String> result = dialog.showAndWait();

			req = new Request(Request.REMOVE_FILE_FROM_SERVER, result.get());
			System.out.println("Requesting copy of " + req.payload);
			out.writeObject(req);
			res = (Response) in.readObject();

		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stage.getScene().setCursor(Cursor.DEFAULT);

		return res;
	}

	private Object pushFile() {
		stage.getScene().setCursor(Cursor.WAIT);

		Response res = null;
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		File file = fileChooser.showOpenDialog(stage);
		if (file != null) {
			Request req = new Request(Request.COPY_FILE_TO_SERVER, file);
			try {
				out.writeObject(req);
				res = (Response) in.readObject();
			} catch (IOException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		stage.getScene().setCursor(Cursor.DEFAULT);

		return res;
	}

	private Object copyClipboard() {
		//
		Request request = new Request(Request.COPY_CLIPBOARD_FROM_SERVER);
		Response res = null;
		try {
			System.out.println("Sending request...");
			out.writeObject(request);
			res = (Response) in.readObject();
			System.out.println("Response received: " + res.response);
			StringSelection selection = new StringSelection((String) res.payload);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
			System.out.println("Successfully copied '" + res.payload + "' to clipboard");
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stage.getScene().setCursor(Cursor.DEFAULT);

		return res;
	}

	private Object pushClipboard() {
		Request request = new Request(Request.COPY_CLIPBOARD_TO_SERVER);
		Response res = null;
		try {
			System.out.println("Sending request...");
			request.payload = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			out.writeObject(request);
			res = (Response) in.readObject();
			System.out.println("Response received: " + res.response);
		} catch (IOException | ClassNotFoundException | HeadlessException | UnsupportedFlavorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stage.getScene().setCursor(Cursor.DEFAULT);

		return res;
	}

}
