import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Cliente Chat TCP/IP com Interface Gráfica (Java Swing)
 * APS UNIP 2026/1 - Secretaria do Meio Ambiente - Rio Tietê
 *
 * Conecta ao Servidor via Socket TCP/IP (Sockets de Berkeley).
 * Usa Thread separada para receber mensagens sem travar a UI.
 */
public class ClienteChat extends JFrame {

    // ── Configurações de conexão ──────────────────────────────────────────
    private static final String HOST_PADRAO = "localhost";
    private static final int    PORTA       = 54321;

    // ── Componentes gráficos ──────────────────────────────────────────────
    private JTextPane  areaChat;
    private JTextField campoMensagem;
    private JTextField campoDestinatario;
    private JButton    btnEnviar;
    private JButton    btnPrivado;
    private JButton    btnUsuarios;
    private JButton    btnSair;
    private JLabel     lblStatus;

    // ── Comunicação ───────────────────────────────────────────────────────
    private Socket        socket;
    private PrintWriter   saida;
    private BufferedReader entrada;
    private String        apelido;
    private boolean       conectado = false;

    // ── Cores do tema ─────────────────────────────────────────────────────
    private static final Color COR_FUNDO      = new Color(30, 30, 30);
    private static final Color COR_CHAT_FUNDO = new Color(20, 20, 20);
    private static final Color COR_TEXTO      = new Color(220, 220, 220);
    private static final Color COR_SERVIDOR   = new Color(100, 200, 100);
    private static final Color COR_PRIVADO    = new Color(255, 200, 80);
    private static final Color COR_PROPRIO    = new Color(100, 180, 255);
    private static final Color COR_OUTROS     = new Color(220, 220, 220);
    private static final Color COR_BOTAO      = new Color(0, 120, 212);
    private static final Color COR_BOTAO_SAI  = new Color(180, 40, 40);

    // ─────────────────────────────────────────────────────────────────────
    public ClienteChat() {
        configurarJanela();
        construirInterface();
        conectar();
    }

