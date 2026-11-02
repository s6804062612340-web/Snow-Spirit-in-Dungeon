package entity.enemy;

import java.awt.*;
import javax.swing.*;

import ai.FlowField;
import entity.DamageText;
import entity.Player;
import interfaces.Damageable;
import item.ChestKey;
import main.Camera;
import main.GamePanel;
import sound.Sound;
import world.Difficulty;
import world.Map;

/**
 * คลาสแม่ (Abstract) ของศัตรูทุกชนิด
 * เก็บของที่ศัตรูทุกตัวมีเหมือนกัน: HP, ความเร็ว, Hitbox, การเดิน, การยิง, Animation, HP Bar
 * ส่วน update() / draw() ให้คลาสลูก (Slime, Skeleton, Boss) เขียนเอง
 *
 * การเดิน (Navigation):
 *   - ถ้ามองเห็น Player ตรง ๆ (ลากทั้งตัวไปแล้วไม่ชนกำแพง) -> เดินตรงเข้าหาเลย
 *   - ถ้ามีกำแพงขวาง -> เดินตาม Flow Field ทีละ Cell (128px) ซึ่งกว้างกว่าตัวศัตรูทุกตัว
 *     ระหว่างเดินจะค่อย ๆ ขยับเข้ากลางทางเดิน จึงไม่เฉี่ยวมุมกำแพง
 *   - กันติด: ถ้าตั้งใจเดินแต่ไม่ขยับเกิน 0.75 วินาที จะดึงกลับไปกลาง Cell
 *   - ศัตรูดันกันเองไม่ให้ซ้อนเป็นก้อนเดียว
 *
 * การโจมตี (ทุกแบบมีสัญญาณเตือนสีแดง + คูลดาวน์ เหมือน Boss):
 *   - ตีใกล้ (MELEE): เดินเข้าใกล้ -> หยุดยืน + วงแดงบนพื้นค่อย ๆ เต็ม -> ฟาด (โดนเมื่อยังอยู่ในวง) -> พักคูลดาวน์
 *   - ตีไกล (RANGED): ล็อกเป้า + เส้นเล็งสีแดง -> ยิงไปตามเส้นนั้น -> พักคูลดาวน์
 */
public abstract class Enemy implements Damageable {

    protected final GamePanel gamePanel;

    // =========================
    // Position / Stats
    // =========================
    protected int x;                    // ตำแหน่งที่ใช้วาด / Hitbox (ปัดจาก posX, posY)
    protected int y;
    protected double posX;              // ตำแหน่งละเอียด (ทศนิยม) ทำให้เดินเฉียงได้นุ่ม
    protected double posY;
    protected final int width;          // Hitbox = ขนาดรูปที่วาด
    protected final int height;
    protected int speed;
    protected int hp;
    protected int maxHp;
    protected boolean alive = true;
    protected Rectangle hitbox;

    // =========================
    // Damage / Invincibility (โดนตีแล้วอมตะ 0.5 วินาที = 30 เฟรม แล้วกระพริบเหมือน Player)
    // =========================
    protected static final int INVINCIBLE_FRAMES = 30;
    protected boolean invincible = false;
    protected int invincibleCounter = 0;

    // =========================
    // Navigation (Flow Field) + กันติด
    // =========================
    private static final int STUCK_CHECK_FRAMES = 45;
    private int stuckTimer = 0;
    private double stuckAnchorX;
    private double stuckAnchorY;
    private boolean wantedToMove = false;

    // =========================
    // Melee Attack (ใช้เมื่อ style = MELEE): วงแดงเตือน -> ฟาด -> คูลดาวน์
    // =========================
    protected int meleeRadius = 80;                    // รัศมีวงแดง (นับจากกลางตัว)
    protected int meleeWindupFrames = 40;              // เวลาเตือนก่อนฟาด
    protected int meleeDelay = 80;                     // คูลดาวน์หลังฟาด (เฟรม)
    protected int meleeDamage = 1;
    private int meleeTimer = 0;                        // > 0 = กำลังเตือน (ยืนนิ่ง)
    private int meleeCooldown = 30 + (int) (Math.random() * 30);

    // =========================
    // Ranged Attack (ใช้เมื่อ style = RANGED)
    // =========================
    protected CombatStyle style = CombatStyle.MELEE;
    protected static final int CHARGE_FRAMES = 30;     // ล็อกเป้า + เส้นเล็งสีแดง 0.5 วินาที
    protected int keepDistanceMin = 220;               // ใกล้กว่านี้ = ถอยหนี
    protected int keepDistanceMax = 340;               // ไกลกว่านี้ = เดินเข้าหา
    protected int shootDelay = 120;                    // เฟรมระหว่างการยิงแต่ละครั้ง
    protected double projectileSpeed = 4;
    protected int projectileSize = 16;
    protected Color projectileColor = Color.GREEN;
    private int shootTimer = 40 + (int) (Math.random() * 80);
    private Point firingCell = null;                    // Cell ที่เลือกไว้ยืนยิง
    private int thinkTimer = 0;
    private int chargeTimer = 0;
    private int aimX;                                   // จุดที่ล็อกเป้าไว้ตอนเริ่มเล็ง
    private int aimY;

