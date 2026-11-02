package item;

import java.awt.*;
import java.util.ArrayList;

import entity.DamageText;
import entity.Player;
import main.Camera;
import main.GamePanel;
import skill.IceSwordSkill;
import skill.ShieldSkill;
import skill.Skill;
import sound.Sound;
import world.TileSet;

/**
 * หีบสมบัติในดันเจี้ยน (รูป 64x64 = Hitbox)
 * เดินชนพร้อมมีกุญแจหีบ -> ใช้กุญแจ 1 ดอก เปิดหีบ แล้วของรางวัลเด้งออกมาข้างหน้า
 *
 * ของรางวัล (สุ่ม):
 *   - สกิล 50% (หีบใบแรกที่เปิดได้สกิลแน่นอน) : ICE SHIELD หรือ ICE SWORD
 *     ได้เฉพาะสกิลที่ยังไม่มี / ยังอัปเลเวลได้ (ICE SWORD สูงสุด 2 เล่ม)
 *   - ยา 50% : ยาแดง (HP) 60% / ยาเขียว (Stamina) 40%
 */
public class Chest extends Item {

    public static final int SIZE = 64;

    private boolean opened = false;
    private int hintCooldown = 0;       // กันข้อความ "ต้องมีกุญแจ" ขึ้นรัว ๆ
    private int openAnimation = 0;

    // =========================
    // Constructor
    // =========================
    /** (centerX, centerY) = กลาง Cell ที่วางหีบ */
    public Chest(GamePanel gamePanel, int centerX, int centerY) {
        super(gamePanel, centerX - SIZE / 2, centerY - SIZE / 2, SIZE, SIZE);
    }

    // =========================
    // Update
    // =========================
    @Override
    public void update() {
        if (hintCooldown > 0) {
            hintCooldown--;
        }
        if (openAnimation > 0) {
            openAnimation--;
        }
        if (opened || !isPlayerTouching()) {
            return;
        }

        Player player = gamePanel.getPlayer();
        if (player.isGodMode() || player.useChestKey()) {
            open(player);
        } else if (hintCooldown == 0) {
            hintCooldown = 90;
            gamePanel.addDamageText(new DamageText(gamePanel, "Need a chest key!",
                    new Color(190, 215, 240), x + SIZE / 2, y - 6));
            Sound.LOCKED.play();
        }
    }

    private void open(Player player) {
        opened = true;
        openAnimation = 30;
        gamePanel.onChestOpened();

        int cx = x + SIZE / 2;
        int cy = y + SIZE / 2;
        gamePanel.getEffects().burst(cx, cy, 26, new Color(255, 215, 90), 6, 9, 26);
        gamePanel.getEffects().ring(cx, cy, 110, new Color(255, 230, 140), 22);
        gamePanel.getEffects().sparkle(cx, cy, 16, Color.WHITE);
        Sound.CHEST_OPEN.play();

        // ของรางวัลเด้งออกมาด้านล่างหีบ (ยังอยู่ใน Cell เดียวกัน = พื้นแน่นอน)
        gamePanel.spawnItem(rollReward(player, cx, cy + 40));
        System.out.println("Chest Opened!");
    }

    /** สุ่มของรางวัล (Polymorphism: คืนค่าเป็น Item ไม่ว่าจะเป็นยาหรือม้วนสกิล) */
    private Item rollReward(Player player, int centerX, int centerY) {
        ArrayList<Skill> options = new ArrayList<Skill>();
        Skill shield = new ShieldSkill(gamePanel);
        Skill sword = new IceSwordSkill(gamePanel);
        if (player.canLearnSkill(shield)) {
            options.add(shield);
        }
        if (player.canLearnSkill(sword)) {
            options.add(sword);
        }

        boolean giveSkill = !options.isEmpty() && (!player.hasAnySkill() || Math.random() < 0.5);
        if (giveSkill) {
            Skill skill = options.get((int) (Math.random() * options.size()));
            return new SkillScroll(gamePanel, centerX, centerY, skill);
        }
        if (Math.random() < 0.6) {
            return new HealthPotion(gamePanel, centerX, centerY);
        }
        return new StaminaPotion(gamePanel, centerX, centerY);
    }

    // =========================
    // Draw
    // =========================
    @Override
    public void draw(Graphics g) {
        Camera camera = gamePanel.getCamera();
        int sx = camera.screenX(x);
        int sy = camera.screenY(y);

        // หีบที่ยังไม่เปิด มีแสงทองเรือง ๆ ให้มองเห็นง่าย
        if (!opened) {
            int glow = 6 + (int) (4 * Math.sin(System.nanoTime() / 180000000.0));
            g.setColor(new Color(255, 210, 80, 55));
            g.fillOval(sx - glow, sy + 8 - glow, SIZE + glow * 2, SIZE - 8 + glow * 2);
        }
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(sx + 6, sy + SIZE - 14, SIZE - 12, 12);

        int bounce = openAnimation > 20 ? -(openAnimation - 20) / 2 : 0;
        if (TileSet.isLoaded()) {
            TileSet.draw(g, opened ? TileSet.CHEST_OPEN : TileSet.CHEST_CLOSED, sx, sy + bounce, SIZE);
        } else {
            g.setColor(new Color(150, 90, 40));
            g.fillRect(sx + 6, sy + 18, SIZE - 12, SIZE - 26);
            g.setColor(opened ? new Color(60, 40, 20) : new Color(200, 160, 60));
            g.fillRect(sx + 6, sy + 12, SIZE - 12, 12);
        }

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.WHITE);
            g.drawRect(camera.screenX(hitbox.x), camera.screenY(hitbox.y), hitbox.width, hitbox.height);
        }
    }

    public boolean isOpened() { return opened; }
    public int getCenterX() { return x + SIZE / 2; }
    public int getCenterY() { return y + SIZE / 2; }
}
