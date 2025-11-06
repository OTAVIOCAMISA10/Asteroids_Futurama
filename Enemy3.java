import java.awt.Image;
import java.awt.Rectangle;
import javax.swing.ImageIcon;

public class Enemy3 implements Enemy {
    private Image imagem;
    private int x, y;
    private int largura, altura;
    private boolean visivel;

    private static final int VELOCIDADE = 6;  // Ajuste a velocidade conforme necessário

    public Enemy3(int x, int y) {
        this.x = x;
        this.y = y;
        this.visivel = true;
        load(); // Carrega a imagem da rocha
    }

    private void load() {
        // Carrega a imagem da rocha (ajuste o caminho conforme necessário)
        ImageIcon referencia = new ImageIcon("C:\\Users\\3001032\\Downloads\\Asteroids_Futurama-main\\Asteroids_Futurama-main\\Imagem\\ROCHA.png");
        imagem = referencia.getImage();
        largura = imagem.getWidth(null);
        altura = imagem.getHeight(null);
    }

    public void update() {
        this.x -= VELOCIDADE; // Movimento para a esquerda
        // Fica invisível quando sai da tela
        if (this.x < -largura) {
            visivel = false;
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, largura, altura);
    }

    public boolean isVisivel() {
        return visivel;
    }

    public void setVisivel(boolean v) {
        this.visivel = v;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Image getImagem() {
        return imagem;
    }

    public int getLargura() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLargura'");
    }
}