    // ศัตรูตัวนี้ถือกุญแจหีบสมบัติ (ตายแล้วดรอปกุญแจหีบ)
    private boolean carriesChestKey = false;

    // =========================
    // Animation
    // =========================
    protected Image sprite;              // null = โหลดรูปไม่ได้
    protected int spriteWidth = 64;      // ขนาด 1 เฟรมในไฟล์รูป
    protected int spriteHeight = 64;
    protected int spriteRow = 0;
    protected int spriteColumn = 0;
    protected boolean moving = false;
    protected int animationCounter = 0;
    protected String direction = "down";

    // =========================
    // Constructor
    // =========================
    /**
     * @param size ขนาดรูปที่วาด (px) ซึ่งเท่ากับขนาด Hitbox
     */
    public Enemy(GamePanel gamePanel, int hp, int speed, int size) {
        this.gamePanel = gamePanel;
        this.hp = hp;
        this.maxHp = hp;
        this.speed = speed;
        this.width = size;
        this.height = size;
        hitbox = new Rectangle(0, 0, width, height);
    }

    /** ปรับค่าตามระดับความยาก: HP, ความเร็ว, ความเร็วกระสุน, ความถี่การยิง */
    public void applyDifficulty(Difficulty difficulty) {
        hp = Math.max(1, (int) Math.round(hp * difficulty.getEnemyHpMultiplier()));
        maxHp = hp;
        speed += difficulty.getEnemySpeedBonus();
        projectileSpeed *= difficulty.getShotSpeedMultiplier();
        shootDelay = (int) Math.round(shootDelay * difficulty.getShootDelayMultiplier());
        meleeDelay = (int) Math.round(meleeDelay * difficulty.getShootDelayMultiplier());
    }

    /** เพิ่มความเร็ว (ด่านหลัง ๆ ศัตรูเร็วขึ้น) */
    public void addSpeed(int bonus) {
        speed += bonus;
    }

    /** วางศัตรูให้อยู่กลาง Cell (cellCol, cellRow) */
    public void placeAtCell(int cellCol, int cellRow) {
        posX = Map.cellCenter(cellCol) - width / 2.0;
        posY = Map.cellCenter(cellRow) - height / 2.0;
        stuckTimer = 0;
        stuckAnchorX = posX;
        stuckAnchorY = posY;
        syncPosition();
    }

    /** วางศัตรูให้จุดกลางตัวอยู่ที่ (centerX, centerY) */
    public void placeAtCenter(int centerX, int centerY) {
        posX = centerX - width / 2.0;
        posY = centerY - height / 2.0;
        stuckAnchorX = posX;
        stuckAnchorY = posY;
        syncPosition();
    }

    // =========================
    // Abstract Methods (คลาสลูกต้อง Override)
    // =========================
    public abstract void update();

    public abstract void draw(Graphics g);

    // =========================
    // Behaviors (คลาสลูกเรียกใช้ใน update)
    // =========================
    /** เดินเข้าหา Player (Boss ใช้ตอนสกิลติดคูลดาวน์) */
    protected void chasePlayer() {
        Player player = gamePanel.getPlayer();
        navigateTo(player.getCenterX(), player.getCenterY());
    }

    /**
     * ตีใกล้: เดินเข้าหา -> พอ Player อยู่ในวงโจมตี (และคูลดาวน์หมด) หยุดยืนแล้วแสดงวงแดง
     * วงด้านในขยายจนเต็มวง = ฟาด ถ้า Player ยังอยู่ในวงจะโดนดาเมจ แล้วพักคูลดาวน์
     * ระหว่างคูลดาวน์จะเดินตามแต่ไม่เบียดทับตัว Player
     */
    protected void chaseAndAttack() {
        Player player = gamePanel.getPlayer();
        int px = player.getCenterX();
        int py = player.getCenterY();

        if (meleeCooldown > 0) {
            meleeCooldown--;
        }

        // กำลังเตือน -> ยืนนิ่ง หันหน้าหา Player
        if (meleeTimer > 0) {
            meleeTimer--;
            faceToward(px, py);
            holdPosition();
            if (meleeTimer == 0) {
                meleeStrike();
            }
            return;
        }

        double reach = distanceToPlayerBox();
        if (meleeCooldown == 0 && reach <= meleeRadius - 24
                && hasLineOfSight(getCenterX(), getCenterY(), px, py, 16)) {
            meleeTimer = meleeWindupFrames;
            resetStuckTimer();
            Sound.ENEMY_WINDUP.play();
            return;
        }

        // ใกล้พอแล้ว ไม่ต้องเดินทับตัว Player (ยืนรอคูลดาวน์)
        double centerDistance = Math.hypot(px - getCenterX(), py - getCenterY());
        if (centerDistance < (width + Player.SIZE) / 2.0 + 8) {
            faceToward(px, py);
            holdPosition();
        } else {
            navigateTo(px, py);
        }
    }

