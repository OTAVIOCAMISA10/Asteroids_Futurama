// StartScreen.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StartScreen extends JPanel {

    private static final String IMAGE_PATH = "C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\PLAY.jpg";
    private final JFrame frame;
    private final Image bg;

    public StartScreen(JFrame frame) {
        this.frame = frame;
        setPreferredSize(new Dimension(Fase.LARGURA, Fase.ALTURA));
        setFocusable(true);
        bg = new ImageIcon(IMAGE_PATH).getImage();

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { startGame(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { startGame(); }
        });
    }

    private void startGame() {
        Fase fase = new Fase();
        frame.setContentPane(fase);
        frame.revalidate();
        frame.repaint();
        SwingUtilities.invokeLater(fase::requestFocusInWindow);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    // >>> PONTO DE ENTRADA (sem Game.java)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Asteroids Futurama");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(Fase.LARGURA, Fase.ALTURA);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new StartScreen(frame));
            frame.setVisible(true);
        });
    }
}