    // ── Configuração básica da JFrame ─────────────────────────────────────
    private void configurarJanela() {
        setTitle("Chat Rio Tietê – Secretaria do Meio Ambiente");
        setSize(800, 600);
        setMinimumSize(new Dimension(600, 450));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COR_FUNDO);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { sair(); }
        });
    }

    // ── Montagem da interface ─────────────────────────────────────────────
    private void construirInterface() {
        setLayout(new BorderLayout(5, 5));

        // ── Cabeçalho ──
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(new Color(0, 80, 140));
        cabecalho.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel titulo = new JLabel("🌊 Chat Rio Tietê – Monitoramento Ambiental");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(Color.WHITE);

        lblStatus = new JLabel("Conectando...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(180, 230, 180));

        cabecalho.add(titulo, BorderLayout.WEST);
        cabecalho.add(lblStatus, BorderLayout.EAST);
        add(cabecalho, BorderLayout.NORTH);

        // ── Área de chat ──
        areaChat = new JTextPane();
        areaChat.setEditable(false);
        areaChat.setBackground(COR_CHAT_FUNDO);
        areaChat.setFont(new Font("Consolas", Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(areaChat);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        add(scroll, BorderLayout.CENTER);

        // ── Painel inferior ──
        JPanel painelInferior = new JPanel(new BorderLayout(5, 5));
        painelInferior.setBackground(COR_FUNDO);
        painelInferior.setBorder(BorderFactory.createEmptyBorder(5, 8, 8, 8));

        // Linha de mensagem privada
        JPanel linhaPrivada = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        linhaPrivada.setBackground(COR_FUNDO);
        JLabel lblPara = new JLabel("Para (privado):");
        lblPara.setForeground(COR_TEXTO);
        lblPara.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        campoDestinatario = new JTextField(12);
        campoDestinatario.setToolTipText("Deixe em branco para mensagem geral");
        estilizarCampo(campoDestinatario);
        linhaPrivada.add(lblPara);
        linhaPrivada.add(campoDestinatario);

        // Linha da mensagem
        JPanel linhaMensagem = new JPanel(new BorderLayout(5, 0));
        linhaMensagem.setBackground(COR_FUNDO);

        campoMensagem = new JTextField();
        campoMensagem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        estilizarCampo(campoMensagem);
        campoMensagem.addActionListener(e -> enviarMensagem());

        // Botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        painelBotoes.setBackground(COR_FUNDO);

        btnEnviar   = criarBotao("Enviar ▶", COR_BOTAO);
        btnPrivado  = criarBotao("Privado 🔒", new Color(120, 80, 180));
        btnUsuarios = criarBotao("Usuários 👥", new Color(40, 120, 80));
        btnSair     = criarBotao("Sair ✖", COR_BOTAO_SAI);

        btnEnviar.addActionListener(e -> enviarMensagem());
        btnPrivado.addActionListener(e -> enviarPrivado());
        btnUsuarios.addActionListener(e -> solicitarUsuarios());
        btnSair.addActionListener(e -> sair());

        painelBotoes.add(btnUsuarios);
        painelBotoes.add(btnPrivado);
        painelBotoes.add(btnEnviar);
        painelBotoes.add(btnSair);

        linhaMensagem.add(campoMensagem, BorderLayout.CENTER);
        linhaMensagem.add(painelBotoes, BorderLayout.EAST);

        painelInferior.add(linhaPrivada, BorderLayout.NORTH);
        painelInferior.add(linhaMensagem, BorderLayout.CENTER);

        add(painelInferior, BorderLayout.SOUTH);
    }

    private JButton criarBotao(String texto, Color cor) {
        JButton btn = new JButton(texto);
        btn.setBackground(cor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setBackground(new Color(45, 45, 45));
        campo.setForeground(COR_TEXTO);
        campo.setCaretColor(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 80)),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    // ── Conexão ao servidor ───────────────────────────────────────────────
    private void conectar() {
        // Pede servidor/porta
        String host = JOptionPane.showInputDialog(this,
            "Endereço do servidor:", HOST_PADRAO);
        if (host == null || host.isBlank()) host = HOST_PADRAO;

        // Pede identificação do inspetor
        apelido = JOptionPane.showInputDialog(this,
            "Seu nome / identificação (ex: Inspetor_Carlos_Salesópolis):");
        if (apelido == null || apelido.isBlank()) {
            JOptionPane.showMessageDialog(this, "Identificação obrigatória.");
            System.exit(0);
        }

        try {
            socket  = new Socket(host, PORTA);
            saida   = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            // Responde ao protocolo de identificação do servidor
            // (o servidor pede o nome após a conexão)
            // Lê a saudação e o pedido de nome
            entrada.readLine(); // "Bem-vindo..."
            entrada.readLine(); // "Digite seu nome..."
            saida.println(apelido);

            conectado = true;
            lblStatus.setText("Conectado como: " + apelido + " | " + host + ":" + PORTA);
            setTitle("Chat Rio Tietê – " + apelido);

            // Thread de recepção de mensagens
            Thread receptor = new Thread(this::receberMensagens);
            receptor.setDaemon(true);
            receptor.start();

            appendMensagem("Sistema", "Conectado ao servidor! Bem-vindo, " + apelido + "!", COR_SERVIDOR);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Não foi possível conectar ao servidor:\n" + e.getMessage(),
                "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            lblStatus.setText("Desconectado");
        }
    }

    // ── Recepção contínua de mensagens (thread separada) ──────────────────
    private void receberMensagens() {
        try {
            String linha;
            while ((linha = entrada.readLine()) != null) {
                final String msg = linha;
                SwingUtilities.invokeLater(() -> processarMensagemRecebida(msg));
            }
        } catch (IOException e) {
            if (conectado) {
                SwingUtilities.invokeLater(() ->
                    appendMensagem("Sistema", "Conexão encerrada pelo servidor.", COR_SERVIDOR));
            }
        }
        conectado = false;
    }

    private void processarMensagemRecebida(String msg) {
        if (msg.contains("[PRIVADO")) {
            appendMensagem(null, msg, COR_PRIVADO);
        } else if (msg.startsWith("SERVIDOR:")) {
            appendMensagem(null, msg, COR_SERVIDOR);
        } else if (apelido != null && msg.contains("[" + apelido + "]:")) {
            appendMensagem(null, msg, COR_PROPRIO);
        } else {
            appendMensagem(null, msg, COR_OUTROS);
        }
    }

    // ── Envio de mensagem geral ───────────────────────────────────────────
    private void enviarMensagem() {
        if (!conectado) { mostrarErroConexao(); return; }
        String texto = campoMensagem.getText().trim();
        if (texto.isEmpty()) return;
        saida.println(texto);
        campoMensagem.setText("");
        campoMensagem.requestFocus();
    }

    // ── Envio de mensagem privada ─────────────────────────────────────────
    private void enviarPrivado() {
        if (!conectado) { mostrarErroConexao(); return; }
        String dest  = campoDestinatario.getText().trim();
        String texto = campoMensagem.getText().trim();
        if (dest.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha o campo 'Para' com o nome do destinatário.");
            return;
        }
        if (texto.isEmpty()) return;
        saida.println("/privado " + dest + " " + texto);
        appendMensagem(null, "[VOCÊ → " + dest + " (privado)]: " + texto, COR_PRIVADO);
        campoMensagem.setText("");
        campoMensagem.requestFocus();
    }

    private void solicitarUsuarios() {
        if (!conectado) { mostrarErroConexao(); return; }
        saida.println("/usuarios");
    }

    private void mostrarErroConexao() {
        JOptionPane.showMessageDialog(this, "Sem conexão com o servidor.", "Erro", JOptionPane.WARNING_MESSAGE);
    }

    // ── Encerra conexão e fecha janela ────────────────────────────────────
    private void sair() {
        if (conectado && saida != null) {
            saida.println("/sair");
        }
        conectado = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        dispose();
        System.exit(0);
    }

    // ── Adiciona texto colorido na área de chat ───────────────────────────
    private void appendMensagem(String prefixo, String texto, Color cor) {
        StyledDocument doc = areaChat.getStyledDocument();
        Style style = areaChat.addStyle("style_" + System.nanoTime(), null);
        StyleConstants.setForeground(style, cor);
        StyleConstants.setFontFamily(style, "Consolas");
        StyleConstants.setFontSize(style, 13);
        try {
            String linha = (prefixo != null ? "[" + prefixo + "] " : "") + texto + "\n";
            doc.insertString(doc.getLength(), linha, style);
            areaChat.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {}
    }

    // ── Ponto de entrada ──────────────────────────────────────────────────
    public static void main(String[] args) {
        // Visual moderno do sistema operacional
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ClienteChat cliente = new ClienteChat();
            cliente.setVisible(true);
        });
    }
}