    /** ฟาด: วงกระแทก + โดน Player ถ้ายังอยู่ในวงแดง */
    private void meleeStrike() {
        meleeCooldown = meleeDelay;
        gamePanel.getEffects().ring(getCenterX(), getCenterY(), meleeRadius, new Color(255, 80, 60), 16);
        gamePanel.getEffects().burst(getCenterX(), getCenterY(), 10, getDeathColor(), 4, 7, 14);
        Sound.ENEMY_STRIKE.play();
        if (distanceToPlayerBox() <= meleeRadius) {
            gamePanel.getPlayer().takeDamage(meleeDamage);
        }
    }

    /** ระยะจากกลางตัวศัตรูถึงขอบ Hitbox ของ Player ที่ใกล้ที่สุด (0 = ทับกัน) */
    protected double distanceToPlayerBox() {
        Rectangle box = gamePanel.getPlayer().getHitbox();
        int nearestX = Math.max(box.x, Math.min(getCenterX(), box.x + box.width));
        int nearestY = Math.max(box.y, Math.min(getCenterY(), box.y + box.height));
        return Math.hypot(nearestX - getCenterX(), nearestY - getCenterY());
    }

    /**
     * ตีไกล: เลือกยืนในจุดที่ "มองเห็น Player และห่างพอดี"
     *   - อยู่ไกลมาก -> เดินเข้าหาด้วย Flow Field
     *   - อยู่ใกล้แล้ว -> ทุก 1/3 วินาที ให้คะแนน Cell ที่ยืนอยู่ + Cell ข้าง ๆ 4 ทิศ
     *       มองเห็น Player (ยิงโดน) = คะแนนสูง, ระยะใกล้ค่ากลางของ keepDistanceMin..Max = คะแนนสูง
     *       ใกล้เกิน keepDistanceMin = หักคะแนนมาก (จึงถอยหนีเอง)
     *     แล้วเดินไป Cell ที่ได้คะแนนสูงสุด ถึงแล้วก็ยืนยิง
     *   ไม่เดินกลับไปกลับมาที่มุมกำแพง เพราะเลือกจุดจาก "ยิงโดนหรือไม่" ไม่ใช่แค่ระยะ
     */
    protected void keepDistanceAndShoot() {
        Player player = gamePanel.getPlayer();
        int px = player.getCenterX();
        int py = player.getCenterY();
        double distance = Math.hypot(px - getCenterX(), py - getCenterY());
        boolean canSee = hasLineOfSight(getCenterX(), getCenterY(), px, py, projectileSize);

        // กำลังเรืองแสงเตรียมยิง -> ยืนนิ่ง หันหน้าหา Player
        if (chargeTimer > 0) {
            chargeTimer--;
            faceToward(aimX, aimY);
            if (chargeTimer == 0) {
                fireAt(aimX, aimY);         // ยิงไปตามเส้นเล็งที่ล็อกไว้ (เดินหลบได้)
                resetStuckTimer();          // ยืนเล็งโดยตั้งใจ ไม่นับว่าติด
            }
            wantedToMove = false;
            return;
        }

        int cellCol = Map.toCell(getCenterX());
        int cellRow = Map.toCell(getCenterY());
        int stepsToPlayer = gamePanel.getFlowField().getDistance(cellCol, cellRow);

        // กันสั่น: เข้าโหมดเลือกจุดยิงเมื่อห่าง <= 5 Cell แต่จะกลับไปโหมดเดินเข้าหาเมื่อห่าง > 7 Cell
        int farSteps = (firingCell == null) ? 5 : 7;
        if (stepsToPlayer > farSteps || distance > keepDistanceMax + 260) {
            firingCell = null;
            navigateTo(px, py);             // ไกลมาก -> เดินเข้าหาก่อน
        } else {
            thinkTimer--;
            if (thinkTimer <= 0 || firingCell == null) {
                firingCell = chooseFiringCell(cellCol, cellRow, px, py);
                thinkTimer = 20;
            }
            if (firingCell.x == cellCol && firingCell.y == cellRow) {
                holdPosition();
            } else {
                moveToNeighborCell(firingCell, cellCol, cellRow);
            }
        }

        // ยิงเมื่อคูลดาวน์หมดและมองเห็น Player
        shootTimer--;
        if (shootTimer <= 0 && canSee && distance < keepDistanceMax + 80) {
            chargeTimer = CHARGE_FRAMES;
            shootTimer = shootDelay;
            aimX = px;
            aimY = py;
            Sound.ENEMY_AIM.play();
            resetStuckTimer();
        }
    }

