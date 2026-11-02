package skill;

import java.awt.*;
import java.util.ArrayList;

import entity.Player;
import entity.enemy.Enemy;
import entity.enemy.EnemyProjectile;
import main.Camera;
import main.GamePanel;
import sound.Sound;
import world.TileSet;

/**
 * สกิลดาบน้ำแข็ง [E]
 * กดใช้ -> ดาบน้ำแข็งหมุนรอบตัว 6 วินาที
 *   - ดาบแต่ละเล่มโดนศัตรู = ดาเมจ 1 (ศัตรูอมตะ 0.5 วินาทีหลังโดน จึงไม่โดนรัว)
 *   - ดาบปัดกระสุนศัตรูที่บินเข้ามาให้แตกได้
 * Lv1 = 1 เล่ม, Lv2 = 2 เล่ม (สูงสุด) ได้ Lv2 เมื่อเปิดหีบเจอสกิลนี้ซ้ำ
 * หมดเวลา -> คูลดาวน์ 8 วินาที
 */
public class IceSwordSkill extends Skill {

    private static final int STAMINA_COST = 35;
    private static final int COOLDOWN = 480;        // 8 วินาที (นับหลังดาบหายไป)
    private static final int DURATION = 360;        // หมุน 6 วินาที
    private static final int ORBIT_RADIUS = 92;
    private static final int BLADE_SIZE = 48;       // รูปดาบ
    private static final int HIT_SIZE = 40;         // Hitbox ดาบ
    private static final double SPIN_SPEED = 0.1;   // เรเดียนต่อเฟรม (~1 รอบต่อวินาที)
    public static final int DAMAGE = 1;

    private int activeTimer = 0;
    private double angle = 0;

    public IceSwordSkill(GamePanel gamePanel) {
        super(gamePanel, "ICE SWORD", "E", 1, STAMINA_COST, COOLDOWN, 2);
    }

    @Override
    protected void activate(Player player) {
        activeTimer = DURATION;
        gamePanel.getEffects().ring(player.getCenterX(), player.getCenterY(), ORBIT_RADIUS + 20, new Color(120, 220, 255), 18);
        Sound.ICE_SWORD.play();
    }

    @Override
    public boolean isActive() {
        return activeTimer > 0;
    }

    /** จำนวนดาบ = เลเวล (สูงสุด 2) */
    public int getBladeCount() {
        return level;
    }

    @Override
    public void update(Player player) {
        super.update(player);
        if (activeTimer <= 0) {
            return;
        }

        activeTimer--;
        angle += SPIN_SPEED;

        for (int i = 0; i < getBladeCount(); i++) {
            int bx = bladeX(player, i);
            int by = bladeY(player, i);
            Rectangle blade = new Rectangle(bx - HIT_SIZE / 2, by - HIT_SIZE / 2, HIT_SIZE, HIT_SIZE);

            // ฟันศัตรู
            for (Enemy enemy : gamePanel.getEnemies()) {
                if (enemy.isAlive() && !enemy.isInvincible() && blade.intersects(enemy.getHitbox())) {
                    enemy.takeDamage(DAMAGE);
                }
            }

            // ปัดกระสุนศัตรู
            ArrayList<EnemyProjectile> shots = gamePanel.getEnemyProjectiles();
            for (int s = shots.size() - 1; s >= 0; s--) {
                if (blade.intersects(shots.get(s).getHitbox())) {
                    gamePanel.getEffects().burst(shots.get(s).getCenterX(), shots.get(s).getCenterY(),
                            8, new Color(190, 240, 255), 3, 6, 12);
                    shots.remove(s);
                }
            }

            // เกล็ดน้ำแข็งตามหลังดาบ
            if (activeTimer % 2 == 0) {
                gamePanel.getEffects().trail(bx, by, new Color(170, 230, 255), 10);
            }
        }

        if (activeTimer == 0) {
            startCooldown();
            gamePanel.getEffects().sparkle(player.getCenterX(), player.getCenterY(), 10, new Color(170, 230, 255));
        }
    }

