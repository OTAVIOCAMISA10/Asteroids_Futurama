import java.awt.Image;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.ImageIcon;

public class Stars {

    private Image imagem;
    private int x, y;
    private int largura, altura;
    private boolean visivel;

    private static int VELOCIDADE = 3;

    public Stars(int x, int y) {
        this.x = x;
        this.y = y;
        this.visivel = true;
        load(); // carrega uma vez
    }

    private void load() {
        ImageIcon referencia = new ImageIcon("C:\\Users\\Zoro\\Desktop\\teste-main\\Imagem\\STAR.png");
        imagem  = referencia.getImage();
        largura = imagem.getWidth(null);
        altura  = imagem.getHeight(null);
    }
    

    public void update() {
        // move para a esquerda
        this.x -= VELOCIDADE;

        // se saiu totalmente da tela pela esquerda, reaparece à direita
        if (this.x < -largura) {
            // reaparece com deslocamento aleatório à frente
            int offset = ThreadLocalRandom.current().nextInt(100, 600);
            this.x = Fase.LARGURA + offset;

            // y aleatório dentro da altura jogável
            this.y = ThreadLocalRandom.current().nextInt(30, Fase.ALTURA - 30);
        }
    }

    public boolean isVisivel() { return visivel; }
    public void setVisivel(boolean v) { this.visivel = v; }

    public static int getVELOCIDADE() { return VELOCIDADE; }
    public static void setVELOCIDADE(int v) { VELOCIDADE = v; }

    public int getX() { return x; }
    public int getY() { return y; }
    public Image getImagem() { return imagem; }
}