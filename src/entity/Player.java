package entity;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

import interfaces.Damageable;
import item.Potion;
import main.GamePanel;
import skill.Skill;
import sound.Sound;
import world.Map;

/**
 * ตัวละครผู้เล่น (Snow Spirit)
 * - เดินด้วย WASD / ลูกศร
 * - คลิกซ้ายเพื่อยิงกระสุนผ่าน Fairy
 * - SPACE / SHIFT = Dash, Q / E = สกิล, 1-5 = ดื่มยา
 *
 * Stamina (สูงสุด 100): ยิง -4, Dash -25, สกิล -30/-35
 *   ฟื้นเองตลอด (ช้า) และฟื้นเร็วขึ้นเมื่อไม่ได้ใช้ 0.6 วินาที
 *
 * Hitbox = กรอบรูปที่วาด (96x96) ตำแหน่ง (x, y) คือมุมซ้ายบนของรูป
 * ขนาดคิดจาก Map.TILE_SIZE จึงสัมพันธ์กับช่องของแผนที่
 * (ทางเดินในแผนที่กว้าง 2 ช่อง = 128px ใหญ่กว่าตัว Player เสมอ)
 */
public class Player implements Damageable, KeyListener, MouseListener, MouseMotionListener {

    // =========================
    // Size (อิงจากขนาดช่องของแผนที่)
    // =========================
    private static final int TILE = Map.TILE_SIZE;                 // 64
    public static final int SIZE = TILE * 3 / 2;                   // 96 = รูป = Hitbox
    private static final int SPRITE_FRAME_SIZE = 128;              // ขนาด 1 เฟรมในไฟล์รูป

    /** ถ้าเดินชนมุมกำแพงไม่เกินระยะนี้ จะถูกดันให้ไถลเข้าทางเดินเอง */
    private static final int CORNER_TOLERANCE = TILE * 3 / 4;      // 48 (ทางเดินกว้าง 128 ตัว 96 -> เยื้องได้เกือบครึ่งช่อง)

    // =========================
    // Stats
    // =========================
    private static final int NORMAL_MAX_HP = 6;
    private static final int NORMAL_SPEED = 4;
    private static final int NORMAL_PROJECTILE_DAMAGE = 1;
    private static final int SHOOT_COOLDOWN_FRAMES = 15;           // 0.25 วินาที (60 FPS)
    public static final int MAX_POTIONS = 5;                       // กระเป๋ายาจุได้ 5 ขวด

    // =========================
    // Stamina
    // =========================
    public static final int MAX_STAMINA = 100;
    public static final int SHOOT_STAMINA = 4;
    public static final int DASH_STAMINA = 25;
    private static final double STAMINA_REGEN = 0.2;               // ต่อเฟรม (ฟื้นตลอดเวลา = 12 ต่อวินาที)
    private static final double STAMINA_REGEN_REST = 0.5;          // ต่อเฟรม เมื่อไม่ได้ใช้ Stamina (30 ต่อวินาที)
    private static final int STAMINA_REST_FRAMES = 36;             // ไม่ได้ใช้ 0.6 วินาที = พัก

    // =========================
    // Skills: ช่อง 0 = Q (ICE SHIELD), ช่อง 1 = E (ICE SWORD)
    // =========================
    public static final int SKILL_SLOTS = 2;

    // =========================
    // Dash (Space / Shift): พุ่งสั้น ๆ 0.17 วินาที ระยะ ~140px อมตะระหว่างพุ่ง
    // =========================
    private static final int DASH_FRAMES = 10;
    private static final double DASH_SPEED = 14;
    public static final int DASH_COOLDOWN = 50;                    // ใช้ได้ทุก ~0.8 วินาที

    // =========================
    // References
    // =========================
    private final GamePanel gamePanel;
    private final Fairy fairy;          // Composition: Player "มี" Fairy

    // =========================
    // Position
    // =========================
    private int x;
    private int y;
    private final Rectangle hitbox;
    private int mouseX;
    private int mouseY;

    private int hp = NORMAL_MAX_HP;
    private int maxHp = NORMAL_MAX_HP;
    private int baseMaxHp = NORMAL_MAX_HP;      // HP สูงสุดตามระดับความยาก
    private int speed = NORMAL_SPEED;
    private int projectileDamage = NORMAL_PROJECTILE_DAMAGE;