    /** ให้คะแนน Cell ปัจจุบัน + 4 ทิศ แล้วคืน Cell ที่ดีที่สุดสำหรับยืนยิง */
    private Point chooseFiringCell(int cellCol, int cellRow, int px, int py) {
        int[][] options = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
        double preferred = (keepDistanceMin + keepDistanceMax) / 2.0;
        Point best = new Point(cellCol, cellRow);
        double bestScore = -Double.MAX_VALUE;
        boolean anyLineOfSight = false;

        for (int[] o : options) {
            int c = cellCol + o[0];
            int r = cellRow + o[1];
            if (gamePanel.getMap().isCellWall(c, r)) {
                continue;
            }
            int cx = Map.cellCenter(c);
            int cy = Map.cellCenter(r);
            double d = Math.hypot(px - cx, py - cy);

            double score = -Math.abs(d - preferred);
            if (hasLineOfSight(cx, cy, px, py, projectileSize)) {
                score += 1000;          // ยิงโดนได้ = สำคัญที่สุด
                anyLineOfSight = true;
            }
            if (d < keepDistanceMin - 20) {
                score -= 600;           // ใกล้เกินไป = อันตราย
            }
            if (o[0] == 0 && o[1] == 0) {
                score += 25;            // ชอบอยู่ที่เดิม (ไม่ขยับเปล่า ๆ)
            }
            if (score > bestScore) {
                bestScore = score;
                best = new Point(c, r);
            }
        }

        // ไม่มีจุดไหนยิงโดนเลย (Player อยู่หลังมุมกำแพง) -> เดินเข้าหาอีก 1 Cell เพื่อหามุมยิง
        if (!anyLineOfSight) {
            Point closer = gamePanel.getFlowField().nextStepToward(cellCol, cellRow);
            if (closer != null) {
                return closer;
            }
        }
        return best;
    }

    private void fireAt(int targetX, int targetY) {
        gamePanel.addEnemyProjectile(new EnemyProjectile(gamePanel, getCenterX(), getCenterY(),
                targetX, targetY, projectileSpeed, projectileSize, 1, projectileColor));
        gamePanel.getEffects().burst(getCenterX(), getCenterY(), 6, projectileColor, 3, 6, 12);
        Sound.ENEMY_SHOOT.play();
    }

    // =========================
    // Navigation
    // =========================
    /** เดินไปยังจุด (targetX, targetY) ในโลก (ใช้กับเป้าหมายที่เป็น Player) */
    protected void navigateTo(int targetX, int targetY) {
        int cx = getCenterX();
        int cy = getCenterY();
        double distance = Math.hypot(targetX - cx, targetY - cy);

        // มองเห็นตรง ๆ และไม่ไกลมาก -> เดินตรงไปเลย
        if (distance < 700 && hasLineOfSight(cx, cy, targetX, targetY, width)) {
            steer(targetX - cx, targetY - cy, speed);
            return;
        }
        if (!followFlow(true)) {
            steer(targetX - cx, targetY - cy, speed);      // อยู่ Cell เดียวกับ Player แล้ว
        }
    }

    /**
     * เดินตาม Flow Field ไป Cell ข้าง ๆ
     * @param toward true = เข้าหา Player, false = หนีจาก Player
     * @return false ถ้าไม่มี Cell ให้ไป (ถึงแล้ว / ทางตัน)
     */
    protected boolean followFlow(boolean toward) {
        FlowField field = gamePanel.getFlowField();
        int cellCol = Map.toCell(getCenterX());
        int cellRow = Map.toCell(getCenterY());
        Point next = toward ? field.nextStepToward(cellCol, cellRow) : field.nextStepAway(cellCol, cellRow);
        if (next == null) {
            return false;
        }

        moveToNeighborCell(next, cellCol, cellRow);
        return true;
    }

    /** เดินจาก Cell ปัจจุบันไป Cell ข้าง ๆ (next) โดยอยู่ในเส้นกลางทางเดิน ไม่เฉี่ยวมุม */
    protected void moveToNeighborCell(Point next, int cellCol, int cellRow) {
        // ระยะที่เยื้องจากเส้นกลางทางเดินได้โดยไม่ชนกำแพง (Slime 64px = 30, Boss 112px = 6)
        int slack = Math.max(1, (Map.CELL_SIZE - Math.max(width, height)) / 2 - 2);
        boolean horizontal = next.x != cellCol;
        int laneOffset = horizontal
                ? getCenterY() - Map.cellCenter(cellRow)
                : getCenterX() - Map.cellCenter(cellCol);

        if (Math.abs(laneOffset) > slack) {
            // เยื้องเกิน -> ขยับเข้าเส้นกลางทางเดินก่อน (ด้านข้างอย่างเดียว) แล้วค่อยเดินหน้า
            double fix = Math.min(speed, Math.abs(laneOffset) - slack + 1) * -Math.signum(laneOffset);
            if (horizontal) {
                steer(0, fix, Math.abs(fix));
            } else {
                steer(fix, 0, Math.abs(fix));
            }
            return;
        }

        // อยู่ในเส้นทางแล้ว -> เดินไปกลาง Cell ถัดไป (ค่อย ๆ ดึงเข้ากลางเส้นไปด้วย)
        int targetX = horizontal ? Map.cellCenter(next.x) : Map.cellCenter(cellCol);
        int targetY = horizontal ? Map.cellCenter(cellRow) : Map.cellCenter(next.y);
        steer(targetX - getCenterX(), targetY - getCenterY(), speed);
    }

