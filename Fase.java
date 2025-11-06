import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Fase extends JPanel implements ActionListener {

    public static final int LARGURA = 1024;
    public static final int ALTURA  = 728;

    private static final int MAX_ENEMY1 = 40;
    private static final int MAX_ENEMY2 = 25;
    private static final int MAX_ENEMY3 = 20;   // Rochas
    private static final int RESPAWN_MIN_OFFSET = 250;
    private static final int RESPAWN_MAX_OFFSET = 2000;

    // Assets
    private static final String SKIN_PLAYER = "C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\NAVE2.png";
    private static final String SKIN_PLAYER_TURBO = "C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\NAVE3.png";
    private static final String BG_PATH   = "C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\FUNDO.jpg";

    private Image fundo;
    private Timer timer;

    private Player player;
    private List<Enemy1> enemy1;
    private List<Enemy2> enemy2;
    private List<Enemy3> enemy3;
    private List<Stars>  stars;

    // HUD simples
    private long ticks = 0;
    private int kills  = 0;

    public Fase() {
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setFocusable(true);
        setDoubleBuffered(true);

        ImageIcon referencia = new ImageIcon(BG_PATH);
        fundo = referencia.getImage();

        addKeyListener(new TecladoAdapter());

        startGame();

        timer = new Timer(16, this); // ~60 FPS
        timer.start();
    }

    // ===================== Inicialização =====================
    private void startGame() {
        ticks = 0;
        kills = 0;

        // Player
        player = new Player();
        player.load(SKIN_PLAYER, SKIN_PLAYER_TURBO);
        player.setPosition(80, ALTURA/2 - player.getAltura()/2);

        // Fundo/estrelas
        inicializaStars();

        // Inimigos
        inicializaInimigos();
        inicializaInimigos2();
        inicializaInimigos3();

        // Tiro baseline
        Tiro.setVELOCIDADE(10);
    }

    private void inicializaStars() {
        stars = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            int x = ThreadLocalRandom.current().nextInt(0, LARGURA);
            int y = ThreadLocalRandom.current().nextInt(0, ALTURA);
            stars.add(new Stars(x, y));
        }
    }

    private void inicializaInimigos() {
        enemy1 = new ArrayList<>();
        spawnEnemy1(MAX_ENEMY1);
    }

    private void inicializaInimigos2() {
        enemy2 = new ArrayList<>();
        spawnEnemy2(MAX_ENEMY2);
    }

    private void inicializaInimigos3() {
        enemy3 = new ArrayList<>();
        spawnEnemy3(MAX_ENEMY3);
    }

    private void spawnEnemy1(int count) {
        for (int i = 0; i < count; i++) {
            int x = LARGURA + ThreadLocalRandom.current().nextInt(RESPAWN_MIN_OFFSET, RESPAWN_MAX_OFFSET);
            int y = ThreadLocalRandom.current().nextInt(30, ALTURA - 78);
            enemy1.add(new Enemy1(x, y));
        }
    }

    private void spawnEnemy2(int count) {
        for (int i = 0; i < count; i++) {
            int x = LARGURA + ThreadLocalRandom.current().nextInt(RESPAWN_MIN_OFFSET, RESPAWN_MAX_OFFSET);
            int y = ThreadLocalRandom.current().nextInt(30, ALTURA - 78);
            enemy2.add(new Enemy2(x, y));
        }
    }

    private void spawnEnemy3(int count) {
        for (int i = 0; i < count; i++) {
            // Rochas preferem os cantos
            int x, y;
            int corner = ThreadLocalRandom.current().nextInt(4);
            switch (corner) {
                case 0 -> { x = 0;            y = ThreadLocalRandom.current().nextInt(0, ALTURA/4); }
                case 1 -> { x = LARGURA-100;  y = ThreadLocalRandom.current().nextInt(0, ALTURA/4); }
                case 2 -> { x = 0;            y = ALTURA-100; }
                default -> { x = LARGURA-100; y = ALTURA-100; }
            }
            enemy3.add(new Enemy3(x, y));
        }
    }

    // ===================== Desenho =====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D gg = (Graphics2D) g;

        gg.drawImage(fundo, 0, 0, this);

        // Estrelas
        for (Stars s : stars) gg.drawImage(s.getImagem(), s.getX(), s.getY(), this);

        // Tiros
        for (Tiro t : player.getTiros()) {
            if (t.isVisivel()) gg.drawImage(t.getImagem(), t.getX(), t.getY(), this);
        }

        // Player
        if (player.isVisivel()) {
            gg.drawImage(player.getImagem(), player.getX(), player.getY(), this);
        }

        // Inimigos
        for (Enemy1 e : enemy1) if (e.isVisivel()) gg.drawImage(e.getImagem(), e.getX(), e.getY(), this);
        for (Enemy2 e : enemy2) if (e.isVisivel()) gg.drawImage(e.getImagem(), e.getX(), e.getY(), this);
        for (Enemy3 e : enemy3) if (e.isVisivel()) gg.drawImage(e.getImagem(), e.getX(), e.getY(), this);

        // HUD simples (sem geração/IA)
        gg.setFont(new Font("Arial", Font.BOLD, 14));
        gg.setColor(new Color(0, 0, 0, 160));
        gg.fillRoundRect(10, 10, 260, 52, 10, 10);
        gg.setColor(Color.WHITE);
        gg.drawString(String.format("Vivo: %s | Kills: %d", player.isVisivel() ? "Sim" : "Não", kills), 20, 28);
        gg.drawString(String.format("Ticks: %d", ticks), 20, 46);
    }

    // ===================== Atualização =====================
    @Override
    public void actionPerformed(ActionEvent e) {
        ticks++;

        // Atualiza player
        if (player.isVisivel()) player.update();

        // Estrelas
        for (Stars s : stars) s.update();

        // Tiros
        List<Tiro> tiros = player.getTiros();
        for (int i = tiros.size() - 1; i >= 0; i--) {
            Tiro t = tiros.get(i);
            if (t.isVisivel()) t.update();
            else tiros.remove(i);
        }

        // Inimigos com respawn
        for (int i = enemy1.size() - 1; i >= 0; i--) {
            Enemy1 t = enemy1.get(i);
            if (t.isVisivel()) t.update();
            else { enemy1.remove(i); spawnEnemy1(1); }
        }

        for (int i = enemy2.size() - 1; i >= 0; i--) {
            Enemy2 t = enemy2.get(i);
            if (t.isVisivel()) t.update();
            else { enemy2.remove(i); spawnEnemy2(1); }
        }

        for (int i = enemy3.size() - 1; i >= 0; i--) {
            Enemy3 t = enemy3.get(i);
            if (t.isVisivel()) t.update();
            else { enemy3.remove(i); spawnEnemy3(1); }
        }

        // Colisões
        checkCollisions();

        // Se o player morreu, recomeça a fase
        if (!player.isVisivel()) {
            startGame();
        }

        repaint();
    }

    // ===================== Colisões =====================
    private void checkCollisions() {
        if (!player.isVisivel()) return;

        Rectangle formaNave = player.getBounds();

        // Player vs Enemy1
        for (Enemy1 e1 : enemy1) {
            if (e1.isVisivel() && formaNave.intersects(e1.getBounds())) {
                if (player.isTurbo()) {
                    e1.setVisivel(false);
                    kills++;
                } else {
                    player.setVisivel(false);
                    e1.setVisivel(false);
                }
                break;
            }
        }

        // Player vs Enemy2
        for (Enemy2 e2 : enemy2) {
            if (e2.isVisivel() && formaNave.intersects(e2.getBounds())) {
                if (player.isTurbo()) {
                    e2.setVisivel(false);
                    kills++;
                } else {
                    player.setVisivel(false);
                    e2.setVisivel(false);
                }
                break;
            }
        }

        // Player vs Enemy3 (rocha) => sempre morre
        for (Enemy3 e3 : enemy3) {
            if (e3.isVisivel() && formaNave.intersects(e3.getBounds())) {
                player.setVisivel(false);
                e3.setVisivel(false);
                break;
            }
        }

        // Tiros do player vs inimigos
        List<Tiro> tiros = player.getTiros();
        for (int j = 0; j < tiros.size(); j++) {
            Tiro t = tiros.get(j);
            if (!t.isVisivel()) continue;
            Rectangle rt = t.getBounds();

            // Enemy1
            for (Enemy1 e1 : enemy1) {
                if (e1.isVisivel() && rt.intersects(e1.getBounds())) {
                    e1.setVisivel(false);
                    t.setVisivel(false);
                    kills++;
                    break;
                }
            }

            // Enemy2
            for (Enemy2 e2 : enemy2) {
                if (e2.isVisivel() && rt.intersects(e2.getBounds())) {
                    e2.setVisivel(false);
                    t.setVisivel(false);
                    kills++;
                    break;
                }
            }

            // Enemy3 (rocha) bloqueia tiro
            for (Enemy3 e3 : enemy3) {
                if (e3.isVisivel() && rt.intersects(e3.getBounds())) {
                    t.setVisivel(false); // tiro desaparece
                    break;
                }
            }
        }
    }

    // ===================== Teclado =====================
    private class TecladoAdapter extends KeyAdapter {
        @Override public void keyPressed(KeyEvent e) { player.keyPressed(e); }
        @Override public void keyReleased(KeyEvent e) { player.keyReleased(e); }
    }
}
