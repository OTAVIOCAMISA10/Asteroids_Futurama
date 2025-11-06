import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

public class Player {

    private int x, y;
    private int dx, dy;
    private Image imagem;
    private Image normalImg, turboImg;
    private int largura, altura;
    private final List<Tiro> tiros;
    private boolean isVisivel;

    // ===== TIRO / CADÊNCIA =====
    private int shootCooldown = 0;                       // ticks restantes para poder atirar de novo
    private static final int SHOOT_COOLDOWN_TICKS = 12;  // ~0,2s em ~60 FPS

    // ===== TURBO =====
    private boolean isTurbo = false;
    private int turboTicks = 0;           // frames restantes de turbo
    private int turboCooldown = 0;        // frames restantes de cooldown
    private static final int TURBO_DURATION   = 90;   // ~1.5s
    private static final int TURBO_COOLDOWN   = 150;  // ~2.5s
    private double speedMultiplier = 1.0;  // 2.0 quando turbo ON

    public int getTurboTicks()      { return turboTicks; }
    public int getTurboCooldown()   { return turboCooldown; }
    public int getTurboDurationMax(){ return TURBO_DURATION; }
    public int getTurboCooldownMax(){ return TURBO_COOLDOWN; }

    public Player() {
        this.x = 100;
        this.y = 100;
        isVisivel = true;
        this.tiros = new ArrayList<>();
    }

    // === load padrão usando os caminhos pedidos ===
    public void load() {
        load("C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\NAVE2.png",
             "C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\NAVE3.png");
    }

    // === load com caminhos customizados (útil p/ várias naves) ===
    public void load(String normalPath, String turboPath) {
        ImageIcon refNormal = new ImageIcon(normalPath);
        ImageIcon refTurbo  = new ImageIcon(turboPath);
        normalImg = refNormal.getImage();
        turboImg  = refTurbo.getImage();

        imagem  = normalImg;
        largura = imagem.getWidth(null);
        altura  = imagem.getHeight(null);
    }

    // Posiciona o player (para populações simultâneas)
    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public void update() {
        // --- Turbo ---
        if (turboTicks > 0) {
            turboTicks--;
            isTurbo = true;
            speedMultiplier = 2.0;
            imagem = turboImg;
        } else {
            isTurbo = false;
            speedMultiplier = 1.0;
            imagem = normalImg;
        }
        if (turboCooldown > 0) turboCooldown--;

        // --- Cooldown de tiro ---
        if (shootCooldown > 0) shootCooldown--;

        // --- Movimento ---
        x += (int) Math.round(dx * speedMultiplier);
        y += (int) Math.round(dy * speedMultiplier);

        // Limites
        x = Math.max(0, Math.min(x, Fase.LARGURA - largura));
        y = Math.max(0, Math.min(y, Fase.ALTURA  - altura));
    }

    // cria o projétil
    private void tiroSimples() {
        this.tiros.add(new Tiro(x + largura, y + (altura / 2)));
    }

    // Ativa o turbo se não estiver em cooldown
    private void tryStartTurbo() {
        if (turboCooldown == 0 && turboTicks == 0) {
            turboTicks    = TURBO_DURATION;
            turboCooldown = TURBO_COOLDOWN;
        }
    }
    // Permite que a IA peça turbo (respeita cooldown + duração)
    public void tryTurbo() {
        tryStartTurbo(); // já faz as checagens internas
    }


    // ===== API para IA/teclado =====
    public void setDx(int dx) { this.dx = dx; }
    public void setDy(int dy) { this.dy = dy; }

    // respeita cooldown e mantém “sem tiros no turbo”
    public void shoot() {
        if (isTurbo) return;
        if (shootCooldown == 0) {
            tiroSimples();
            shootCooldown = SHOOT_COOLDOWN_TICKS;
        }
    }

    public void keyPressed(KeyEvent tecla) {
        int codigo = tecla.getKeyCode();
        if (codigo == KeyEvent.VK_SPACE)  tryStartTurbo();
        if (codigo == KeyEvent.VK_A)      shoot();

        if (codigo == KeyEvent.VK_UP)      dy = -3;
        if (codigo == KeyEvent.VK_DOWN)    dy =  3;
        if (codigo == KeyEvent.VK_LEFT)    dx = -3;
        if (codigo == KeyEvent.VK_RIGHT)   dx =  3;
    }

    public void keyReleased(KeyEvent tecla) {
        int codigo = tecla.getKeyCode();
        if (codigo == KeyEvent.VK_UP)      dy = 0;
        if (codigo == KeyEvent.VK_DOWN)    dy = 0;
        if (codigo == KeyEvent.VK_LEFT)    dx = 0;
        if (codigo == KeyEvent.VK_RIGHT)   dx = 0;
    }

    // ===== Colisão / visibilidade =====
    public Rectangle getBounds () { return new Rectangle(x, y, largura, altura); }
    public boolean isVisivel() { return isVisivel; }
    public void setVisivel (boolean v) { this.isVisivel = v; }

    // ===== Getters =====
    public boolean isTurbo()   { return isTurbo; }
    public Image getImagem()   { return imagem; }
    public int getX()          { return x; }
    public int getY()          { return y; }
    public int getLargura()    { return largura; }
    public int getAltura()     { return altura; }
    public List<Tiro> getTiros() { return tiros; }
}