    private boolean hasKey = false;
    private boolean godMode = false;
    private boolean invincible = false;
    private int invincibleCounter = 0;
    private int shootCooldown = 0;          // > 0 = ยังยิงไม่ได้
    private double stamina = MAX_STAMINA;
    private int staminaRestTimer = 0;       // นับเฟรมตั้งแต่ใช้ Stamina ครั้งล่าสุด
    private int noStaminaTimer = 0;         // > 0 = แถบ Stamina กระพริบแดง (เพิ่งใช้ไม่พอ)
    private int chestKeys = 0;              // กุญแจหีบ (เก็บข้ามด่านได้)
    private int messageCooldown = 0;        // กันข้อความเตือนขึ้นรัว ๆ
    private int dashTimer = 0;              // > 0 = กำลังพุ่ง
    private int dashCooldown = 0;           // > 0 = ยังพุ่งไม่ได้
    private double dashDirX = 0;
    private double dashDirY = 0;
    private double dashRemainderX = 0;      // เศษทศนิยมของระยะพุ่ง (พุ่งเฉียงได้แม่น)
    private double dashRemainderY = 0;

    // กระเป๋ายา (Composition: Player "มี" Potion หลายขวด)
    private final ArrayList<Potion> potions = new ArrayList<Potion>();

    // สกิลที่ได้จากหีบ (Composition + Polymorphism: เก็บเป็นชนิด Skill ทั้งหมด)
    private final Skill[] skills = new Skill[SKILL_SLOTS];

    // =========================
    // Animation
    // =========================
    private final ImageIcon spriteSheet = new ImageIcon("assets/Snow_spirit.png");
    private int spriteRow = 0;
    private int spriteColumn = 0;
    private int animationCounter = 0;
    private boolean moving = false;
    private String direction = "down";

    // =========================
    // Input
    // =========================
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;

    // =========================
    // Constructor
    // =========================
    public Player(GamePanel gamePanel) {
        this.gamePanel = gamePanel;

        hitbox = new Rectangle(0, 0, SIZE, SIZE);
        setBlockPosition(2, 2);

        mouseX = gamePanel.getWidth() / 2;
        mouseY = gamePanel.getHeight() / 2;

        fairy = new Fairy(gamePanel, this);
    }

    /** วาง Player ให้อยู่กลาง Cell (128x128) ของเขาวงกต ตอนเริ่มด่าน */
    public void setCellPosition(int cellCol, int cellRow) {
        setBlockPosition(cellCol * 2, cellRow * 2);
    }

    /** HP สูงสุดตามระดับความยาก (เรียกตอนเริ่มเกม) */
    public void setMaxHp(int value) {
        baseMaxHp = value;
        maxHp = value;
        hp = value;
    }

    /** วาง Player ให้อยู่กลางกล่อง 2x2 ช่อง ที่เริ่มช่อง (col, row) ตอนเริ่มด่าน */
    public void setBlockPosition(int col, int row) {
        int blockSize = Map.blocksFor(SIZE);
        x = Map.blockCenter(col, blockSize) - SIZE / 2;
        y = Map.blockCenter(row, blockSize) - SIZE / 2;
        updateHitbox();

        // ป้องกันปุ่มเดิมค้างตอนเปลี่ยนด่าน
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;

        moving = false;
        spriteColumn = 0;
        animationCounter = 0;
    }

    // =========================
    // Update
    // =========================
    public void update() {
        if (dashTimer > 0) {
            updateDash();
        } else {
            updateWalk();
        }

        updateHitbox();
        updateAnimation();
        updateInvincibility();
        updateTimers();
        updateStamina();
        for (Skill skill : skills) {
            if (skill != null) {
                skill.update(this);     // Polymorphism: แต่ละสกิล update ต่างกัน
            }
        }
        fairy.update(mouseX, mouseY);
    }

    /** เดินปกติด้วย WASD */
    private void updateWalk() {
        int moveSpeed = speed;
        int dx = 0;
        int dy = 0;

        if (upPressed) {
            dy -= moveSpeed;
            direction = "up";
        }
        if (downPressed) {
            dy += moveSpeed;
            direction = "down";
        }
        if (leftPressed) {
            dx -= moveSpeed;
            direction = "left";
        }
        if (rightPressed) {
            dx += moveSpeed;
            direction = "right";
        }
        moving = dx != 0 || dy != 0;

        // แยกเดินแกน X และแกน Y -> ชนกำแพงด้านหนึ่งก็ยังไถลไปอีกด้านได้
        // ไถลเข้ามุมเฉพาะตอนกดทิศเดียว (กดเฉียงอยู่แล้วก็ไถลเองตามแกนอื่น)
        moveX(dx, dy == 0);
        moveY(dy, dx == 0);
    }

