import Controller.EscuelaController;
import Web.WebServer;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        EscuelaController controller = new EscuelaController();

        WebServer server = new WebServer(controller, 8080);

        try {
            server.start();
            System.out.println("Servidor iniciado.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}