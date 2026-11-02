package world;

import java.awt.*;

import entity.Player;
import main.Camera;
import main.GamePanel;
import sound.Sound;

/**
 * ประตูทางออกของด่าน อยู่ในช่องที่เจาะทะลุกำแพงด้านล่าง (กว้าง 2 ช่อง)
 * Hitbox = กรอบรูปประตูที่วาด (128x64)
 * เปิดได้เมื่อ Player เดินเข้ามาชนประตูและมีกุญแจ (หรือเปิด God Mode)
 */
public class Door {

    private final GamePanel gamePanel;
    private final int x;
    private final int y;
    private final int width = Map.TILE_SIZE * 2;    // 128
    private final int height = Map.TILE_SIZE;       // 64
    private final Rectangle hitbox;
    private boolean opened = false;

    // =========================
    // Constructor
    // =========================
    public Door(GamePanel gamePanel, int x, int y) {
        this.gamePanel = gamePanel;
        this.x = x;
        this.y = y;
        hitbox = new Rectangle(x, y, width, height);
    }

    // =========================
    // Update
    // =========================
    public void update() {
        if (opened) {
            return;
        }

        Player player = gamePanel.getPlayer();
        if (hitbox.intersects(player.getHitbox())
                && (player.hasKey() || player.isGodMode())) {
            opened = true;
            gamePanel.getEffects().burst(x + width / 2, y + height / 2, 30, Color.YELLOW, 7, 10, 30);
            gamePanel.getEffects().ring(x + width / 2, y + height / 2, 150, Color.WHITE, 26);
            Sound.DOOR.play();
            System.out.println("Door Opened!");
            gamePanel.completeLevel();
        }
    }

    public int getCenterX() { return x + width / 2; }
    public int getCenterY() { return y + height / 2; }

    // =========================
    // Draw
    // =========================
    public void draw(Graphics g) {
        Camera camera = gamePanel.getCamera();
        int screenX = camera.screenX(x);
        int screenY = camera.screenY(y);

        int half = width / 2;
        if (TileSet.isLoaded()) {
            // ประตูคู่จาก Tileset (ปิด = บานไม้, เปิด = ช่องมืด)
            TileSet.draw(g, opened ? TileSet.DOOR_OPEN_LEFT : TileSet.DOOR_LEFT, screenX, screenY, half);
            TileSet.draw(g, opened ? TileSet.DOOR_OPEN_RIGHT : TileSet.DOOR_RIGHT, screenX + half, screenY, half);
        } else {
            g.setColor(opened ? Color.DARK_GRAY : new Color(150, 30, 30));
            g.fillRect(screenX, screenY, width, height);
            g.setColor(Color.BLACK);
            g.drawLine(screenX + half, screenY, screenX + half, screenY + height);
        }

        // แม่กุญแจตรงกลาง (ยังไม่มีกุญแจ = แดง, มีกุญแจแล้ว = เหลืองกระพริบ)
        if (!opened) {
            boolean hasKey = gamePanel.getPlayer().hasKey();
            boolean blink = (System.nanoTime() / 250000000L) % 2 == 0;
            g.setColor(hasKey ? (blink ? Color.YELLOW : Color.ORANGE) : new Color(200, 40, 40));
            g.fillOval(screenX + half - 9, screenY + height / 2 - 9, 18, 18);
            g.setColor(Color.BLACK);
            g.drawOval(screenX + half - 9, screenY + height / 2 - 9, 18, 18);
        }

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.WHITE);
            g.drawRect(camera.screenX(hitbox.x), camera.screenY(hitbox.y), hitbox.width, hitbox.height);
        }
    }
}
