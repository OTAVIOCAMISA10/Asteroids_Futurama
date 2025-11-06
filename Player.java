import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

public class Player {

    // Posição e velocidade
    protected int x, y;
    protected int dx, dy;

    // Sprites e dimensões cacheadas
    protected Image imgNormal;
    protected Image imgTurbo;
    protected int largura = 32;
    protected int altura  = 32;

    // Estado de vida/visibilidade
    protected boolean visivel = true;

    // --- TURBO / INVULNERABILIDADE ---
    // Energia de 0..1 (1 = cheio). Recarrega em 5s.
    private float turboEnergy = 1.0f; // começa cheio
    private boolean turbo = false;

    // Parâmetros do turbo
    private static final float FPS_APROX = 60f;          // Timer ~16ms
    private static final float RECHARGE_SECONDS = 5.0f;  // 5s para encher
    private static final float DRAIN_SECONDS    = 2.5f;  // ~2.5s de turbo contínuo

    private static final float RECHARGE_PER_TICK = 1f / (RECHARGE_SECONDS * FPS_APROX);
    private static final float DRAIN_PER_TICK    = 1f / (DRAIN_SECONDS    * FPS_APROX);

    // Multiplicador de velocidade quando turbo
    private static final float TURBO_SPEED_MULT = 1.8f;

    // Armas
    protected final List<Tiro> tiros = new ArrayList<>();

    public Player() {}

    /** Carrega sprites e cacheia dimensões */
    public void load(String pathNormal, String pathTurbo) {
        if (pathNormal != null) {
            ImageIcon icon = new ImageIcon(pathNormal);
            imgNormal = icon.getImage();
        }
        if (pathTurbo != null) {
            ImageIcon iconT = new ImageIcon(pathTurbo);
            imgTurbo = iconT.getImage();
        }
        Image ref = (imgNormal != null ? imgNormal : imgTurbo);
        if (ref != null) {
            int w = ref.getWidth(null), h = ref.getHeight(null);
            if (w > 0) largura = w;
            if (h > 0) altura  = h;
        }
    }

    /** Atualização por frame: aplica velocidade e administra turbo/energia */
    public void update() {
        // Move (com boost se turbo)
        if (turbo) {
            x += Math.round(dx * TURBO_SPEED_MULT);
            y += Math.round(dy * TURBO_SPEED_MULT);
        } else {
            x += dx;
            y += dy;
        }

        // Gerência de energia
        if (turbo) {
            turboEnergy -= DRAIN_PER_TICK;
            if (turboEnergy <= 0f) {
                turboEnergy = 0f;
                turbo = false; // fim do turbo quando esgota
            }
        } else {
            turboEnergy += RECHARGE_PER_TICK;
            if (turboEnergy > 1f) turboEnergy = 1f;
        }
    }

    // ==== Entrada humana (vazia por padrão; use PlayerHumano para teclado) ====
    public void keyPressed(KeyEvent e)  {}
    public void keyReleased(KeyEvent e) {}

    // ==== Armas / Turbo ====
    public void shoot() {
        int tx = x + largura;
        int ty = y + altura / 2;
        tiros.add(new Tiro(tx, ty));
    }

    public void tryTurbo() {
        if (!turbo && turboEnergy >= 1f) {
            turbo = true;
        }
    }

    // ==== Getters/Setters ====
    public Image getImagem() {
        if (turbo && imgTurbo != null) return imgTurbo;
        return (imgNormal != null ? imgNormal : imgTurbo);
    }
    public List<Tiro> getTiros() { return tiros; }

    public boolean isVisivel() { return visivel; }
    public void setVisivel(boolean v) { this.visivel = v; }

    /** Invulnerável enquanto turbo */
    public boolean isTurbo() { return turbo; }
    public boolean isInvulneravel() { return turbo; }

    /** 0..1 */
    public float getTurboEnergy() { return turboEnergy; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getLargura() { return largura; }
    public int getAltura()  { return altura;  }

    public Rectangle getBounds() { return new Rectangle(x, y, largura, altura); }

    public void setPosition(int nx, int ny) { this.x = nx; this.y = ny; }
    public void setDx(int dx) { this.dx = dx; }
    public void setDy(int dy) { this.dy = dy; }
}