    private double bladeAngle(int index) {
        return angle + index * Math.PI * 2 / getBladeCount();
    }

    private int bladeX(Player player, int index) {
        return player.getCenterX() + (int) (Math.cos(bladeAngle(index)) * ORBIT_RADIUS);
    }

    private int bladeY(Player player, int index) {
        return player.getCenterY() + (int) (Math.sin(bladeAngle(index)) * ORBIT_RADIUS);
    }

    @Override
    public void draw(Graphics g, Player player) {
        if (activeTimer <= 0) {
            return;
        }
        Camera camera = gamePanel.getCamera();

        // วงโคจรจาง ๆ (ช่วงท้ายกระพริบเตือนว่าใกล้หมดเวลา)
        boolean ending = activeTimer < 60 && activeTimer % 10 < 5;
        g.setColor(new Color(150, 220, 255, ending ? 20 : 50));
        int cx = camera.screenX(player.getCenterX());
        int cy = camera.screenY(player.getCenterY());
        g.drawOval(cx - ORBIT_RADIUS, cy - ORBIT_RADIUS, ORBIT_RADIUS * 2, ORBIT_RADIUS * 2);

        for (int i = 0; i < getBladeCount(); i++) {
            int sx = camera.screenX(bladeX(player, i));
            int sy = camera.screenY(bladeY(player, i));

            // แสงเรืองรอบดาบ
            g.setColor(new Color(120, 210, 255, ending ? 40 : 90));
            g.fillOval(sx - 26, sy - 26, 52, 52);

            // หมุนรูปดาบให้ปลายชี้ออกจากตัว (Graphics2D อยู่ใน java.awt.* อยู่แล้ว)
            Graphics2D g2 = (Graphics2D) g.create();
            g2.rotate(bladeAngle(i) + Math.PI / 2, sx, sy);
            drawBlade(g2, sx - BLADE_SIZE / 2, sy - BLADE_SIZE / 2, BLADE_SIZE);
            g2.dispose();

            if (gamePanel.isDebugMode()) {
                g.setColor(Color.CYAN);
                g.drawRect(sx - HIT_SIZE / 2, sy - HIT_SIZE / 2, HIT_SIZE, HIT_SIZE);
            }
        }
    }

    /** รูปดาบ (จาก Tileset ย้อมสีฟ้า หรือวาดเองถ้าโหลดรูปไม่ได้) */
    private void drawBlade(Graphics g, int x, int y, int size) {
        if (TileSet.isLoaded()) {
            TileSet.draw(g, TileSet.SWORD, x, y, size);
            g.setColor(new Color(130, 220, 255, 110));
            g.fillRect(x + size * 7 / 16, y + size / 8, size / 8, size / 2);
            return;
        }
        g.setColor(new Color(190, 240, 255));
        g.fillRect(x + size / 2 - 3, y + 2, 6, size * 2 / 3);
        g.setColor(new Color(90, 140, 200));
        g.fillRect(x + size / 2 - 9, y + size * 2 / 3, 18, 4);
        g.fillRect(x + size / 2 - 2, y + size * 2 / 3 + 4, 4, size / 4);
    }

    @Override
    public void drawIcon(Graphics g, int x, int y, int size) {
        drawBlade(g, x, y, size);
        if (level > 1) {
            g.setFont(new Font("Arial", Font.BOLD, Math.max(10, size / 3)));
            g.setColor(Color.BLACK);
            g.drawString("x" + level, x + size - size / 3, y + size - 1);
            g.setColor(new Color(150, 230, 255));
            g.drawString("x" + level, x + size - size / 3 - 1, y + size - 2);
        }
    }

    @Override
    public String getDescription() {
        return "Spinning ice blades, 1 dmg each (max 2)";
    }
}
