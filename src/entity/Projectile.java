package entity;

import java.awt.*;

import main.GamePanel;

/**
 * กระสุนที่ Player ยิงออกไป วิ่งเป็นเส้นตรงตามทิศที่เล็ง
 */
public class Projectile {

    private static final double SPEED = 8;
    private static final int SIZE = 12;     // ขนาดรูป = ขนาด Hitbox

    private final GamePanel gamePanel;
    private double x;
    private double y;
    private final double velocityX;
    private final double velocityY;
    private final int damage;
    private final Rectangle hitbox;

    // =========================
    // Constructor
    // =========================
    public Projectile(GamePanel gamePanel, double x, double y,
                      double directionX, double directionY, int damage) {
        this.gamePanel = gamePanel;
        // (x, y) ที่ส่งมาคือจุดกลาง -> แปลงเป็นมุมซ้ายบน
        this.x = x - SIZE / 2.0;
        this.y = y - SIZE / 2.0;
        this.damage = damage;

        // Normalize ทิศทาง แล้วคูณความเร็ว
        double length = Math.sqrt(directionX * directionX + directionY * directionY);
        if (length == 0) {
            length = 1;
        }
        velocityX = directionX / length * SPEED;
        velocityY = directionY / length * SPEED;

        hitbox = new Rectangle((int) this.x, (int) this.y, SIZE, SIZE);
    }

    // =========================
    // Update / Draw
    // =========================
    public void update() {
        x += velocityX;
        y += velocityY;
        hitbox.x = (int) x;
        hitbox.y = (int) y;
    }

    public void draw(Graphics g) {
        int screenX = gamePanel.getCamera().screenX((int) x);
        int screenY = gamePanel.getCamera().screenY((int) y);

        g.setColor(Color.CYAN);
        g.fillOval(screenX, screenY, SIZE, SIZE);

        if (gamePanel.isDebugMode()) {
            g.drawRect(
                    gamePanel.getCamera().screenX(hitbox.x),
                    gamePanel.getCamera().screenY(hitbox.y),
                    hitbox.width, hitbox.height);
        }
    }

    // =========================
    // Getters
    // =========================
    public double getX() { return x; }
    public double getY() { return y; }
    public int getDamage() { return damage; }
    public Rectangle getHitbox() { return hitbox; }
}