    /**
     * ยืนอยู่กับที่ (ตีไกลยืนยิง) มีแค่แรงผลักจากศัตรูตัวอื่น
     * ถ้าโดนผลักจนเยื้องจากกลาง Cell มาก ค่อยขยับกลับ
     */
    protected void holdPosition() {
        int targetX = Map.cellCenter(Map.toCell(getCenterX()));
        int targetY = Map.cellCenter(Map.toCell(getCenterY()));
        double offset = Math.hypot(targetX - getCenterX(), targetY - getCenterY());
        if (offset > 40) {
            steer(targetX - getCenterX(), targetY - getCenterY(), Math.min(speed, offset - 40));
        } else {
            steer(0, 0, speed);
        }
        wantedToMove = false;       // ตั้งใจยืน ไม่นับว่าติด
    }

    /**
     * ขยับไปทิศ (dirX, dirY) ด้วยความเร็ว moveSpeed
     * + แรงผลักจากศัตรูตัวอื่นที่อยู่ใกล้ + ไถลตามกำแพง
     */
    protected void steer(double dirX, double dirY, double moveSpeed) {
        double length = Math.sqrt(dirX * dirX + dirY * dirY);
        double moveX = 0;
        double moveY = 0;
        if (length > 0.001) {
            moveX = dirX / length * moveSpeed;
            moveY = dirY / length * moveSpeed;
        }
        wantedToMove = length > 0.001;

        // แรงผลักออกจากศัตรูตัวอื่น (ไม่ให้ซ้อนกัน) - ไม่เกินครึ่งความเร็ว
        double pushX = 0;
        double pushY = 0;
        for (Enemy other : gamePanel.getEnemies()) {
            if (other == this || !other.alive) {
                continue;
            }
            double ox = getCenterX() - other.getCenterX();
            double oy = getCenterY() - other.getCenterY();
            double d = Math.sqrt(ox * ox + oy * oy);
            double minDistance = (width + other.width) / 2.0 * 0.8;
            if (d > 0.01 && d < minDistance) {
                double push = (minDistance - d) / minDistance;
                pushX += ox / d * push;
                pushY += oy / d * push;
            }
        }
        double pushLength = Math.sqrt(pushX * pushX + pushY * pushY);
        if (pushLength > 0.001) {
            double limit = Math.max(0.5, moveSpeed * 0.5);
            moveX += pushX / pushLength * Math.min(limit, pushLength * moveSpeed);
            moveY += pushY / pushLength * Math.min(limit, pushLength * moveSpeed);
        }

        moveWithCollision(moveX, moveY);
    }

    /** ขยับทีละแกน ถ้าแกนไหนชนกำแพงจะหยุดเฉพาะแกนนั้น (ไถลตามกำแพงได้) */
    protected void moveWithCollision(double moveX, double moveY) {
        Map map = gamePanel.getMap();
        if (!map.isWallCollision((int) Math.round(posX + moveX), (int) Math.round(posY), width, height)) {
            posX += moveX;
        }
        if (!map.isWallCollision((int) Math.round(posX), (int) Math.round(posY + moveY), width, height)) {
            posY += moveY;
        }
        syncPosition();
    }

    /**
     * กันติดถาวร: ทุก 45 เฟรม ถ้าตั้งใจเดินแต่ขยับได้ไม่ถึง 6px
     * ให้ดึงกลับไปกลาง Cell ที่ยืนอยู่ (ซึ่งเป็นพื้นแน่นอน) แล้วเดินต่อจากตรงนั้น
     */
    protected void checkStuck() {
        stuckTimer++;
        if (stuckTimer < STUCK_CHECK_FRAMES) {
            return;
        }
        stuckTimer = 0;

        double moved = Math.hypot(posX - stuckAnchorX, posY - stuckAnchorY);
        int cellCol = Map.toCell(getCenterX());
        int cellRow = Map.toCell(getCenterY());
        if (wantedToMove && moved < 6 && !gamePanel.getMap().isCellWall(cellCol, cellRow)
                && !isNextToPlayer() && !isBlockedByOtherEnemy()) {
            placeAtCell(cellCol, cellRow);
            unstuckCount++;
        }
        stuckAnchorX = posX;
        stuckAnchorY = posY;
    }

