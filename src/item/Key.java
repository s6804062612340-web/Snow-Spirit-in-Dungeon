package item;

import java.awt.*;

import main.Camera;
import main.GamePanel;
import sound.Sound;

/**
 * กุญแจ: ดรอปเมื่อศัตรูตัวสุดท้ายตาย ใช้เปิดประตูไปด่านถัดไป
 */
public class Key extends Item {

    // =========================
    // Constructor
    // =========================
    /** (x, y) คือจุดกลางที่กุญแจตก */
    public Key(GamePanel gamePanel, int x, int y) {
        super(gamePanel, x - 20, y - 12, 40, 24);    // รูปกุญแจ 40x24 = Hitbox
    }

    // =========================
    // Update
    // =========================
    @Override
    public void update() {
        if (collected) {
            return;
        }

        updateHitbox();

        if (isPlayerTouching()) {
            collected = true;
            gamePanel.getPlayer().setHasKey(true);
            gamePanel.getEffects().sparkle(x + width / 2, y + height / 2, 20, Color.YELLOW);
            gamePanel.getEffects().ring(x + width / 2, y + height / 2, 50, Color.YELLOW, 18);
            Sound.KEY.play();
            System.out.println("Key Collected!");
        }
    }

    // =========================
    // Draw (รูปชั่วคราว)
    // =========================
    @Override
    public void draw(Graphics g) {
        if (collected) {
            return;
        }

        Camera camera = gamePanel.getCamera();

        // แสงเรืองกระพริบรอบกุญแจ ให้มองเห็นง่าย
        int glow = 16 + (int) (6 * Math.sin(System.nanoTime() / 150000000.0));
        g.setColor(new Color(255, 230, 80, 70));
        g.fillOval(camera.screenX(x + width / 2) - glow * 2, camera.screenY(y + height / 2) - glow * 2,
                glow * 4, glow * 4);

        // หัวกุญแจ (วงกลม 24x24) + ก้านกุญแจ -> รวมกว้าง 40 สูง 24 เท่ากับ Hitbox
        g.setColor(Color.YELLOW);
        g.fillOval(camera.screenX(x), camera.screenY(y), 24, 24);
        g.fillRect(camera.screenX(x + 20), camera.screenY(y + 8), 20, 8);
        g.fillRect(camera.screenX(x + 32), camera.screenY(y + 16), 4, 8);

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.WHITE);
            g.drawRect(camera.screenX(hitbox.x), camera.screenY(hitbox.y), hitbox.width, hitbox.height);
        }
    }
}