    // =========================
    // Dash
    // =========================
    /** เริ่มพุ่งไปทางที่กดปุ่มเดินอยู่ (ถ้าไม่ได้กด จะพุ่งไปทางเมาส์) */
    public void startDash() {
        if (dashTimer > 0 || dashCooldown > 0) {
            return;
        }

        double dirX = (rightPressed ? 1 : 0) - (leftPressed ? 1 : 0);
        double dirY = (downPressed ? 1 : 0) - (upPressed ? 1 : 0);
        if (dirX == 0 && dirY == 0) {
            dirX = mouseX + gamePanel.getCamera().getX() - getCenterX();
            dirY = mouseY + gamePanel.getCamera().getY() - getCenterY();
        }
        double length = Math.sqrt(dirX * dirX + dirY * dirY);
        if (length < 0.001 || !useStamina(DASH_STAMINA)) {
            return;
        }

        dashDirX = dirX / length;
        dashDirY = dirY / length;
        dashRemainderX = 0;
        dashRemainderY = 0;
        dashTimer = DASH_FRAMES;
        dashCooldown = DASH_COOLDOWN + DASH_FRAMES;

        gamePanel.getEffects().ring(getCenterX(), getCenterY(), 60, new Color(170, 230, 255), 12);
        Sound.DASH.play();
    }

    private void updateDash() {
        dashTimer--;
        moving = true;

        double stepX = dashDirX * DASH_SPEED + dashRemainderX;
        double stepY = dashDirY * DASH_SPEED + dashRemainderY;
        int moveXAmount = (int) stepX;
        int moveYAmount = (int) stepY;
        dashRemainderX = stepX - moveXAmount;
        dashRemainderY = stepY - moveYAmount;

        moveX(moveXAmount, false);
        moveY(moveYAmount, false);

        // เกล็ดหิมะตามหลังตอนพุ่ง
        gamePanel.getEffects().trail(getCenterX(), getCenterY(), new Color(190, 235, 255), 26);
        gamePanel.getEffects().trail(getCenterX() + (int) ((Math.random() - 0.5) * 50),
                getCenterY() + (int) ((Math.random() - 0.5) * 50), Color.WHITE, 8);
    }

    public boolean isDashing() {
        return dashTimer > 0;
    }

    /** นับถอยหลังคูลดาวน์การยิง / Dash */
    private void updateTimers() {
        if (shootCooldown > 0) {
            shootCooldown--;
        }
        if (dashCooldown > 0) {
            dashCooldown--;
        }
        if (noStaminaTimer > 0) {
            noStaminaTimer--;
        }
        if (messageCooldown > 0) {
            messageCooldown--;
        }
    }

    // =========================
    // Stamina
    // =========================
    private void updateStamina() {
        staminaRestTimer++;
        double regen = staminaRestTimer >= STAMINA_REST_FRAMES ? STAMINA_REGEN_REST : STAMINA_REGEN;
        stamina = Math.min(MAX_STAMINA, stamina + regen);
    }

    /**
     * ใช้ Stamina: พอ = หักแล้วคืน true / ไม่พอ = เตือน (แถบกระพริบ + เสียง) แล้วคืน false
     * God Mode ใช้ได้ไม่จำกัด
     */
    public boolean useStamina(int cost) {
        if (godMode) {
            return true;
        }
        if (stamina < cost) {
            noStaminaTimer = 30;
            if (messageCooldown == 0) {
                messageCooldown = 45;
                gamePanel.addDamageText(new DamageText(
                        gamePanel, "NO STAMINA", new Color(255, 200, 90), getCenterX(), y));
                Sound.NO_STAMINA.play();
            }
            return false;
        }
        stamina -= cost;
        staminaRestTimer = 0;
        return true;
    }

    public void restoreStamina(int amount) {
        double before = stamina;
        stamina = Math.min(MAX_STAMINA, stamina + amount);
        gamePanel.addDamageText(new DamageText(
                gamePanel, "+" + (int) Math.round(stamina - before) + " STAMINA", new Color(120, 240, 130), getCenterX(), y));
    }

