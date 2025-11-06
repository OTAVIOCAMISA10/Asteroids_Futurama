import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Fase extends JPanel implements ActionListener {

    public static final int LARGURA = 1024;
    public static final int ALTURA  = 728;

    private static final int MAX_ENEMY1 = 40;
    private static final int MAX_ENEMY2 = 25;
    private static final int MAX_ENEMY3 = 20;
    private static final int RESPAWN_MIN_OFFSET = 250;
    private static final int RESPAWN_MAX_OFFSET = 2000;

    // Projéteis
    private static final int MAX_BULLETS = 12;

    // Regras de spawn de rochas
    private static final int ROCK_SAFE_RADIUS   = 160;
    private static final int ROCK_MIN_SPACING   = 64;
    private static final int ROCK_SPAWN_TRIES   = 18;

    // HUD Turbo Bar
    private static final int HUD_X = 10, HUD_Y = 10, HUD_W = 300, HUD_H = 90;
    private static final int BAR_X = 20, BAR_Y = 64, BAR_W = 200, BAR_H = 12;

    // Assets
    private static final String SKIN_PLAYER = "C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\NAVE1.png";
    private static final String SKIN_PLAYER_TURBO = "C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\NAVE_TURBO.png";
    private static final String BG_PATH   = "C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\FUNDO.jpg";

    private Image fundo;
    private Timer timer;

    private Player player;
    private List<Enemy1> enemy1;
    private List<Enemy2> enemy2;
    private List<Enemy3> enemy3;
    private List<Stars>  stars;

    // HUD
    private long ticks = 0;
    private int kills  = 0;

    // NN
    private RedeNeural nn;
    private static final int NUM_INPUTS = 8;
    private static final int NUM_OUTPUTS = 3;
    private static final int HIDDEN_LAYERS = 1;
    private static final int HIDDEN_NEURONS = 10;

    // Suavização NN
    private double[] nnEma = new double[] {0, 0, 0};
    private static final double EMA_ALPHA_MOVE = 0.25;
    private static final double EMA_ALPHA_FIRE = 0.15;

    private float vx = 0f, vy = 0f;
    private static final float MAX_SPEED = 3.0f;
    private static final float ACCEL_STEP = 0.5f;

    private int shootCooldown = 0;
    private static final int SHOOT_COOLDOWN_TICKS = 6;

    private RedeNeural externalNN;
    private boolean humanControlled = true;

    // ========= GRADE ESPACIAL (broad-phase) =========
    private static final int CELL = 64;
    private static final int GRID_W = (LARGURA + CELL - 1) / CELL;
    private static final int GRID_H = (ALTURA  + CELL - 1) / CELL;
    @SuppressWarnings("unchecked")
    private final ArrayList<Enemy>[][] enemyGrid = new ArrayList[GRID_W][GRID_H];

    // -----------------------------------------------

    public Fase() {
        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setFocusable(true);
        setDoubleBuffered(true);

        ImageIcon referencia = new ImageIcon(BG_PATH);
        fundo = referencia.getImage();

        addKeyListener(new TecladoAdapter());

        nn = new RedeNeural(HIDDEN_LAYERS, NUM_INPUTS, HIDDEN_NEURONS, NUM_OUTPUTS);
        initGrid();

        startGame();

        timer = new Timer(16, this);
        timer.start();
    }

    public Fase(RedeNeural externalNN) {
        this.externalNN = externalNN;
        this.humanControlled = false;

        setPreferredSize(new Dimension(LARGURA, ALTURA));
        setFocusable(true);
        setDoubleBuffered(true);

        ImageIcon referencia = new ImageIcon(BG_PATH);
        fundo = referencia.getImage();

        addKeyListener(new TecladoAdapter());

        nn = externalNN;
        initGrid();

        startGame();

        timer = new Timer(16, this);
        timer.start();
    }

    // ===== Grid =====
    private void initGrid() {
        for (int gx = 0; gx < GRID_W; gx++) {
            for (int gy = 0; gy < GRID_H; gy++) {
                enemyGrid[gx][gy] = new ArrayList<>(16);
            }
        }
    }
    private static int cellX(int x) { int c = x / CELL; if (c < 0) c = 0; if (c >= GRID_W) c = GRID_W - 1; return c; }
    private static int cellY(int y) { int c = y / CELL; if (c < 0) c = 0; if (c >= GRID_H) c = GRID_H - 1; return c; }
    private void clearGrid() {
        for (int gx = 0; gx < GRID_W; gx++) {
            for (int gy = 0; gy < GRID_H; gy++) {
                enemyGrid[gx][gy].clear();
            }
        }
    }
    private void addEnemyToGrid(Enemy e) {
        if (!e.isVisivel()) return;
        int cx = cellX(e.getX());
        int cy = cellY(e.getY());
        enemyGrid[cx][cy].add(e);
    }
    private void fillEnemyGrid() {
        clearGrid();
        for (int i = 0; i < enemy1.size(); i++) addEnemyToGrid(enemy1.get(i));
        for (int i = 0; i < enemy2.size(); i++) addEnemyToGrid(enemy2.get(i));
        for (int i = 0; i < enemy3.size(); i++) addEnemyToGrid(enemy3.get(i));
    }

    // ===================== Inicialização =====================
    private void startGame() {
        ticks = 0;
        kills = 0;
        nnEma[0] = nnEma[1] = nnEma[2] = 0;
        vx = vy = 0f;
        shootCooldown = 0;

        player = new Player();
        player.load(SKIN_PLAYER, SKIN_PLAYER_TURBO);
        player.setPosition(80, ALTURA/2 - imgH(player.getImagem())/2);

        inicializaStars();
        inicializaInimigos();
        inicializaInimigos2();
        inicializaInimigos3();

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
    // Rochas: cantos OU meio com regras anti-frustração
    private void spawnEnemy3(int count) {
        for (int i = 0; i < count; i++) {
            int tries = ROCK_SPAWN_TRIES;
            int x = 0, y = 0;
            boolean ok = false;

            while (tries-- > 0 && !ok) {
                boolean canto = ThreadLocalRandom.current().nextBoolean();
                if (canto) {
                    int corner = ThreadLocalRandom.current().nextInt(4);
                    switch (corner) {
                        case 0 -> { x = 0;            y = ThreadLocalRandom.current().nextInt(0, ALTURA/4); }
                        case 1 -> { x = LARGURA-100;  y = ThreadLocalRandom.current().nextInt(0, ALTURA/4); }
                        case 2 -> { x = 0;            y = ALTURA-100; }
                        default -> { x = LARGURA-100; y = ALTURA-100; }
                    }
                } else {
                    x = ThreadLocalRandom.current().nextInt(50, LARGURA - 100);
                    y = ThreadLocalRandom.current().nextInt(30, ALTURA  - 100);
                }

                int pcx = player.getX() + imgW(player.getImagem())/2;
                int pcy = player.getY() + imgH(player.getImagem())/2;
                int rcx = x + 32, rcy = y + 32;
                int dx = rcx - pcx, dy = rcy - pcy;
                if ((dx*dx + dy*dy) < (ROCK_SAFE_RADIUS * ROCK_SAFE_RADIUS)) continue;

                boolean overlap = false;
                for (Enemy3 e : enemy3) {
                    int ecx = e.getX() + imgW(e.getImagem())/2;
                    int ecy = e.getY() + imgH(e.getImagem())/2;
                    int ddx = rcx - ecx, ddy = rcy - ecy;
                    if ((ddx*ddx + ddy*ddy) < (ROCK_MIN_SPACING * ROCK_MIN_SPACING)) { overlap = true; break; }
                }
                if (overlap) continue;
                ok = true;
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

        for (Stars s : stars) gg.drawImage(s.getImagem(), s.getX(), s.getY(), this);
        for (Tiro t : player.getTiros()) if (t.isVisivel()) gg.drawImage(t.getImagem(), t.getX(), t.getY(), this);

        if (player.isVisivel()) gg.drawImage(player.getImagem(), player.getX(), player.getY(), this);

        for (Enemy1 e : enemy1) if (e.isVisivel()) gg.drawImage(e.getImagem(), e.getX(), e.getY(), this);
        for (Enemy2 e : enemy2) if (e.isVisivel()) gg.drawImage(e.getImagem(), e.getX(), e.getY(), this);
        for (Enemy3 e : enemy3) if (e.isVisivel()) gg.drawImage(e.getImagem(), e.getX(), e.getY(), this);

        // HUD + Turbo bar
        gg.setFont(new Font("Arial", Font.BOLD, 14));
        gg.setColor(new Color(0, 0, 0, 160));
        gg.fillRoundRect(HUD_X, HUD_Y, HUD_W, HUD_H, 10, 10);
        gg.setColor(Color.WHITE);
        gg.drawString(String.format("Vivo: %s | Kills: %d", player.isVisivel() ? "Sim" : "Não", kills), 20, 28);
        gg.drawString(String.format("Ticks: %d", ticks), 20, 46);

        float energy = player.getTurboEnergy();
        gg.setColor(Color.LIGHT_GRAY);
        gg.fillRoundRect(BAR_X, BAR_Y, BAR_W, BAR_H, 6, 6);
        gg.setColor(player.isTurbo() ? new Color(0, 200, 255) : (energy >= 1f ? new Color(0, 220, 120) : new Color(140, 140, 140)));
        gg.fillRoundRect(BAR_X, BAR_Y, Math.round(BAR_W * energy), BAR_H, 6, 6);
        gg.setColor(Color.WHITE);
        gg.drawString("Turbo", BAR_X + BAR_W + 10, BAR_Y + BAR_H - 2);

        Toolkit.getDefaultToolkit().sync();
    }

    // ===================== Atualização =====================
    @Override
    public void actionPerformed(ActionEvent e) {
        ticks++;

        // Controle por NN (apenas no modo IA)
        if (!humanControlled && player.isVisivel()) {
            double[] inputs = getInputsForNN();
            double[] raw = nn.feedForward(inputs);

            nnEma[0] += EMA_ALPHA_MOVE * (raw[0] - nnEma[0]);
            nnEma[1] += EMA_ALPHA_MOVE * (raw[1] - nnEma[1]);
            nnEma[2] += EMA_ALPHA_FIRE * (raw[2] - nnEma[2]);

            float targetVx = (float)(nnEma[0] * MAX_SPEED);
            float targetVy = (float)(nnEma[1] * MAX_SPEED);

            float stepX = clamp(targetVx - vx, -ACCEL_STEP, ACCEL_STEP);
            float stepY = clamp(targetVy - vy, -ACCEL_STEP, ACCEL_STEP);
            vx += stepX; vy += stepY;

            player.setDx(Math.round(vx));
            player.setDy(Math.round(vy));

            double speed = Math.hypot(vx, vy);
            if (speed > 2.5) player.tryTurbo(); // só ativa se barra cheia (regra no Player)

            if (shootCooldown > 0) shootCooldown--;
            if (nnEma[2] > 0.65 && shootCooldown == 0 && player.getTiros().size() < MAX_BULLETS) {
                player.shoot();
                shootCooldown = SHOOT_COOLDOWN_TICKS;
            }
        }

        if (player.isVisivel()) player.update();
        confinaPlayerNasBordas();

        int worldSteps = player.isTurbo() ? 2 : 1; // acelera mundo no turbo (sem aumentar custo fora do turbo)
        for (int step = 0; step < worldSteps; step++) {
            worldStep();
        }

        repaint();
    }

    private void worldStep() {
        for (Stars s : stars) s.update();

        List<Tiro> tiros = player.getTiros();
        for (int i = tiros.size() - 1; i >= 0; i--) {
            Tiro t = tiros.get(i);
            if (t.isVisivel()) t.update();
            else tiros.remove(i);
        }

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

        // >>> Broad-phase: enche a grade e colide só nas células relevantes
        fillEnemyGrid();
        checkCollisionsGrid();

        if (!player.isVisivel()) startGame();
    }

    private void confinaPlayerNasBordas() {
        int pw = imgW(player.getImagem());
        int ph = imgH(player.getImagem());
        int nx = Math.max(0, Math.min(player.getX(), LARGURA - pw));
        int ny = Math.max(0, Math.min(player.getY(), ALTURA  - ph));

        if (nx != player.getX() || ny != player.getY()) {
            player.setPosition(nx, ny);
            vx = vy = 0f;
            player.setDx(0); player.setDy(0);
        }
    }

    // ==== NN Inputs ====
    private double[] getInputsForNN() {
        double[] inputs = new double[NUM_INPUTS];
        inputs[0] = player.getX() / (double) LARGURA;
        inputs[1] = player.getY() / (double) ALTURA;

        Enemy1 c1 = getClosestEnemy(enemy1, player.getX(), player.getY());
        Enemy2 c2 = getClosestEnemy(enemy2, player.getX(), player.getY());
        Enemy3 c3 = getClosestEnemy(enemy3, player.getX(), player.getY());

        inputs[2] = (c1 != null) ? c1.getX() / (double) LARGURA : -1;
        inputs[3] = (c1 != null) ? c1.getY() / (double) ALTURA  : -1;
        inputs[4] = (c2 != null) ? c2.getX() / (double) LARGURA : -1;
        inputs[5] = (c2 != null) ? c2.getY() / (double) ALTURA  : -1;
        inputs[6] = (c3 != null) ? c3.getX() / (double) LARGURA : -1;
        inputs[7] = (c3 != null) ? c3.getY() / (double) ALTURA  : -1;
        return inputs;
    }

    private <T extends Enemy> T getClosestEnemy(List<T> enemies, int px, int py) {
        T closest = null;
        long minDist2 = Long.MAX_VALUE;
        for (int i = 0; i < enemies.size(); i++) {
            T e = enemies.get(i);
            if (!e.isVisivel()) continue;
            int dx = e.getX() - px;
            int dy = e.getY() - py;
            long d2 = (long)dx * dx + (long)dy * dy;
            if (d2 < minDist2) { minDist2 = d2; closest = e; }
        }
        return closest;
    }

    // ==== Helpers imagem ====
    private static int imgW(Image img) { int w = (img != null ? img.getWidth(null) : -1); return w > 0 ? w : 32; }
    private static int imgH(Image img) { int h = (img != null ? img.getHeight(null) : -1); return h > 0 ? h : 32; }

    // ==== Colisão (usando grade) ====
    private void checkCollisionsGrid() {
        if (!player.isVisivel()) return;

        // Player vs inimigos (apenas células tocadas pelo player)
        final int px = player.getX(), py = player.getY();
        final int pw = imgW(player.getImagem()), ph = imgH(player.getImagem());
        int cminx = cellX(px), cmaxx = cellX(px + pw);
        int cminy = cellY(py), cmaxy = cellY(py + ph);

        outer:
        for (int cx = cminx; cx <= cmaxx; cx++) {
            for (int cy = cminy; cy <= cmaxy; cy++) {
                ArrayList<Enemy> bucket = enemyGrid[cx][cy];
                for (int i = 0; i < bucket.size(); i++) {
                    Enemy e = bucket.get(i);
                    if (!e.isVisivel()) continue;
                    int ex = e.getX(), ey = e.getY();
                    int ew = imgW(e.getImagem()), eh = imgH(e.getImagem());
                    if (intersects(px, py, pw, ph, ex, ey, ew, eh)) {
                        if (player.isTurbo()) {
                            e.setVisivel(false);
                            kills++;
                        } else {
                            player.setVisivel(false);
                            e.setVisivel(false);
                        }
                        break outer;
                    }
                }
            }
        }

        // Tiros vs inimigos (checa só as células do tiro)
        List<Tiro> tiros = player.getTiros();
        for (int j = 0; j < tiros.size(); j++) {
            Tiro t = tiros.get(j);
            if (!t.isVisivel()) continue;

            int tx = t.getX(), ty = t.getY();
            int tw = imgW(t.getImagem()), th = imgH(t.getImagem());
            int tminx = cellX(tx), tmaxx = cellX(tx + tw);
            int tminy = cellY(ty), tmaxy = cellY(ty + th);

            boolean hit = false;
            for (int cx = tminx; cx <= tmaxx && !hit; cx++) {
                for (int cy = tminy; cy <= tmaxy && !hit; cy++) {
                    ArrayList<Enemy> bucket = enemyGrid[cx][cy];
                    for (int i = 0; i < bucket.size(); i++) {
                        Enemy e = bucket.get(i);
                        if (!e.isVisivel()) continue;
                        int ex = e.getX(), ey = e.getY();
                        int ew = imgW(e.getImagem()), eh = imgH(e.getImagem());

                        if (intersects(tx, ty, tw, th, ex, ey, ew, eh)) {
                            // Rochas (Enemy3) bloqueiam o tiro
                            if (e instanceof Enemy3) {
                                t.setVisivel(false);
                            } else {
                                e.setVisivel(false);
                                t.setVisivel(false);
                                kills++;
                            }
                            hit = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean intersects(int ax, int ay, int aw, int ah,
                                      int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx &&
               ay < by + bh && ay + ah > by;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private class TecladoAdapter extends KeyAdapter {
        @Override public void keyPressed(KeyEvent e) { player.keyPressed(e); }
        @Override public void keyReleased(KeyEvent e) { player.keyReleased(e); }
    }

    public RedeNeural getNeuralNetwork() { return nn; }
    public int getKills() { return kills; }
    public Player getPlayer() { return player; }
}