    /** เริ่มนับเวลากันติดใหม่ (เรียกตอนหยุดเดินโดยตั้งใจ เช่น Boss ชาร์จ / วาร์ป) */
    protected void resetStuckTimer() {
        stuckTimer = 0;
        stuckAnchorX = posX;
        stuckAnchorY = posY;
    }

    /** ประชิดตัว Player อยู่แล้ว (ไม่ขยับเพราะกำลังตี ไม่ใช่ติด) */
    private boolean isNextToPlayer() {
        Player player = gamePanel.getPlayer();
        double distance = Math.hypot(player.getCenterX() - getCenterX(), player.getCenterY() - getCenterY());
        return distance < (width + Player.SIZE) / 2.0 + 24;
    }

    /** มีศัตรูตัวอื่นขวางอยู่ (ต่อแถวกันในทางเดิน ไม่ใช่ติดกำแพง) */
    private boolean isBlockedByOtherEnemy() {
        for (Enemy other : gamePanel.getEnemies()) {
            if (other != this && other.alive
                    && Math.hypot(other.getCenterX() - getCenterX(), other.getCenterY() - getCenterY())
                       < (width + other.width) / 2.0 + 8) {
                return true;
            }
        }
        return false;
    }

    /** ถูกดึงออกจากจุดติดไปกี่ครั้ง (ใช้ตอนทดสอบ) */
    private int unstuckCount = 0;

    public int getUnstuckCount() {
        return unstuckCount;
    }

