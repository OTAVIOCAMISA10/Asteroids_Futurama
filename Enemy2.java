import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.ImageIcon;

public class Enemy2 {

    private Image imagem;
    private int x, y;
    private int largura, altura;
    private boolean visivel;

    private static int VELOCIDADE = 10;

    public Enemy2(int x, int y) {
        this.x = x;
        this.y = y;
        this.visivel = true;
        load(); // carrega a imagem uma única vez
    }

    private void load() {
        ImageIcon referencia = new ImageIcon("C:\\Users\\Zoro\\Desktop\\teste-main\\Imagem\\ASTEROID.png");
        imagem  = referencia.getImage();
        largura = imagem.getWidth(null);
        altura  = imagem.getHeight(null);
    }

    public void update() {
        this.x -= VELOCIDADE; // anda para a esquerda
        // Fica invisível quando sai totalmente da tela pela esquerda
        if (this.x < -largura) {
            visivel = false;
        }
    }

    public  Rectangle getBounds () {
        return new Rectangle(x,y,largura,altura);
    }

    public boolean isVisivel() { return visivel; }
    public void setVisivel(boolean v) { this.visivel = v; }

    public static int getVELOCIDADE() { return VELOCIDADE; }
    public static void setVELOCIDADE(int v) { VELOCIDADE = v; }

    public int getX() { return x; }
    public int getY() { return y; }
    public Image getImagem() { return imagem; }
}
