package item;

import java.awt.*;

import main.GamePanel;

/**
 * คลาสแม่ (Abstract) ของไอเทมที่วางอยู่บนพื้น
 */
public abstract class Item {

    protected final GamePanel gamePanel;
    protected int x;
    protected int y;
    protected final int width;      // ขนาดรูป = ขนาด Hitbox
    protected final int height;
    protected boolean collected = false;
    protected Rectangle hitbox;

    // =========================
    // Constructor
    // =========================
    public Item(GamePanel gamePanel, int x, int y, int width, int height) {
        this.gamePanel = gamePanel;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        hitbox = new Rectangle(x, y, width, height);
    }

    // =========================
    // Abstract Methods
    // =========================
    public abstract void update();

    public abstract void draw(Graphics g);

    // =========================
    // Helpers
    // =========================
    protected void updateHitbox() {
        hitbox.x = x;
        hitbox.y = y;
    }

    protected boolean isPlayerTouching() {
        return hitbox.intersects(gamePanel.getPlayer().getHitbox());
    }
}