    // =========================
    // Skills
    // =========================
    /** กดปุ่มสกิล (Q = 0, E = 1) */
    public void useSkill(int slot) {
        Skill skill = skills[slot];
        if (skill == null) {
            showMessage("No skill yet - open chests!", new Color(200, 210, 230));
            return;
        }
        if (skill.isActive()) {
            return;
        }
        if (skill.getCooldownTimer() > 0) {
            showMessage(skill.getName() + " " + ((skill.getCooldownTimer() + 59) / 60) + "s", new Color(200, 210, 230));
            return;
        }
        skill.tryUse(this);
    }

    /** เรียนสกิลใหม่ หรืออัปเลเวลถ้ามีอยู่แล้ว @return true ถ้าเป็นการอัปเลเวล */
    public boolean learnSkill(Skill skill) {
        Skill current = skills[skill.getSlot()];
        if (current == null) {
            skills[skill.getSlot()] = skill;
            return false;
        }
        current.upgrade();
        return true;
    }

    /** เรียน / อัปเลเวลสกิลนี้ได้อีกไหม (ใช้ตอนสุ่มของในหีบ) */
    public boolean canLearnSkill(Skill skill) {
        Skill current = skills[skill.getSlot()];
        return current == null || current.canUpgrade();
    }

    public boolean hasAnySkill() {
        for (Skill skill : skills) {
            if (skill != null) {
                return true;
            }
        }
        return false;
    }

    private void showMessage(String text, Color color) {
        if (messageCooldown > 0) {
            return;
        }
        messageCooldown = 45;
        gamePanel.addDamageText(new DamageText(gamePanel, text, color, getCenterX(), y));
        Sound.LOCKED.play();
    }

    // =========================
    // Movement + Collision
    // =========================
    private boolean canStandAt(int newX, int newY) {
        return !gamePanel.getMap().isWallCollision(newX, newY, SIZE, SIZE);
    }

    /** เดินทีละ 1 pixel จนชนกำแพง -> ชิดกำแพงพอดี ไม่เหลือช่องว่าง */
    private void moveX(int dx, boolean allowSlide) {
        int step = Integer.signum(dx);
        for (int i = 0; i < Math.abs(dx); i++) {
            if (canStandAt(x + step, y)) {
                x += step;
            } else {
                if (allowSlide) {
                    slideAroundCorner(step, 0);
                }
                return;
            }
        }
    }

    private void moveY(int dy, boolean allowSlide) {
        int step = Integer.signum(dy);
        for (int i = 0; i < Math.abs(dy); i++) {
            if (canStandAt(x, y + step)) {
                y += step;
            } else {
                if (allowSlide) {
                    slideAroundCorner(0, step);
                }
                return;
            }
        }
    }

    /**
     * Corner Sliding: ถ้าเดินชนขอบมุมกำแพงนิดเดียว
     * ให้ดันตัวไปด้านข้างเพื่อเข้าช่องทางเดินได้เอง (ไม่ต้องเล็งให้ตรงเป๊ะ)
     */
    private void slideAroundCorner(int stepX, int stepY) {
        for (int offset = 1; offset <= CORNER_TOLERANCE; offset++) {
            for (int side = -1; side <= 1; side += 2) {
                // เลื่อนตั้งฉากกับทิศที่เดิน
                int shiftX = (stepX == 0) ? side * offset : 0;
                int shiftY = (stepY == 0) ? side * offset : 0;

                if (canStandAt(x + shiftX, y + shiftY)
                        && canStandAt(x + shiftX + stepX, y + shiftY + stepY)) {
                    nudge(Integer.signum(shiftX), Integer.signum(shiftY), Math.min(speed, offset));
                    return;
                }
            }
        }
    }

    private void nudge(int stepX, int stepY, int amount) {
        for (int i = 0; i < amount && canStandAt(x + stepX, y + stepY); i++) {
            x += stepX;
            y += stepY;
        }
    }

    private void updateHitbox() {
        hitbox.x = x;
        hitbox.y = y;
    }

    private void updateAnimation() {
        if (!moving) {
            spriteColumn = 0;   // หยุดเดิน = Frame แรก
            return;
        }

        animationCounter++;
        if (animationCounter > 6) {
            spriteColumn = (spriteColumn + 1) % 4;
            animationCounter = 0;
        }
    }

