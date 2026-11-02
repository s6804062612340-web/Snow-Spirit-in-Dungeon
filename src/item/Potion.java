package item;

import java.awt.*;

import entity.Player;
import interfaces.Usable;
import main.Camera;
import main.GamePanel;
import world.TileSet;
import sound.Sound;

/**
 * คลาสแม่ (Abstract) ของยา
 * - เป็น Item ที่ตกอยู่บนพื้น เดินชนแล้วเก็บเข้ากระเป๋า (สูงสุด 5 ขวด)
 * - เป็น Usable: กดเลข 1-5 เพื่อดื่มขวดในช่องนั้น
 *
 * คลาสลูกต้องเขียน use(Player) เอง ว่าดื่มแล้วเกิดอะไรขึ้น (Polymorphism)
 * รูปขวดยา 24x32 = Hitbox
 */
public abstract class Potion extends Item implements Usable {

    public static final int WIDTH = 24;
    public static final int HEIGHT = 32;

    private final String name;
    private final Color liquidColor;

    // =========================
    // Constructor
    // =========================
    /** (centerX, centerY) คือจุดที่ยาตก */
    public Potion(GamePanel gamePanel, int centerX, int centerY, String name, Color liquidColor) {
        super(gamePanel, centerX - WIDTH / 2, centerY - HEIGHT / 2, WIDTH, HEIGHT);
        this.name = name;
        this.liquidColor = liquidColor;
    }

    // =========================
    // Abstract / Overridable
    // =========================
    /** ผลของยา (มาจาก interface Usable) — คลาสลูกเขียนเอง */
    @Override
    public abstract void use(Player player);

    /** ดื่มได้ตอนนี้หรือไม่ (เช่น HP เต็มแล้วไม่ต้องดื่มยาฟื้น HP) */
    public boolean canUse(Player player) {
        return true;
    }

    // =========================
    // Update: เดินชนแล้วเก็บเข้ากระเป๋า
    // =========================
    @Override
    public void update() {
        if (collected) {
            return;
        }

        updateHitbox();

        if (isPlayerTouching() && gamePanel.getPlayer().addPotion(this)) {
            collected = true;
            gamePanel.getEffects().sparkle(x + width / 2, y + height / 2, 10, liquidColor);
            Sound.PICKUP.play();
            System.out.println(name + " Collected!");
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
        drawIcon(g, camera.screenX(x), camera.screenY(y));

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.WHITE);
            g.drawRect(camera.screenX(hitbox.x), camera.screenY(hitbox.y), hitbox.width, hitbox.height);
        }
    }

    /** เลขภาพใน Tileset ของยาชนิดนี้ (-1 = วาดด้วยรูปทรงแทน) คลาสลูก Override ได้ */
    protected int getTileIndex() {
        return -1;
    }

    /** วาดขวดยาขนาด 24x32 ที่ตำแหน่งบนจอ (ใช้ทั้งบนพื้นและในช่องกระเป๋า) */
    public void drawIcon(Graphics g, int screenX, int screenY) {
        if (getTileIndex() >= 0 && TileSet.isLoaded()) {
            // ภาพจาก Tileset 16x16 ขยายเป็น 32x32 วางกลางกรอบ 24x32
            TileSet.draw(g, getTileIndex(), screenX - 4, screenY, 32);
            return;
        }
        // จุกขวด
        g.setColor(new Color(140, 90, 50));
        g.fillRect(screenX + 8, screenY, 8, 5);

        // คอขวด
        g.setColor(new Color(220, 235, 245));
        g.fillRect(screenX + 7, screenY + 5, 10, 7);

        // ตัวขวด + น้ำยา
        g.fillOval(screenX + 1, screenY + 9, 22, 22);
        g.setColor(liquidColor);
        g.fillOval(screenX + 3, screenY + 13, 18, 17);

        // ขอบ + แสงสะท้อน
        g.setColor(new Color(40, 40, 60));
        g.drawOval(screenX + 1, screenY + 9, 22, 22);
        g.drawRect(screenX + 7, screenY + 5, 10, 6);
        g.setColor(new Color(255, 255, 255, 200));
        g.fillOval(screenX + 6, screenY + 15, 5, 7);
    }

    // =========================
    // Getters
    // =========================
    public String getName() { return name; }
    public Color getLiquidColor() { return liquidColor; }
}
