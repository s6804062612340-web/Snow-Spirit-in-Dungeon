package entity.enemy;

import java.awt.*;

import main.Camera;
import main.GamePanel;

/**
 * กระสุนของศัตรูตีไกล (Slime พ่นเมือก / Skeleton ปากระดูก)
 * วิ่งตรงไปทางที่เล็งไว้ตอนยิง โดนกำแพงหรือโดน Player แล้วหายไป
 * Hitbox = ขนาดลูกกระสุนที่วาด
 */
public class EnemyProjectile {

    private final GamePanel gamePanel;
    private double x;               // มุมซ้ายบน
    private double y;
    private final double velocityX;
    private final double velocityY;
    private final int size;
    private final int damage;
    private final Color color;
    private final Rectangle hitbox;
    private int age = 0;

    // =========================
    // Constructor
    // =========================
    /** (centerX, centerY) = จุดปล่อยกระสุน, (targetX, targetY) = จุดที่เล็ง */
    public EnemyProjectile(GamePanel gamePanel, int centerX, int centerY,
                           int targetX, int targetY, double speed, int size, int damage, Color color) {
        this.gamePanel = gamePanel;
        this.size = size;
        this.damage = damage;
        this.color = color;
        this.x = centerX - size / 2.0;
        this.y = centerY - size / 2.0;

        double dx = targetX - centerX;
        double dy = targetY - centerY;
        double length = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        velocityX = dx / length * speed;
        velocityY = dy / length * speed;

        hitbox = new Rectangle((int) x, (int) y, size, size);
    }

    // =========================
    // Update / Draw
    // =========================
    public void update() {
        x += velocityX;
        y += velocityY;
        hitbox.x = (int) x;
        hitbox.y = (int) y;
        age++;

        // หางกระสุน
        if (age % 2 == 0) {
            gamePanel.getEffects().trail(getCenterX(), getCenterY(), color, size * 0.6);
        }
    }

    public void draw(Graphics g) {
        Camera camera = gamePanel.getCamera();
        int screenX = camera.screenX((int) x);
        int screenY = camera.screenY((int) y);

        // แสงเรือง (ใหญ่กว่า + โปร่งแสง) แล้วตามด้วยตัวกระสุน
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
        g.fillOval(screenX - 4, screenY - 4, size + 8, size + 8);
        g.setColor(color);
        g.fillOval(screenX, screenY, size, size);
        g.setColor(new Color(255, 255, 255, 200));
        g.fillOval(screenX + size / 4, screenY + size / 4, size / 3, size / 3);

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.RED);
            g.drawRect(camera.screenX(hitbox.x), camera.screenY(hitbox.y), hitbox.width, hitbox.height);
        }
    }

    // =========================
    // Getters
    // =========================
    public Rectangle getHitbox() { return hitbox; }
    public int getDamage() { return damage; }
    public Color getColor() { return color; }
    public int getCenterX() { return (int) x + size / 2; }
    public int getCenterY() { return (int) y + size / 2; }
}