    private void updateInvincibility() {
        if (!invincible) {
            return;
        }
        invincibleCounter++;
        if (invincibleCounter > 60) {
            invincible = false;
            invincibleCounter = 0;
        }
    }

    // =========================
    // Combat
    // =========================
    @Override
    public void takeDamage(int damage) {
        if (godMode || invincible || isDashing()) {     // Dash = อมตะระหว่างพุ่ง
            return;
        }

        // โล่ (หรือสกิลอื่นที่กันดาเมจได้) รับแทน -> ไม่เสีย HP แต่ได้อมตะสั้น ๆ 0.5 วินาที
        for (Skill skill : skills) {
            if (skill != null && skill.absorbHit(this)) {
                invincible = true;
                invincibleCounter = 30;
                return;
            }
        }

        hp -= damage;
        if (hp < 0) {
            hp = 0;
        }
        System.out.println("Player HP: " + hp);

        // อมตะชั่วคราวหลังโดนตี (60 เฟรม)
        invincible = true;
        invincibleCounter = 0;

        // Effect โดนตี: จอแดงวาบ + จอสั่น + เศษกระจาย + เสียง
        gamePanel.getEffects().flash(new Color(255, 0, 0, 110), 14);
        gamePanel.getEffects().shake(7, 12);
        gamePanel.getEffects().burst(getCenterX(), getCenterY(), 14, new Color(255, 90, 90), 5, 8, 18);
        gamePanel.addDamageText(new DamageText(
                gamePanel, "-" + damage, new Color(255, 80, 80), getCenterX(), y));
        Sound.PLAYER_HURT.play();

        if (hp <= 0) {
            gamePanel.setGameOver(true);
            System.out.println("Player Dead!");
        }
    }

    /** ยิงได้ 1 นัดทุก 0.25 วินาที */
    public void shoot() {
        if (shootCooldown > 0 || !useStamina(SHOOT_STAMINA)) {
            return;
        }
        shootCooldown = SHOOT_COOLDOWN_FRAMES;

        // แสงวาบที่ปลาย Fairy + เสียงยิง
        gamePanel.getEffects().burst((int) fairy.getCenterX(), (int) fairy.getCenterY(), 5,
                new Color(150, 240, 255), 2.5, 6, 8);
        Sound.SHOOT.play();

        gamePanel.addProjectile(new Projectile(
                gamePanel,
                fairy.getCenterX(),
                fairy.getCenterY(),
                fairy.getAimX(),
                fairy.getAimY(),
                projectileDamage));
    }

    // =========================
    // Potions
    // =========================
    /** เก็บยาเข้ากระเป๋า คืนค่า false ถ้ากระเป๋าเต็ม (ยาจะยังอยู่บนพื้น) */
    public boolean addPotion(Potion potion) {
        if (potions.size() >= MAX_POTIONS) {
            return false;
        }
        potions.add(potion);
        return true;
    }

    /** ดื่มยาช่องที่ index (0-4) — Polymorphism: แต่ละชนิดทำงานต่างกันใน use() */
    public void usePotion(int index) {
        if (index < 0 || index >= potions.size()) {
            return;
        }
        Potion potion = potions.get(index);
        if (!potion.canUse(this)) {
            gamePanel.addDamageText(new DamageText(
                    gamePanel, "HP FULL", Color.WHITE, getCenterX(), y));
            return;
        }
        potion.use(this);
        potions.remove(index);
        gamePanel.getEffects().sparkle(getCenterX(), getCenterY(), 16, potion.getLiquidColor());
        Sound.DRINK.play();
        System.out.println("Used " + potion.getName());
    }

    /** ฟื้น HP (ไม่เกิน maxHp) แล้วแสดงตัวเลขสีเขียว */
    public void heal(int amount) {
        int before = hp;
        hp = Math.min(maxHp, hp + amount);
        gamePanel.addDamageText(new DamageText(
                gamePanel, "+" + (hp - before) + " HP", new Color(90, 255, 120), getCenterX(), y));
    }

    // =========================
    // Chest Keys
    // =========================
    public void addChestKey() {
        chestKeys++;
        gamePanel.addDamageText(new DamageText(
                gamePanel, "+1 CHEST KEY", new Color(185, 215, 240), getCenterX(), y));
    }

