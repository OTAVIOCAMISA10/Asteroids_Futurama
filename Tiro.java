import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.ImageIcon;

public class Tiro {

    private Image imagem;
    private int x, y;
    private int largura, altura;
    private boolean visivel;

    private static final int LIMITE_X = Fase.LARGURA; 
    private static int VELOCIDADE = 10;

    public Tiro(int x, int y) {
        this.x = x;
        this.y = y;
        this.visivel = true;
        load(); 
    }

    private void load() {
        ImageIcon referencia = new ImageIcon("C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\TIRO_ALIADO.png");
        imagem  = referencia.getImage();
        largura = imagem.getWidth(null);
        altura  = imagem.getHeight(null);
    }

    public void update() {
        this.x += VELOCIDADE;
        if (this.x > LIMITE_X) {
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

    public int getLargura() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLargura'");
    }
}