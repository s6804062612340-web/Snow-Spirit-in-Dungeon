package entity.enemy;

import java.awt.*;
import java.util.ArrayList;

import main.Camera;
import main.GamePanel;
import sound.Sound;
import world.Map;

/**
 * Boss: ตัวใหญ่ 112x112
 *
 * สกิล "วาร์ป + ทุบ":
 *   1. เมื่อ Player เข้ามาในระยะ TELEPORT_RANGE และสกิลไม่ติดคูลดาวน์
 *   2. Boss ค่อย ๆ จางหาย (0.4 วินาที) พร้อมวงสีม่วงบอกจุดที่จะโผล่ข้าง ๆ Player
 *   3. โผล่ที่จุดนั้น แล้วชาร์จทุบพื้นทันที (วงแดง 0.6 วินาที) -> ทุบ ดาเมจ 2
 *   ผู้เล่นมีเวลาเกือบ 1 วินาทีให้เดิน / Dash หนีออกจากวงแดง
 *
 * ถ้าสกิลติดคูลดาวน์ -> เดินเข้าหา Player เหมือนเดิม และถ้าเดินถึงตัวก็ทุบได้ตามปกติ
 *
 * รูป assets/Boss_Spirit.png = Sprite Sheet 4x4 เฟรมละ 128x128
 *   แถว 0 = ลง, 1 = ขึ้น, 2 = ขวา, 3 = ซ้าย (เหมือน Snow Spirit)
 */
public class Boss extends Enemy {

    public static final int SIZE = 112;

    private static final int ATTACK_RANGE = 140;     // เดินมาถึงระยะนี้ = ทุบปกติ
    private static final int SLAM_RADIUS = 170;      // รัศมีที่โดนทุบ
    private static final int WINDUP_FRAMES = 36;     // 0.6 วินาที
    private static final int ATTACK_DELAY = 70;
    private static final int TELEPORT_RANGE = 520;   // Player เข้ามาในระยะนี้ = วาร์ปได้
    private static final int VANISH_FRAMES = 24;     // 0.4 วินาที

    private int attackCooldown = 30;
    private int windupTimer = 0;
    private int teleportDelay = 360;                 // คูลดาวน์สกิลวาร์ป (ตั้งตามระดับความยาก)
    private int teleportCooldown = 120;              // เริ่มด่านมาให้เวลา 2 วินาทีก่อน
    private int vanishTimer = 0;
    private Point destination = null;                // Cell ที่จะไปโผล่

    // =========================
    // Constructor
    // =========================
    public Boss(GamePanel gamePanel, int hp, int speed) {
        super(gamePanel, hp, speed, SIZE);
        spriteWidth = 128;                  // 1 เฟรมในไฟล์ Boss_Spirit.png
        spriteHeight = 128;
        loadSprite("assets/Boss_Spirit.png");
    }

    /** คูลดาวน์สกิลวาร์ป (เฟรม) ปรับตามระดับความยาก */
    public void setTeleportDelay(int frames) {
        teleportDelay = frames;
    }

    // =========================
    // Update
    // =========================
    @Override
    public void update() {
        if (!alive) {
            return;
        }

        int oldX = x;
        int oldY = y;
        int px = gamePanel.getPlayer().getCenterX();
        int py = gamePanel.getPlayer().getCenterY();

        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (vanishTimer > 0) {
            updateVanish();
        } else if (windupTimer > 0) {
            // กำลังชาร์จ: ยืนนิ่ง หันหน้าหา Player
            windupTimer--;
            faceToward(px, py);
            if (windupTimer == 0) {
                slam();
            }
        } else {
            double distance = distanceToPlayer();

            if (teleportCooldown == 0 && distance <= TELEPORT_RANGE && distance > ATTACK_RANGE) {
                startTeleport();
            } else {
                // สกิลติดคูลดาวน์ -> เดินเข้าหาตามปกติ
                if (distance > ATTACK_RANGE) {
                    chasePlayer();
                    checkStuck();
                }
                if (distance <= ATTACK_RANGE && attackCooldown == 0) {
                    startWindup();
                }
            }
        }

        updateMovementAnimation(oldX, oldY);
        updateAnimation();
        updateInvincibility();
    }

    // =========================
    // Skill: Teleport
    // =========================
    private void startTeleport() {
        destination = chooseDestination();
        if (destination == null) {
            teleportCooldown = 60;      // ไม่มีที่ว่างข้าง Player -> ลองใหม่อีก 1 วินาที
            return;
        }
        vanishTimer = VANISH_FRAMES;
        teleportCooldown = teleportDelay;
        resetStuckTimer();
        gamePanel.getEffects().burst(getCenterX(), getCenterY(), 20, new Color(180, 90, 255), 5, 10, 24);
        Sound.BOSS_TELEPORT.play();
    }

    private void updateVanish() {
        vanishTimer--;
        if (vanishTimer % 3 == 0) {
            gamePanel.getEffects().sparkle(getCenterX(), getCenterY(), 2, new Color(180, 90, 255));
        }
        if (vanishTimer == 0) {
            placeAtCell(destination.x, destination.y);
            gamePanel.getEffects().ring(getCenterX(), getCenterY(), 120, new Color(180, 90, 255), 18);
            gamePanel.getEffects().burst(getCenterX(), getCenterY(), 24, new Color(200, 140, 255), 6, 10, 22);
            gamePanel.getEffects().shake(5, 10);
            destination = null;
            startWindup();      // โผล่แล้วทุบทันที
        }
    }