    /** ใช้กุญแจหีบ 1 ดอก (ไม่มีกุญแจ = false) */
    public boolean useChestKey() {
        if (chestKeys <= 0) {
            return false;
        }
        chestKeys--;
        return true;
    }

    /** เปิด/ปิด God Mode (ตั้งจากเมนู Setting ก่อนเริ่มเกม) */
    public void setGodMode(boolean on) {
        godMode = on;

        if (godMode) {
            maxHp = 1000;
            hp = 1000;
            speed = 20;
            projectileDamage = 1000;
            System.out.println("GODMODE: ON");
        } else {
            maxHp = baseMaxHp;
            hp = baseMaxHp;
            speed = NORMAL_SPEED;
            projectileDamage = NORMAL_PROJECTILE_DAMAGE;
            System.out.println("GODMODE: OFF");
        }
    }

    // =========================
    // Draw
    // =========================
    public void draw(Graphics g) {
        // กระพริบตอนอมตะ (ซ่อนแค่ตัวละคร ส่วนโล่ / ดาบยังวาดอยู่)
        if (invincible && invincibleCounter % 10 < 5) {
            drawSkills(g);
            return;
        }

        // แถวของ Sprite Sheet: 0=ลง, 1=ขึ้น, 2=ขวา, 3=ซ้าย
        if (direction.equals("down")) {
            spriteRow = 0;
        } else if (direction.equals("up")) {
            spriteRow = 1;
        } else if (direction.equals("right")) {
            spriteRow = 2;
        } else if (direction.equals("left")) {
            spriteRow = 3;
        }

        int screenX = gamePanel.getCamera().screenX(x);
        int screenY = gamePanel.getCamera().screenY(y);
        int sx = spriteColumn * SPRITE_FRAME_SIZE;
        int sy = spriteRow * SPRITE_FRAME_SIZE;

        // ตัด 1 Frame จาก Sprite Sheet แล้ววาดให้พอดีกับ Hitbox
        g.drawImage(spriteSheet.getImage(),
                screenX, screenY, screenX + SIZE, screenY + SIZE,
                sx, sy, sx + SPRITE_FRAME_SIZE, sy + SPRITE_FRAME_SIZE,
                null);

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.GREEN);
            g.drawRect(
                    gamePanel.getCamera().screenX(hitbox.x),
                    gamePanel.getCamera().screenY(hitbox.y),
                    hitbox.width, hitbox.height);
        }

        fairy.draw(g);
        drawSkills(g);
    }

    private void drawSkills(Graphics g) {
        for (Skill skill : skills) {
            if (skill != null) {
                skill.draw(g, this);
            }
        }
    }

    // =========================
    // Keyboard
    // =========================
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        setDirectionKey(key, true);

        // ปุ่ม 1-5 = ดื่มยาช่องที่ 1-5
        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_5) {
            usePotion(key - KeyEvent.VK_1);
        }

        // Space หรือ Shift = Dash
        if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_SHIFT) {
            startDash();
        }

        // Q / E = สกิล
        if (key == KeyEvent.VK_Q) {
            useSkill(0);
        }
        if (key == KeyEvent.VK_E) {
            useSkill(1);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        setDirectionKey(e.getKeyCode(), false);
    }

    private void setDirectionKey(int key, boolean pressed) {
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) {
            upPressed = pressed;
        }
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) {
            downPressed = pressed;
        }
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) {
            leftPressed = pressed;
        }
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) {
            rightPressed = pressed;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    // =========================
    // Mouse
    // =========================
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            shoot();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    // =========================
    // Getters / Setters
    // =========================
    public int getX() { return x; }
    public int getY() { return y; }

    /** จุดกลางตัว Player (= กลาง Hitbox) */
    public int getCenterX() { return x + SIZE / 2; }
    public int getCenterY() { return y + SIZE / 2; }

    public Rectangle getHitbox() { return hitbox; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isGodMode() { return godMode; }
    public boolean hasKey() { return hasKey; }
    public ArrayList<Potion> getPotions() { return potions; }
    public int getDashCooldown() { return dashCooldown; }
    public double getStamina() { return stamina; }
    public int getMaxStamina() { return MAX_STAMINA; }
    public boolean isStaminaWarning() { return noStaminaTimer > 0; }
    public int getChestKeys() { return chestKeys; }
    public Skill getSkill(int slot) { return skills[slot]; }
    public void setHasKey(boolean hasKey) { this.hasKey = hasKey; }
}
