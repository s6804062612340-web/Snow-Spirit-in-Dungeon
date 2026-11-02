package effect;

import java.awt.*;

import main.Camera;

/**
 * วงคลื่นกระแทก ขยายออกจากจุดกลางแล้วจางหาย (ใช้ตอน Boss ทุบพื้น / ศัตรูตาย / Dash)
 */
public class Ring {

    private final int x;
    private final int y;
    private final int maxRadius;
    private final Color color;
    private final int maxLife;
    private int life;

    public Ring(int x, int y, int maxRadius, Color color, int life) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.color = color;
        this.maxLife = life;
        this.life = life;
    }

    public void update() {
        life--;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void draw(Graphics g, Camera camera) {
        double t = 1 - (double) life / maxLife;        // 0 -> 1
        int radius = (int) (maxRadius * (0.2 + 0.8 * t));
        int alpha = (int) (220 * (1 - t));

        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        int screenX = camera.screenX(x) - radius;
        int screenY = camera.screenY(y) - radius;
        // วาด 3 วงซ้อน ให้เส้นดูหนา
        for (int i = 0; i < 3; i++) {
            g.drawOval(screenX + i, screenY + i, radius * 2 - i * 2, radius * 2 - i * 2);
        }
    }
}