    /** หา Cell ว่างรอบ ๆ Player (ห่าง 1-2 Cell) ที่ไม่ทับ Player */
    private Point chooseDestination() {
        Map map = gamePanel.getMap();
        int playerCol = Map.toCell(gamePanel.getPlayer().getCenterX());
        int playerRow = Map.toCell(gamePanel.getPlayer().getCenterY());
        ArrayList<Point> options = new ArrayList<Point>();

        for (int r = playerRow - 2; r <= playerRow + 2; r++) {
            for (int c = playerCol - 2; c <= playerCol + 2; c++) {
                int steps = Math.abs(r - playerRow) + Math.abs(c - playerCol);
                if (steps < 1 || steps > 2 || map.isCellWall(c, r)) {
                    continue;
                }
                double distance = Math.hypot(Map.cellCenter(c) - gamePanel.getPlayer().getCenterX(),
                        Map.cellCenter(r) - gamePanel.getPlayer().getCenterY());
                if (distance >= 115) {      // ไม่โผล่ทับตัว Player
                    options.add(new Point(c, r));
                }
            }
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get((int) (Math.random() * options.size()));
    }

    // =========================
    // Attack: Slam
    // =========================
    private void startWindup() {
        windupTimer = WINDUP_FRAMES;
        resetStuckTimer();
        Sound.BOSS_CHARGE.play();
    }

    /** ทุบพื้น: โดน Player ถ้ายังอยู่ในวงแดง */
    private void slam() {
        attackCooldown = ATTACK_DELAY;

        gamePanel.getEffects().ring(getCenterX(), getCenterY(), SLAM_RADIUS, new Color(255, 80, 60), 24);
        gamePanel.getEffects().ring(getCenterX(), getCenterY(), SLAM_RADIUS / 2, Color.WHITE, 16);
        gamePanel.getEffects().debris(getCenterX(), getCenterY() + height / 3, 18, new Color(120, 110, 100));
        gamePanel.getEffects().shake(10, 18);
        Sound.BOSS_SLAM.play();

        if (distanceToPlayer() <= SLAM_RADIUS) {
            gamePanel.getPlayer().takeDamage(2);
            System.out.println("Boss slam hit Player!");
        }
    }

    private double distanceToPlayer() {
        int dx = gamePanel.getPlayer().getCenterX() - getCenterX();
        int dy = gamePanel.getPlayer().getCenterY() - getCenterY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** ระหว่างหายตัว โจมตีไม่โดน */
    @Override
    public void takeDamage(int damage) {
        if (vanishTimer > 0) {
            return;
        }
        super.takeDamage(damage);
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        gamePanel.getEffects().burst(getCenterX(), getCenterY(), 40, new Color(110, 210, 190), 9, 14, 40);
        gamePanel.getEffects().ring(getCenterX(), getCenterY(), 220, new Color(110, 210, 190), 30);
        gamePanel.getEffects().shake(14, 25);
    }

    @Override
    protected Color getDeathColor() {
        return new Color(110, 210, 190);
    }

    // =========================
    // Draw
    // =========================
    @Override
    public void draw(Graphics g) {
        if (!alive) {
            return;
        }

        drawTeleportMarker(g);
        drawSlamWarning(g);

        if (vanishTimer > 0) {
            // จางหาย: ลดความทึบของรูปด้วย Graphics2D (อยู่ใน java.awt.* อยู่แล้ว)
            Graphics2D g2 = (Graphics2D) g;
            Composite old = g2.getComposite();
            float alpha = Math.max(0.05f, (float) vanishTimer / VANISH_FRAMES);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            drawBody(g, Color.MAGENTA);
            g2.setComposite(old);
        } else {
            drawBody(g, Color.MAGENTA);     // Sprite Sheet + กระพริบตอนอมตะ
        }
        drawHPBar(g);
        drawChestKeyMark(g);
        drawHitbox(g);
    }

    /** วงสีม่วงตรงจุดที่ Boss จะโผล่ */
    private void drawTeleportMarker(Graphics g) {
        if (vanishTimer <= 0 || destination == null) {
            return;
        }
        Camera camera = gamePanel.getCamera();
        double t = 1 - (double) vanishTimer / VANISH_FRAMES;       // 0 -> 1
        int cx = camera.screenX(Map.cellCenter(destination.x));
        int cy = camera.screenY(Map.cellCenter(destination.y));
        int radius = (int) (SIZE / 2 + 20 * (1 - t));

        g.setColor(new Color(180, 90, 255, (int) (60 + 100 * t)));
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.setColor(new Color(220, 170, 255));
        g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.drawOval(cx - radius + 6, cy - radius + 6, radius * 2 - 12, radius * 2 - 12);
    }

    /** วงแดงบนพื้นระหว่างชาร์จ: สีเข้มขึ้นเรื่อย ๆ จนทุบ */
    private void drawSlamWarning(Graphics g) {
        if (windupTimer <= 0) {
            return;
        }
        Camera camera = gamePanel.getCamera();
        double t = 1 - (double) windupTimer / WINDUP_FRAMES;       // 0 -> 1
        drawDangerCircle(g, camera.screenX(getCenterX()), camera.screenY(getCenterY()), SLAM_RADIUS, t);
    }

    public boolean isWindingUp() {
        return windupTimer > 0;
    }

    public boolean isTeleporting() {
        return vanishTimer > 0;
    }
}
