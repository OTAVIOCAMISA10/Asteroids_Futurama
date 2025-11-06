import java.awt.Image;
import java.awt.Rectangle;

// Interface comum para inimigos
public interface Enemy {
    boolean isVisivel();
    void setVisivel(boolean v);
    int getX();
    int getY();
    Rectangle getBounds();
    Image getImagem();
}