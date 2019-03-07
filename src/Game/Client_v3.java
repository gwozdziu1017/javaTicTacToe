package Game;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Klasa obslugujaca klienta - gracza
 */
public class Client_v3 {
    private JFrame frame = new JFrame("Gra 'Kółko i krzyżyk'");   // Nowe okno
    private JLabel messageLabel = new JLabel("");

    private Square[] board = new Square[9];     // tablica do gry
    private Square currentSquare;   // identyfikator biezacego kwadraciku

    private static int PORT = 8901;     // port do polaczenia
    private Socket socket;  // gniazdo
    private BufferedReader in;  // wejscie
    private PrintWriter out;    // wyjscie

    /**
     *
     */
    public Client_v3(String serverAddress) throws Exception {

        // Ustawienia polaczenia i strumieni we/wy
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        /** Ustawienia layout'u planszy do gry
         * Kolory i liczba kwadracikow
         * Mozna powiekszyc plansze do gry, ale wiaze sie to z uaktualnieniem warunkow sprawdzajacych czy jest zwyciezca lub remis
         */
        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, "South");

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(3, 3, 2, 2));
        for (int i = 0; i < board.length; i++) {
            final int j = i;
            board[i] = new Square();
            board[i].addMouseListener(new MouseAdapter() {  // sterowanie myszka
                public void mousePressed(MouseEvent e) {
                    currentSquare = board[j];
                    out.println("MOVE " + j);}});
            boardPanel.add(board[i]);
        }
        frame.getContentPane().add(boardPanel, "Center");
    }

    /**
     * Funkcja obslugujaca rozgrywke
     */
    public void play() throws Exception {
        String response;
        try {
            while (true) {
                response = in.readLine();
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Ruch przeciwnika");
                    currentSquare.setIcon("X");
                    currentSquare.repaint();
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(response.substring(15));
                    board[loc].setIcon(("O"));
                    board[loc].repaint();
                    messageLabel.setText("Twoja kolej!");
                } else if (response.startsWith("VICTORY")) {
                    messageLabel.setText("Wygrana!!");
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    messageLabel.setText("Przegrałeś :(");
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("Remis");
                    break;
                } else if (response.startsWith("MESSAGE")) {
                    messageLabel.setText(response.substring(8));
                }
            }
            out.println("Wyjście");
        }
        finally {
            socket.close();
        }
    }

    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame,
                "Grasz ponownie?",
                "Kółko i krzyżyk",
                JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

    /**
     * Graficzne przedstawienie planszy do gry
     */
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);

        public Square() {
            setBackground(Color.white);
            add(label);
        }

        /**
         * Ustawienie ikony dla gracza - X lub O
         */
        public void setIcon(String icon) {
            label.setText((icon));
        }
    }

    /**
     * Funckja main - ustawienie rozmiaru planszy i polaczenie do serwera
     * Plus sprawdzenie na koncu czy gracz kliknal "Tak" przy pytaniu o ponowna gre.
     */
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[1];
            Client_v3 client = new Client_v3((serverAddress));
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(240, 160);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }
}