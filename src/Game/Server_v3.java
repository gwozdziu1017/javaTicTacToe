package Game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Klasa obslugujaca serwer do ktorego chca sie polaczyc gracze
 * Paruje uzytkownikow
 * Zawiera tablice imitujaca plansze do gry
 * Sprawdza czy jest zwyciezca lub czy jest remis
 *
 * Komunikacja miedzy serwerem a klientem odbywa sie poprzez "slowa kluczowe" np message albo valid move
 */
public class Server_v3 {

    /**
     * Start aplikacji. Moze byc kilka dwojek graczy - program paruje ich kolejnoscia dolaczenia
     * Pierwszy gracz zawsze dostaje X - drugi O
     */
    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(8901);
        System.out.println("Serwer uruchomiony");
        try {
            while (true) {
                Game game = new Game();
                Game.Player playerX = game.new Player(listener.accept(), 'X');
                Game.Player playerO = game.new Player(listener.accept(), 'O');

                // parowanie uzytkownikow
                playerX.setOpponent(playerO);
                playerO.setOpponent(playerX);
                game.currentPlayer = playerX;

                // startowanie graczy
                playerX.start();
                playerO.start();
            }
        } finally {
            listener.close();   // w finally "zamykam nasluchiwanie" niezaleznie czy bylo polaczenie czy nie
        }
    }
}


class Game {

    // Plansza do gry - kwadrat 3x3
    private Player[] board = {
            null, null, null,
            null, null, null,
            null, null, null};

    Player currentPlayer;

    // Sprawdzanie kombinacji wygrywajacych
    public boolean hasWinner() {
        return
                (
                            board[0] != null && board[0] == board[1] && board[0] == board[2])
                        ||(board[3] != null && board[3] == board[4] && board[3] == board[5])
                        ||(board[6] != null && board[6] == board[7] && board[6] == board[8])
                        ||(board[0] != null && board[0] == board[3] && board[0] == board[6])
                        ||(board[1] != null && board[1] == board[4] && board[1] == board[7])
                        ||(board[2] != null && board[2] == board[5] && board[2] == board[8])
                        ||(board[0] != null && board[0] == board[4] && board[0] == board[8])
                        ||(board[2] != null && board[2] == board[4] && board[2] == board[6]
                );
    }

    // Sprawdzanie zapelnienia calej planszy
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sprawdzanie czy ruch ktory chce wykonac gracz jest mozliwy:
     * Czy gracz wykonujacy ruch to jest ten ktory ma teraz kolejke
     * Czy miejsce w ktore klika jest puste
     */
    public synchronized boolean legalMove(int location, Player player) {
        if (player == currentPlayer && board[location] == null) {
            board[location] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(location);
            return true;
        }
        return false;
    }

    /**
     * Klasa gracza "rozszerzajaca" dzialanie watku
     */
    class Player extends Thread {
        char mark;  // znak X lub O
        Player opponent;    // deklaracja przeciwnika dla gracza (class Player = this)
        Socket socket;      // socket
        BufferedReader input;   // "nasluchiwanie" wejscia
        PrintWriter output;     // wyjscie dla komunikatow

        /**
         * Konstruktor klasy Player
         * Ustalam gniazdo (socket) i znak gracza
         */
        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Oczekiwanie na przeciwnika...");  // przy nieparzystej liczbie graczy oczekuje na polaczenie "do pary"
            } catch (IOException e) {
                System.out.println("Błąd gracza: " + e);
            }
        }

        /**
         * Przypisanie przeciwnika dla gracza
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /**
         * Funkcja po ruchu przeciwnika
         * Wyswietla komunikat o wykonanym ruchu
         * Sprawdza jesli po ruchu przeciwnika jest zwyciezca znaczy ze gracz (this) przegral - wyswietla "defeat"
         * Jesli nie sprawdza czy cala tablica jest wypelniona i jesli tak wyswietla komunikat o remisie
         */
        public void otherPlayerMoved(int location) {
            output.println("OPPONENT_MOVED " + location);
            output.println(
                    hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }

        // Funckja Run() klasy
        public void run() {
            try {
                output.println("MESSAGE Ruch gracza");

                // Wyswietlanie informacji dla gracza (X) ze to jego kolej
                if (mark == 'X') {
                    output.println("MESSAGE Twoja kolej!");
                }

                /**
                *   Petla nieskonczona odpowiedzialna za logike gry
                 *   Sprawdza czy wystapil ruch
                 *   Czy jest zwyciezca czy remis itp
                 */
                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        if (legalMove(location, this))
                        {
                            output.println("VALID_MOVE");
                            output.println(hasWinner() ? "VICTORY"
                                    : boardFilledUp() ? "TIE"
                                    : "");
                        }
                    } else if (command.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Bład gracza: " + e);
            } finally
            {
                try
                {
                    socket.close();
                }
                catch (IOException e)
                {

                }
            }
        }
    }
}