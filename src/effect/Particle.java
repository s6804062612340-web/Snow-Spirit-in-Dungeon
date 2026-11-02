package effect;

import java.awt.*;

import main.Camera;

/**
 * อนุภาค 1 จุด (ประกายไฟ / เกล็ดหิมะ / ฝุ่น) ลอยไปตามความเร็ว แล้วค่อย ๆ จางและเล็กลง
 */
public class Particle {

    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private final double gravity;
    private final double startSize;
    private final Color color;
    private final int maxLife;
    private int life;

    // =========================
    // Constructor
    // =========================
    public Particle(double x, double y, double velocityX, double velocityY,
                    double size, Color color, int life, double gravity) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.startSize = size;
        this.color = color;
        this.maxLife = life;
        this.life = life;
        this.gravity = gravity;
    }

    // =========================
    // Update / Draw
    // =========================
    public void update() {
        x += velocityX;
        y += velocityY;
        velocityX *= 0.92;          // แรงต้านอากาศ
        velocityY = velocityY * 0.92 + gravity;
        life--;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void draw(Graphics g, Camera camera) {
        double t = (double) life / maxLife;            // 1 -> 0
        int size = Math.max(1, (int) (startSize * (0.4 + 0.6 * t)));
        int alpha = (int) (255 * Math.min(1, t * 1.5));

        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.fillOval(camera.screenX((int) x) - size / 2, camera.screenY((int) y) - size / 2, size, size);
    }
}