    /** ตรวจว่าลากกล่องขนาด boxSize จากจุด 1 ไปจุด 2 แล้วไม่ชนกำแพง */
    protected boolean hasLineOfSight(int x1, int y1, int x2, int y2, int boxSize) {
        Map map = gamePanel.getMap();
        double distance = Math.hypot(x2 - x1, y2 - y1);
        int steps = (int) (distance / 8) + 1;
        int half = boxSize / 2;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int sx = (int) (x1 + (x2 - x1) * t);
            int sy = (int) (y1 + (y2 - y1) * t);
            if (map.isWallCollision(sx - half, sy - half, half * 2, half * 2)) {
                return false;
            }
        }
        return true;
    }

    // =========================
    // Common Update Helpers
    // =========================
    /** อัปเดต x, y (int) และ Hitbox จากตำแหน่งละเอียด */
    protected void syncPosition() {
        x = (int) Math.round(posX);
        y = (int) Math.round(posY);
        updateHitbox();
    }

    protected void updateHitbox() {
        hitbox.x = x;
        hitbox.y = y;
    }

    /** นับเวลาอมตะหลังโดนตี */
    protected void updateInvincibility() {
        if (!invincible) {
            return;
        }
        invincibleCounter++;
        if (invincibleCounter >= INVINCIBLE_FRAMES) {
            invincible = false;
            invincibleCounter = 0;
        }
    }

    // =========================
    // Take Damage (จาก Damageable)
    // =========================
    @Override
    public void takeDamage(int damage) {
        if (!alive || invincible) {
            return;
        }

        hp -= damage;
        invincible = true;
        invincibleCounter = 0;
        System.out.println(getClass().getSimpleName() + " HP: " + hp);

        // ตัวเลขความเสียหาย + ประกาย + เสียง + กระเด็นถอยหลังเล็กน้อย
        gamePanel.addDamageText(new DamageText(
                gamePanel, "-" + damage, new Color(255, 230, 60), getCenterX(), y - 16));
        gamePanel.getEffects().burst(getCenterX(), getCenterY(), 8, new Color(255, 240, 150), 4, 6, 14);
        Sound.ENEMY_HIT.play();
        knockback(8);

        if (hp <= 0) {
            hp = 0;
            alive = false;
            onDeath();
            System.out.println(getClass().getSimpleName() + " Dead!");
        }
    }

    /** ถูกผลักออกจาก Player */
    private void knockback(double power) {
        double dx = getCenterX() - gamePanel.getPlayer().getCenterX();
        double dy = getCenterY() - gamePanel.getPlayer().getCenterY();
        double length = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        moveWithCollision(dx / length * power, dy / length * power);
    }

    /** Effect ตอนตาย (คลาสลูก Override เพิ่มได้ เช่น Boss) */
    protected void onDeath() {
        gamePanel.getEffects().burst(getCenterX(), getCenterY(), 22, getDeathColor(), 6, 10, 26);
        gamePanel.getEffects().ring(getCenterX(), getCenterY(), width, Color.WHITE, 18);
        gamePanel.getEffects().shake(4, 8);
        Sound.ENEMY_DIE.play();
    }

    /** สีของเศษตอนตาย */
    protected Color getDeathColor() {
        return Color.WHITE;
    }

    // =========================
    // Animation
    // =========================
    /** เลือกแถวของ Sprite ตามทิศที่ขยับมากที่สุด */
    protected void updateMovementAnimation(int oldX, int oldY) {
        int dx = x - oldX;
        int dy = y - oldY;

        if (dx == 0 && dy == 0) {
            moving = false;
            spriteColumn = 0;
            animationCounter = 0;
            return;
        }

        moving = true;
        setDirection(dx, dy);
    }

    /** หันหน้าไปหาจุด (ใช้ตอนยืนยิง / ยืนตี) */
    protected void faceToward(int targetX, int targetY) {
        setDirection(targetX - getCenterX(), targetY - getCenterY());
    }

    private void setDirection(int dx, int dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            direction = dx > 0 ? "right" : "left";
            spriteRow = dx > 0 ? 2 : 3;
        } else {
            direction = dy > 0 ? "down" : "up";
            spriteRow = dy > 0 ? 0 : 1;
        }
    }

    protected void updateAnimation() {
        if (!moving) {
            spriteColumn = 0;
            animationCounter = 0;
            return;
        }

        animationCounter++;
        if (animationCounter > 10) {
            spriteColumn = (spriteColumn + 1) % 4;
            animationCounter = 0;
        }
    }

    /** โหลดรูปด้วย ImageIcon (ถ้าไม่พบไฟล์ sprite จะเป็น null แล้ววาดกล่องสีแทน) */
    protected void loadSprite(String filePath) {
        ImageIcon icon = new ImageIcon(filePath);
        if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
            sprite = icon.getImage();
        } else {
            System.out.println("Sprite not found: " + filePath + " (using placeholder)");
        }
    }

    // =========================
    // Common Draw Helpers
    // =========================
    /** วาด 1 เฟรมของ Sprite Sheet ให้พอดีกับ Hitbox (รูป = Hitbox) */
    protected void drawAnimatedSprite(Graphics g) {
        if (sprite == null) {
            return;
        }
        Camera camera = gamePanel.getCamera();
        int dx = camera.screenX(x);
        int dy = camera.screenY(y);
        int sx = spriteColumn * spriteWidth;
        int sy = spriteRow * spriteHeight;

        g.drawImage(sprite,
                dx, dy, dx + width, dy + height,
                sx, sy, sx + spriteWidth, sy + spriteHeight,
                null);
    }

    /** กล่องสีแทนรูป (ใช้เมื่อโหลด Sprite ไม่ได้) */
    protected void drawPlaceholder(Graphics g, Color color) {
        Camera camera = gamePanel.getCamera();
        g.setColor(color);
        g.fillRect(camera.screenX(x), camera.screenY(y), width, height);
    }

    /** เงาใต้ตัว (ศัตรูตีไกลมีเงาเป็นวงสีของกระสุน ไว้แยกกับตัวตีใกล้) */
    protected void drawShadow(Graphics g) {
        Camera camera = gamePanel.getCamera();
        int shadowWidth = width * 3 / 4;
        int shadowX = camera.screenX(getCenterX() - shadowWidth / 2);
        int shadowY = camera.screenY(y + height - 12);

        if (style == CombatStyle.RANGED) {
            g.setColor(new Color(projectileColor.getRed(), projectileColor.getGreen(), projectileColor.getBlue(), 110));
            g.fillOval(shadowX - 6, shadowY - 3, shadowWidth + 12, 20);
            g.setColor(projectileColor);
            g.drawOval(shadowX - 6, shadowY - 3, shadowWidth + 12, 20);
        } else {
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval(shadowX, shadowY, shadowWidth, 14);
        }
    }

    /**
     * วาดตัวศัตรู (รูป หรือกล่องสีถ้าไม่มีรูป)
     * ระหว่างอมตะจะกระพริบ: ซ่อนตัว 5 เฟรม / แสดง 5 เฟรม เหมือน Player
     */
    protected void drawBody(Graphics g, Color placeholderColor) {
        drawShadow(g);
        drawAttackWarning(g);
        if (invincible && invincibleCounter % 10 < 5) {
            return;
        }
        if (sprite != null) {
            drawAnimatedSprite(g);
        } else {
            drawPlaceholder(g, placeholderColor);
        }
    }

    /**
     * สัญญาณเตือนก่อนโจมตี (วาดใต้ตัวศัตรู)
     *   ตีใกล้ = วงแดงบนพื้น วงในขยายจนเต็ม = ฟาด
     *   ตีไกล  = เส้นเล็งสีแดงไปยังจุดที่ล็อกเป้า + วงเล็กที่ปลายเส้น
     */
    protected void drawAttackWarning(Graphics g) {
        Camera camera = gamePanel.getCamera();
        if (meleeTimer > 0) {
            double t = 1 - (double) meleeTimer / meleeWindupFrames;      // 0 -> 1
            drawDangerCircle(g, camera.screenX(getCenterX()), camera.screenY(getCenterY()), meleeRadius, t);
        }
        if (chargeTimer > 0) {
            double t = 1 - (double) chargeTimer / CHARGE_FRAMES;
            int sx = camera.screenX(getCenterX());
            int sy = camera.screenY(getCenterY());
            double dx = aimX - getCenterX();
            double dy = aimY - getCenterY();
            double length = Math.max(1, Math.hypot(dx, dy));
            int lineLength = 420;
            int ex = sx + (int) (dx / length * lineLength);
            int ey = sy + (int) (dy / length * lineLength);

            // เส้นประสีแดง เข้มขึ้นเรื่อย ๆ จนยิง
            int alpha = (int) (70 + 150 * t);
            g.setColor(new Color(255, 50, 50, alpha));
            int dashes = lineLength / 20;
            for (int i = 0; i < dashes; i += 2) {
                int x1 = sx + (ex - sx) * i / dashes;
                int y1 = sy + (ey - sy) * i / dashes;
                int x2 = sx + (ex - sx) * (i + 1) / dashes;
                int y2 = sy + (ey - sy) * (i + 1) / dashes;
                g.drawLine(x1, y1, x2, y2);
                g.drawLine(x1 + 1, y1, x2 + 1, y2);
            }
            int tx = camera.screenX(aimX);
            int ty = camera.screenY(aimY);
            int r = (int) (22 - 10 * t);
            g.drawOval(tx - r, ty - r, r * 2, r * 2);
            g.drawLine(tx - r - 4, ty, tx + r + 4, ty);
            g.drawLine(tx, ty - r - 4, tx, ty + r + 4);

            // แสงที่ตัวศัตรู
            int glow = (int) (width * 0.25 + width * 0.35 * (1 - t));
            g.setColor(new Color(projectileColor.getRed(), projectileColor.getGreen(), projectileColor.getBlue(), alpha));
            g.drawOval(sx - glow, sy - glow, glow * 2, glow * 2);
        }
    }

    /** วงแดงเตือนบนพื้น (ใช้ร่วมกันทั้งศัตรูตีใกล้และ Boss) t = 0..1 ความคืบหน้าก่อนโจมตี */
    public static void drawDangerCircle(Graphics g, int cx, int cy, int radius, double t) {
        g.setColor(new Color(255, 40, 40, (int) (40 + 80 * t)));
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        int inner = (int) (radius * t);
        g.setColor(new Color(255, 120, 80, 210));
        g.drawOval(cx - inner, cy - inner, inner * 2, inner * 2);
        g.setColor(new Color(255, 60, 60, 230));
        g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g.drawOval(cx - radius + 1, cy - radius + 1, radius * 2 - 2, radius * 2 - 2);
    }

    /** ไอคอนกุญแจหีบเล็ก ๆ เหนือหัว (ศัตรูที่ถือกุญแจหีบ) */
    protected void drawChestKeyMark(Graphics g) {
        if (!carriesChestKey) {
            return;
        }
        Camera camera = gamePanel.getCamera();
        int bob = (int) (3 * Math.sin(System.nanoTime() / 200000000.0));
        ChestKey.drawShape(g, camera.screenX(getCenterX()) - 12, camera.screenY(y) - 34 + bob, 24);
    }

    protected void drawHPBar(Graphics g) {
        int barWidth = width;
        int barHeight = 8;
        int barX = gamePanel.getCamera().screenX(x);
        int barY = gamePanel.getCamera().screenY(y - 12);

        g.setColor(Color.BLACK);
        g.fillRect(barX, barY, barWidth, barHeight);

        int currentHpWidth = (int) ((double) hp / maxHp * barWidth);
        g.setColor(style == CombatStyle.RANGED ? new Color(230, 90, 230) : Color.RED);
        g.fillRect(barX, barY, currentHpWidth, barHeight);
    }

    protected void drawHitbox(Graphics g) {
        if (!gamePanel.isDebugMode()) {
            return;
        }
        Camera camera = gamePanel.getCamera();
        g.setColor(Color.YELLOW);
        g.drawRect(camera.screenX(hitbox.x), camera.screenY(hitbox.y), hitbox.width, hitbox.height);
    }

    // =========================
    // Getters
    // =========================
    public int getX() { return x; }
    public int getY() { return y; }
    public int getCenterX() { return x + width / 2; }
    public int getCenterY() { return y + height / 2; }
    public boolean isAlive() { return alive; }
    public boolean isInvincible() { return invincible; }
    public Rectangle getHitbox() { return hitbox; }
    public CombatStyle getStyle() { return style; }
    public int getSpeed() { return speed; }
    public boolean isCharging() { return chargeTimer > 0; }
    public boolean isWindingUpMelee() { return meleeTimer > 0; }
    public boolean carriesChestKey() { return carriesChestKey; }
    public void setCarriesChestKey(boolean carries) { carriesChestKey = carries; }
}
