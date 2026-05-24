import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servidor de Chat TCP/IP - APS UNIP 2026/1
 * Secretaria do Meio Ambiente - Monitoramento Rio Tietê
 *
 * Utiliza Sockets de Berkeley (ServerSocket/Socket) e Threads
 * para suportar múltiplos inspetores conectados simultaneamente.
 */
public class Servidor {

    private static final int PORTA = 54321;
    // Mapa thread-safe: apelido -> PrintWriter do cliente
    private static final ConcurrentHashMap<String, PrintWriter> clientes = new ConcurrentHashMap<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println("  SECRETARIA DO MEIO AMBIENTE - CHAT TIETÊ");
        System.out.println("  Servidor iniciado na porta " + PORTA);
        System.out.println("=================================================");

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                Socket socketCliente = serverSocket.accept();
                // Cada cliente recebe sua própria thread
                Thread t = new Thread(new ManipuladorCliente(socketCliente));
                t.setDaemon(true);
                t.start();
            }
        }
    }

    // Envia mensagem para TODOS os clientes conectados
    static void transmitirParaTodos(String mensagem) {
        String hora = sdf.format(new Date());
        String msgFinal = "[" + hora + "] " + mensagem;
        System.out.println(msgFinal); // log no servidor
        for (PrintWriter pw : clientes.values()) {
            pw.println(msgFinal);
        }
    }

    // Envia mensagem privada para um cliente específico
    static boolean enviarPrivado(String destinatario, String remetente, String texto) {
        PrintWriter pw = clientes.get(destinatario);
        if (pw != null) {
            String hora = sdf.format(new Date());
            pw.println("[" + hora + "] [PRIVADO de " + remetente + "]: " + texto);
            return true;
        }
        return false;
    }

    // Retorna lista de usuários online
    static String listarUsuarios() {
        if (clientes.isEmpty()) return "Nenhum inspetor conectado.";
        return "Inspetores online: " + String.join(", ", clientes.keySet());
    }

    // -----------------------------------------------------------------------
    // Classe interna: trata cada cliente em sua própria thread
    // -----------------------------------------------------------------------
    static class ManipuladorCliente implements Runnable {
        private final Socket socket;
        private PrintWriter saida;
        private BufferedReader entrada;
        private String apelido;

        ManipuladorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                saida   = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                // Protocolo de identificação
                saida.println("SERVIDOR: Bem-vindo ao Chat do Rio Tietê!");
                saida.println("SERVIDOR: Digite seu nome/identificação:");
                apelido = entrada.readLine();

                if (apelido == null || apelido.isBlank()) {
                    saida.println("SERVIDOR: Identificação inválida. Desconectando.");
                    socket.close();
                    return;
                }

                apelido = apelido.trim();

                if (clientes.containsKey(apelido)) {
                    saida.println("SERVIDOR: Nome já em uso. Tente novamente.");
                    socket.close();
                    return;
                }

                clientes.put(apelido, saida);
                transmitirParaTodos(">>> " + apelido + " entrou no sistema.");
                saida.println("SERVIDOR: Conectado! " + listarUsuarios());
                saida.println("SERVIDOR: Comandos: /privado <nome> <msg> | /usuarios | /sair");

                // Loop principal de leitura de mensagens
                String linha;
                while ((linha = entrada.readLine()) != null) {
                    linha = linha.trim();

                    if (linha.equalsIgnoreCase("/sair")) {
                        break;

                    } else if (linha.equalsIgnoreCase("/usuarios")) {
                        saida.println("SERVIDOR: " + listarUsuarios());

                    } else if (linha.startsWith("/privado ")) {
                        // Formato: /privado <destinatário> <mensagem>
                        String[] partes = linha.split(" ", 3);
                        if (partes.length < 3) {
                            saida.println("SERVIDOR: Uso: /privado <nome> <mensagem>");
                        } else {
                            boolean ok = enviarPrivado(partes[1], apelido, partes[2]);
                            if (ok) {
                                saida.println("SERVIDOR: Mensagem privada enviada para " + partes[1] + ".");
                            } else {
                                saida.println("SERVIDOR: Inspetor '" + partes[1] + "' não encontrado.");
                            }
                        }

                    } else if (!linha.isEmpty()) {
                        transmitirParaTodos("[" + apelido + "]: " + linha);
                    }
                }

            } catch (IOException e) {
                System.out.println("Conexão encerrada: " + (apelido != null ? apelido : "desconhecido"));
            } finally {
                desconectar();
            }
        }

        private void desconectar() {
            if (apelido != null) {
                clientes.remove(apelido);
                transmitirParaTodos("<<< " + apelido + " saiu do sistema.");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
