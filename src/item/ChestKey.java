package item;

import java.awt.*;

import main.Camera;
import main.GamePanel;
import sound.Sound;

/**
 * กุญแจหีบสมบัติ (สีเงิน-ฟ้า ต่างจากกุญแจประตูสีทอง)
 * ดรอปจากศัตรูที่มีไอคอนกุญแจเหนือหัว เดินชนแล้วเก็บ 1 ดอกเปิดหีบได้ 1 ใบ
 * เก็บสะสมข้ามด่านได้
 */
public class ChestKey extends Item {

    public static final int WIDTH = 32;
    public static final int HEIGHT = 20;

    private int age = 0;

    // =========================
    // Constructor
    // =========================
    /** (centerX, centerY) คือจุดที่กุญแจตก */
    public ChestKey(GamePanel gamePanel, int centerX, int centerY) {
        super(gamePanel, centerX - WIDTH / 2, centerY - HEIGHT / 2, WIDTH, HEIGHT);
    }

    // =========================
    // Update
    // =========================
    @Override
    public void update() {
        if (collected) {
            return;
        }
        age++;
        updateHitbox();

        if (isPlayerTouching()) {
            collected = true;
            gamePanel.getPlayer().addChestKey();
            gamePanel.getEffects().sparkle(x + width / 2, y + height / 2, 14, new Color(170, 220, 255));
            Sound.KEY.play();
            System.out.println("Chest Key Collected!");
        }
    }

    // =========================
    // Draw
    // =========================
    @Override
    public void draw(Graphics g) {
        if (collected) {
            return;
        }
        Camera camera = gamePanel.getCamera();
        int sx = camera.screenX(x);
        int sy = camera.screenY(y) + (int) (3 * Math.sin(age * 0.1));   // ลอยขึ้นลงเบา ๆ

        g.setColor(new Color(160, 210, 255, 60));
        g.fillOval(sx - 10, sy - 14, WIDTH + 20, HEIGHT + 28);
        drawShape(g, sx, sy - 6, WIDTH);

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.WHITE);
            g.drawRect(camera.screenX(hitbox.x), camera.screenY(hitbox.y), hitbox.width, hitbox.height);
        }
    }

    /**
     * วาดรูปกุญแจหีบกว้าง size px (ใช้ทั้งบนพื้น, เหนือหัวศัตรู และใน HUD)
     * static เพราะไม่ต้องใช้ข้อมูลของกุญแจดอกไหนเลย
     */
    public static void drawShape(Graphics g, int x, int y, int size) {
        int ring = size * 5 / 8;
        int shaftY = y + ring / 2 - size / 12;
        Color body = new Color(185, 215, 240);
        Color dark = new Color(40, 60, 90);

        g.setColor(dark);
        g.fillOval(x - 1, y - 1, ring + 2, ring + 2);
        g.fillRect(x + ring - 3, shaftY - 1, size - ring + 4, size / 6 + 2);
        g.setColor(body);
        g.fillOval(x, y, ring, ring);
        g.fillRect(x + ring - 2, shaftY, size - ring + 2, size / 6);
        g.fillRect(x + size - size / 5, shaftY, size / 8, size / 3);
        g.fillRect(x + size - size * 2 / 5, shaftY, size / 8, size / 4);
        g.setColor(dark);
        g.fillOval(x + ring / 3, y + ring / 3, ring / 3, ring / 3);
    }
}
