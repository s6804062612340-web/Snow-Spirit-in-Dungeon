package main;

import java.awt.*;
import java.util.ArrayList;

import entity.Player;
import entity.enemy.Boss;
import entity.enemy.Enemy;
import item.Chest;
import item.ChestKey;
import item.Item;
import item.Potion;
import skill.Skill;
import world.LevelMaps;

/**
 * ส่วนแสดงผลบนจอระหว่างเล่น (Heads-Up Display) แยกออกจาก GamePanel ให้อ่านง่าย
 * Composition: GamePanel "มี" Hud แล้วเรียก hud.draw(g) หลังวาดโลกเกมเสร็จ
 *
 *  ┌──────────────┐        ┌───────────┐        [ MENU ]
 *  │ ♥♥♥♥♥  HP    │        │ LEVEL 3/10│      ┌─────────┐
 *  │ ▓▓▓▓▓ STAMINA│        │ NORMAL·MED│      │ TIME    │
 *  └──────────────┘        └───────────┘      │ ENEMIES │
 *                                             │ KEYS    │
 *                                             └─────────┘
 *        ┌──────────────────────────────────────────┐
 *        │ [1][2][3][4][5]  │ [DASH] [Q] [E]          │
 *        └──────────────────────────────────────────┘
 */
public class Hud {

    // ===== สีหลักของ UI =====
    private static final Color PANEL = new Color(12, 18, 34, 185);
    private static final Color PANEL_BORDER = new Color(150, 190, 255, 70);
    private static final Color TEXT = new Color(235, 242, 255);
    private static final Color TEXT_DIM = new Color(150, 170, 205);
    private static final Color HEART = new Color(240, 70, 90);
    private static final Color STAMINA = new Color(110, 225, 130);
    private static final Color ICE = new Color(150, 220, 255);
    private static final Color GOLD = new Color(255, 210, 70);

    // ===== สีลูกศรบอกทาง =====
    private static final Color ARROW_DOOR = new Color(255, 215, 0);
    private static final Color ARROW_ENEMY = new Color(255, 70, 70);
    private static final Color ARROW_BOSS = new Color(190, 90, 255);
    private static final Color ARROW_CHEST = new Color(170, 215, 255);

    private static final int TOP_RESERVED = 100;     // ขอบบนที่ลูกศรจะไม่ไปทับ UI
    private static final int BOTTOM_RESERVED = 100;

    private final GamePanel gamePanel;

    public Hud(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    // =========================
    // Draw ทั้งหมด
    // =========================
    public void draw(Graphics g) {
        Player player = gamePanel.getPlayer();
        if (!gamePanel.isGameOver() && !gamePanel.isVictory()) {
            drawIndicators(g, player);
        }
        drawStatusPanel(g, player);
        drawLevelPanel(g);
        drawInfoPanel(g, player);
        drawHotbar(g, player);
        drawModeChips(g, player);
        drawToast(g);
        drawLevelBanner(g);

        if (gamePanel.isGameOver()) {
            drawEndScreen(g, "GAME OVER", new Color(255, 110, 110));
        } else if (gamePanel.isVictory()) {
            drawEndScreen(g, "VICTORY!", GOLD);
        }
    }

    // =========================
    // มุมซ้ายบน: HP (หัวใจ) + Stamina
    // =========================
    private void drawStatusPanel(Graphics g, Player player) {
        int x = 12;
        int y = 12;
        drawPanel(g, x, y, 240, 74);

        if (player.getMaxHp() <= 10) {
            for (int i = 0; i < player.getMaxHp(); i++) {
                drawHeart(g, x + 14 + i * 28, y + 12, 22, i < player.getHp());
            }
        } else {
            // God Mode (HP 1000) -> ใช้แถบแทนหัวใจ
            drawBar(g, x + 14, y + 14, 212, 16, (double) player.getHp() / player.getMaxHp(), HEART,
                    "HP " + player.getHp());
        }

        double ratio = player.getStamina() / player.getMaxStamina();
        boolean warning = player.isStaminaWarning() && (gamePanel.getPlayFrames() / 4) % 2 == 0;
        Color fill = warning ? new Color(255, 90, 70) : (ratio < 0.25 ? new Color(230, 200, 80) : STAMINA);
        drawBar(g, x + 14, y + 46, 212, 14, ratio, fill, null);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        String label = "STAMINA  " + (int) player.getStamina();
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString(label, x + 21, y + 58);
        g.setColor(Color.WHITE);
        g.drawString(label, x + 20, y + 57);
    }

    /** หัวใจ 1 ดวง (วงกลม 2 วง + สามเหลี่ยม) */
    private void drawHeart(Graphics g, int x, int y, int size, boolean full) {
        int half = size / 2;
        int[] xs = { x, x + size, x + half };
        int[] ys = { y + half - 1, y + half - 1, y + size };

        g.setColor(new Color(0, 0, 0, 120));
        g.fillOval(x, y + 2, half + 1, half + 1);
        g.fillOval(x + half - 1, y + 2, half + 1, half + 1);

        g.setColor(full ? HEART : new Color(70, 40, 55));
        g.fillOval(x, y, half + 1, half + 1);
        g.fillOval(x + half - 1, y, half + 1, half + 1);
        g.fillPolygon(xs, ys, 3);
        if (full) {
            g.setColor(new Color(255, 200, 210));
            g.fillOval(x + 3, y + 3, size / 5, size / 5);
        }
    }

    // =========================
    // กลางบน: ด่าน + ระดับความยาก + ขนาดแผนที่
    // =========================
    private void drawLevelPanel(Graphics g) {
        int level = gamePanel.getCurrentLevelNumber();
        int width = 190;
        int x = (GamePanel.SCREEN_WIDTH - width) / 2;
        drawPanel(g, x, 12, width, 56);

        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(TEXT);
        String title = "LEVEL " + level + " / " + LevelMaps.MAX_LEVELS;
        drawCentered(g, title, GamePanel.SCREEN_WIDTH / 2, 38);

        g.setFont(new Font("Arial", Font.BOLD, 11));
        String mode = gamePanel.getDifficulty().getLabel();
        String size = LevelMaps.getMapSizeName(level) + " MAP";
        int modeWidth = g.getFontMetrics().stringWidth(mode);
        int totalWidth = modeWidth + 10 + g.getFontMetrics().stringWidth(size);
        int startX = GamePanel.SCREEN_WIDTH / 2 - totalWidth / 2;
        g.setColor(gamePanel.getDifficulty().getColor());
        g.drawString(mode, startX, 57);
        g.setColor(TEXT_DIM);
        g.drawString(size, startX + modeWidth + 10, 57);
    }

    // =========================
    // ขวาบน: เวลา / ศัตรูที่เหลือ / กุญแจ
    // =========================
    private void drawInfoPanel(Graphics g, Player player) {
        int width = 150;
        int x = GamePanel.SCREEN_WIDTH - width - 12;
        int y = 52;
        drawPanel(g, x, y, width, 84);

        g.setFont(new Font("Arial", Font.BOLD, 14));

        // เวลา
        g.setColor(TEXT_DIM);
        g.drawString("TIME", x + 12, y + 22);
        g.setColor(TEXT);
        drawRight(g, gamePanel.formatTime(gamePanel.getPlayFrames()), x + width - 12, y + 22);

        // ศัตรู
        g.setColor(TEXT_DIM);
        g.drawString("ENEMIES", x + 12, y + 46);
        g.setColor(gamePanel.getEnemiesRemaining() == 0 ? STAMINA : new Color(255, 130, 130));
        drawRight(g, String.valueOf(gamePanel.getEnemiesRemaining()), x + width - 12, y + 46);

        // กุญแจหีบ + กุญแจประตู
        ChestKey.drawShape(g, x + 12, y + 58, 22);
        g.setColor(TEXT);
        g.drawString("x " + player.getChestKeys(), x + 40, y + 72);
        if (player.hasKey()) {
            drawDoorKey(g, x + width - 44, y + 60);
            g.setFont(new Font("Arial", Font.BOLD, 9));
            g.setColor(GOLD);
            g.drawString("EXIT", x + width - 42, y + 80);
        }
    }

    private void drawDoorKey(Graphics g, int x, int y) {
        g.setColor(GOLD);
        g.fillOval(x, y, 14, 14);
        g.fillRect(x + 12, y + 5, 16, 5);
        g.fillRect(x + 23, y + 9, 3, 5);
        g.setColor(new Color(90, 60, 10));
        g.fillOval(x + 4, y + 4, 6, 6);
    }

    // =========================
    // ล่างกลาง: กระเป๋ายา 1-5 + DASH / Q / E
    // =========================
    private void drawHotbar(Graphics g, Player player) {
        int potionSlot = 46;
        int skillSlot = 54;
        int gap = 6;
        int potionsWidth = Player.MAX_POTIONS * potionSlot + (Player.MAX_POTIONS - 1) * gap;
        int skillsWidth = 3 * skillSlot + 2 * gap;
        int separator = 22;
        int totalWidth = potionsWidth + separator + skillsWidth;
        int x = (GamePanel.SCREEN_WIDTH - totalWidth) / 2;
        int bottom = GamePanel.SCREEN_HEIGHT - 14;

        drawPanel(g, x - 12, bottom - skillSlot - 12, totalWidth + 24, skillSlot + 24);

        // ยา
        ArrayList<Potion> bag = player.getPotions();
        int potionY = bottom - potionSlot - (skillSlot - potionSlot) / 2;
        for (int i = 0; i < Player.MAX_POTIONS; i++) {
            int slotX = x + i * (potionSlot + gap);
            drawSlot(g, slotX, potionY, potionSlot, false);
            if (i < bag.size()) {
                bag.get(i).drawIcon(g, slotX + (potionSlot - Potion.WIDTH) / 2, potionY + (potionSlot - Potion.HEIGHT) / 2);
            }
            drawKeyLabel(g, String.valueOf(i + 1), slotX, potionY);
        }

        // เส้นคั่น
        int sepX = x + potionsWidth + separator / 2;
        g.setColor(PANEL_BORDER);
        g.fillRect(sepX, bottom - skillSlot + 4, 2, skillSlot - 8);

        // DASH
        int sx = x + potionsWidth + separator;
        int sy = bottom - skillSlot;
        double dashRatio = (double) player.getDashCooldown() / (Player.DASH_COOLDOWN + 10);
        drawSlot(g, sx, sy, skillSlot, player.isDashing());
        drawDashIcon(g, sx + 9, sy + 9, skillSlot - 18);
        drawCooldown(g, sx, sy, skillSlot, dashRatio, -1);
        drawKeyLabel(g, "SPACE", sx, sy);
        drawCost(g, Player.DASH_STAMINA, sx, sy, skillSlot, player);

        // สกิล Q / E
        String[] keys = { "Q", "E" };
        for (int i = 0; i < Player.SKILL_SLOTS; i++) {
            int slotX = sx + (i + 1) * (skillSlot + gap);
            Skill skill = player.getSkill(i);
            if (skill == null) {
                drawLockedSlot(g, slotX, sy, skillSlot);
                drawKeyLabel(g, keys[i], slotX, sy);
                continue;
            }
            drawSlot(g, slotX, sy, skillSlot, skill.isActive());
            skill.drawIcon(g, slotX + 9, sy + 9, skillSlot - 18);
            drawCooldown(g, slotX, sy, skillSlot, skill.getCooldownRatio(), skill.getCooldownTimer());
            drawKeyLabel(g, skill.getKeyLabel(), slotX, sy);
            drawCost(g, skill.getStaminaCost(), slotX, sy, skillSlot, player);
        }
    }

    private void drawSlot(Graphics g, int x, int y, int size, boolean active) {
        g.setColor(new Color(30, 40, 66, 230));
        g.fillRoundRect(x, y, size, size, 12, 12);
        g.setColor(active ? ICE : new Color(120, 150, 200, 120));
        g.drawRoundRect(x, y, size, size, 12, 12);
        if (active) {
            g.drawRoundRect(x + 1, y + 1, size - 2, size - 2, 10, 10);
        }
    }

    private void drawLockedSlot(Graphics g, int x, int y, int size) {
        g.setColor(new Color(20, 26, 44, 220));
        g.fillRoundRect(x, y, size, size, 12, 12);
        g.setColor(new Color(90, 110, 150, 120));
        g.drawRoundRect(x, y, size, size, 12, 12);

        // แม่กุญแจ = ยังไม่มีสกิล (หาได้จากหีบ)
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.setColor(new Color(110, 125, 160));
        g.drawArc(cx - 7, cy - 13, 14, 16, 0, 180);
        g.drawArc(cx - 6, cy - 12, 12, 14, 0, 180);
        g.fillRoundRect(cx - 10, cy - 5, 20, 15, 4, 4);
        g.setColor(new Color(20, 26, 44));
        g.fillOval(cx - 2, cy, 4, 5);
    }

    /** เงาคูลดาวน์หมุนแบบนาฬิกา + ตัวเลขวินาทีที่เหลือ */
    private void drawCooldown(Graphics g, int x, int y, int size, double ratio, int framesLeft) {
        if (ratio <= 0) {
            return;
        }
        // วาดเฉพาะในกรอบช่อง: g.create(x, y, w, h) = Graphics ใหม่ที่ถูกตัดขอบไว้แค่ในช่องนี้
        Graphics slot = g.create(x + 1, y + 1, size - 1, size - 1);
        slot.setColor(new Color(0, 0, 0, 160));
        slot.fillArc(-size / 2, -size / 2, size * 2, size * 2, 90, (int) (360 * ratio));
        slot.dispose();
        if (framesLeft > 0) {
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.setColor(Color.WHITE);
            drawCentered(g, String.valueOf((framesLeft + 59) / 60), x + size / 2, y + size / 2 + 6);
        }
    }

    private void drawKeyLabel(Graphics g, String key, int x, int y) {
        g.setFont(new Font("Arial", Font.BOLD, 9));
        int w = g.getFontMetrics().stringWidth(key) + 6;
        g.setColor(new Color(220, 230, 255));
        g.fillRoundRect(x - 3, y - 5, w, 13, 5, 5);
        g.setColor(new Color(20, 30, 60));
        g.drawString(key, x, y + 5);
    }

    /** ค่า Stamina ที่ใช้ (สีแดงถ้า Stamina ไม่พอ) */
    private void drawCost(Graphics g, int cost, int x, int y, int size, Player player) {
        g.setFont(new Font("Arial", Font.BOLD, 10));
        boolean enough = player.isGodMode() || player.getStamina() >= cost;
        g.setColor(enough ? STAMINA : new Color(255, 100, 90));
        drawRight(g, String.valueOf(cost), x + size - 4, y + size - 4);
    }

    private void drawDashIcon(Graphics g, int x, int y, int size) {
        g.setColor(ICE);
        for (int i = 0; i < 2; i++) {
            int ox = x + 4 + i * size / 3;
            int[] xs = { ox, ox + size / 3, ox + size * 2 / 3, ox + size / 3 };
            int[] ys = { y + 4, y + 4, y + size / 2, y + size / 2 };
            g.fillPolygon(xs, ys, 4);
            int[] ys2 = { y + size - 4, y + size - 4, y + size / 2, y + size / 2 };
            g.fillPolygon(xs, ys2, 4);
        }
    }

    // =========================
    // ป้าย DEBUG / GOD
    // =========================
    private void drawModeChips(Graphics g, Player player) {
        int x = 12;
        int y = 94;
        g.setFont(new Font("Arial", Font.BOLD, 11));
        if (gamePanel.isDebugMode()) {
            x = drawChip(g, "DEBUG", x, y, new Color(90, 200, 255)) + 6;
        }
        if (player.isGodMode()) {
            drawChip(g, "GOD MODE", x, y, new Color(255, 170, 60));
        }
    }

    private int drawChip(Graphics g, String text, int x, int y, Color color) {
        int w = g.getFontMetrics().stringWidth(text) + 16;
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g.fillRoundRect(x, y, w, 20, 10, 10);
        g.setColor(color);
        g.drawRoundRect(x, y, w, 20, 10, 10);
        g.drawString(text, x + 8, y + 14);
        return x + w;
    }

    // =========================
    // ข้อความแจ้ง (ได้สกิลใหม่) ใต้ป้ายด่าน
    // =========================
    private void drawToast(Graphics g) {
        int timer = gamePanel.getToastTimer();
        if (timer <= 0 || gamePanel.getToastTitle() == null) {
            return;
        }
        int alpha = Math.min(255, timer * 255 / 30);
        int width = 380;
        int x = (GamePanel.SCREEN_WIDTH - width) / 2;
        int y = 80;

        g.setColor(new Color(20, 40, 70, alpha * 210 / 255));
        g.fillRoundRect(x, y, width, 52, 16, 16);
        g.setColor(new Color(150, 220, 255, alpha));
        g.drawRoundRect(x, y, width, 52, 16, 16);
        g.setFont(new Font("Arial", Font.BOLD, 17));
        drawCentered(g, gamePanel.getToastTitle(), GamePanel.SCREEN_WIDTH / 2, y + 23);
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(220, 235, 255, alpha));
        drawCentered(g, gamePanel.getToastText(), GamePanel.SCREEN_WIDTH / 2, y + 42);
    }

    // =========================
    // ป้ายเริ่มด่าน
    // =========================
    private void drawLevelBanner(Graphics g) {
        int timer = gamePanel.getLevelBannerTimer();
        if (timer <= 0 || gamePanel.isGameOver() || gamePanel.isVictory()) {
            return;
        }
        int level = gamePanel.getCurrentLevelNumber();
        int alpha = Math.min(255, timer * 255 / 40);
        int centerY = GamePanel.SCREEN_HEIGHT / 2;

        g.setColor(new Color(8, 12, 26, alpha * 170 / 255));
        g.fillRect(0, centerY - 62, GamePanel.SCREEN_WIDTH, 100);
        g.setColor(new Color(150, 200, 255, alpha * 120 / 255));
        g.fillRect(0, centerY - 62, GamePanel.SCREEN_WIDTH, 2);
        g.fillRect(0, centerY + 36, GamePanel.SCREEN_WIDTH, 2);

        g.setFont(new Font("Arial", Font.BOLD, 46));
        g.setColor(new Color(255, 255, 255, alpha));
        drawCentered(g, "LEVEL " + level, GamePanel.SCREEN_WIDTH / 2, centerY);

        String info = LevelMaps.getMapSizeName(level) + " MAP   •   " + LevelMaps.getChestCount(level)
                + (LevelMaps.getChestCount(level) > 1 ? " CHESTS" : " CHEST");
        if (LevelMaps.hasBoss(level)) {
            info += "   •   BOSS!";
        }
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(new Color(200, 220, 255, alpha));
        drawCentered(g, info, GamePanel.SCREEN_WIDTH / 2, centerY + 26);
    }

    // =========================
    // จบเกม: การ์ดสรุปผล
    // =========================
    private void drawEndScreen(Graphics g, String title, Color color) {
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(0, 0, GamePanel.SCREEN_WIDTH, GamePanel.SCREEN_HEIGHT);

        int width = 420;
        int height = 310;
        int x = (GamePanel.SCREEN_WIDTH - width) / 2;
        int y = (GamePanel.SCREEN_HEIGHT - height) / 2;
        g.setColor(new Color(16, 24, 44, 240));
        g.fillRoundRect(x, y, width, height, 24, 24);
        g.setColor(color);
        g.drawRoundRect(x, y, width, height, 24, 24);
        g.drawRoundRect(x + 1, y + 1, width - 2, height - 2, 22, 22);

        g.setFont(new Font("Arial", Font.BOLD, 46));
        drawCentered(g, title, GamePanel.SCREEN_WIDTH / 2, y + 66);

        String[][] rows = {
                { "MODE", gamePanel.getDifficulty().getLabel() },
                { "LEVEL", gamePanel.getCurrentLevelNumber() + " / " + LevelMaps.MAX_LEVELS },
                { "TIME", gamePanel.formatTime(gamePanel.getPlayFrames()) },
                { "ENEMIES DEFEATED", String.valueOf(gamePanel.getEnemiesDefeated()) },
                { "CHESTS OPENED", String.valueOf(gamePanel.getChestsOpened()) } };
        g.setFont(new Font("Arial", Font.BOLD, 16));
        for (int i = 0; i < rows.length; i++) {
            int rowY = y + 112 + i * 28;
            g.setColor(TEXT_DIM);
            g.drawString(rows[i][0], x + 50, rowY);
            g.setColor(i == 0 ? gamePanel.getDifficulty().getColor() : TEXT);
            drawRight(g, rows[i][1], x + width - 50, rowY);
        }

        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(TEXT);
        drawCentered(g, "ENTER  Play again        ESC  Menu", GamePanel.SCREEN_WIDTH / 2, y + height - 24);
    }

    // =========================
    // ลูกศรบอกทาง (ขอบจอ)
    // =========================
    /**
     *   สีทอง      = ประตูทางออก (EXIT)
     *   สีแดง      = ศัตรู
     *   สีม่วง     = Boss (BOSS)
     *   สีเงิน-ฟ้า = หีบสมบัติที่ยังไม่เปิด (แสดงเมื่อมีกุญแจหีบ)
     */
    private void drawIndicators(Graphics g, Player player) {
        for (Enemy enemy : gamePanel.getEnemies()) {
            if (!enemy.isAlive()) {
                continue;
            }
            if (enemy instanceof Boss) {
                drawArrow(g, player, enemy.getCenterX(), enemy.getCenterY(), ARROW_BOSS, 16, "BOSS");
            } else {
                drawArrow(g, player, enemy.getCenterX(), enemy.getCenterY(), ARROW_ENEMY, 10, null);
            }
        }
        if (player.getChestKeys() > 0 || player.isGodMode()) {
            for (Item item : gamePanel.getItems()) {
                if (item instanceof Chest && !((Chest) item).isOpened()) {
                    Chest chest = (Chest) item;
                    drawArrow(g, player, chest.getCenterX(), chest.getCenterY(), ARROW_CHEST, 12, "CHEST");
                }
            }
        }
        // ประตูวาดทีหลังสุด ให้อยู่บนสุด
        Color doorColor = player.hasKey() ? ARROW_DOOR : new Color(200, 170, 60);
        drawArrow(g, player, gamePanel.getDoor().getCenterX(), gamePanel.getDoor().getCenterY(), doorColor, 18, "EXIT");
    }

    /** วาดลูกศรที่ขอบจอ ชี้จาก Player ไปยังจุด (worldX, worldY) ถ้าจุดนั้นอยู่นอกจอ */
    private void drawArrow(Graphics g, Player player, int worldX, int worldY, Color color, int size, String label) {
        Camera camera = gamePanel.getCamera();
        int width = GamePanel.SCREEN_WIDTH;
        int height = GamePanel.SCREEN_HEIGHT;
        int targetX = camera.screenX(worldX);
        int targetY = camera.screenY(worldY);
        int margin = 34;
        double minX = margin;
        double maxX = width - margin;
        double minY = TOP_RESERVED;
        double maxY = height - BOTTOM_RESERVED;
        if (targetX > minX && targetX < maxX && targetY > minY && targetY < maxY) {
            return;     // อยู่ในจอแล้ว ไม่ต้องชี้
        }

        // ทิศจาก Player ไปเป้าหมาย
        double originX = camera.screenX(player.getCenterX());
        double originY = camera.screenY(player.getCenterY());
        double dx = targetX - originX;
        double dy = targetY - originY;
        double length = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        dx /= length;
        dy /= length;

        // หาจุดที่เส้นทิศนี้ชนขอบกรอบ (เว้นขอบบน / ล่างไว้ให้ UI)
        double t = Double.MAX_VALUE;
        if (dx > 0.001) t = Math.min(t, (maxX - originX) / dx);
        if (dx < -0.001) t = Math.min(t, (minX - originX) / dx);
        if (dy > 0.001) t = Math.min(t, (maxY - originY) / dy);
        if (dy < -0.001) t = Math.min(t, (minY - originY) / dy);
        if (t == Double.MAX_VALUE || t < 0) {
            return;
        }
        int tipX = (int) (originX + dx * t);
        int tipY = (int) (originY + dy * t);

        // สามเหลี่ยม: ปลายแหลมชี้ไปทางเป้าหมาย
        int backX = (int) (tipX - dx * size * 1.6);
        int backY = (int) (tipY - dy * size * 1.6);
        int sideX = (int) (-dy * size);
        int sideY = (int) (dx * size);
        int[] xs = { tipX, backX + sideX, backX - sideX };
        int[] ys = { tipY, backY + sideY, backY - sideY };

        g.setColor(color);
        g.fillPolygon(xs, ys, 3);
        g.setColor(new Color(0, 0, 0, 200));
        g.drawPolygon(xs, ys, 3);

        if (label != null) {
            g.setFont(new Font("Arial", Font.BOLD, 11));
            int textWidth = g.getFontMetrics().stringWidth(label);
            int textX = (int) (backX - dx * 14) - textWidth / 2;
            int textY = (int) (backY - dy * 14) + 5;
            g.setColor(Color.BLACK);
            g.drawString(label, textX + 1, textY + 1);
            g.setColor(color);
            g.drawString(label, textX, textY);
        }
    }

    // =========================
    // Helpers
    // =========================
    private void drawPanel(Graphics g, int x, int y, int width, int height) {
        g.setColor(PANEL);
        g.fillRoundRect(x, y, width, height, 16, 16);
        g.setColor(PANEL_BORDER);
        g.drawRoundRect(x, y, width, height, 16, 16);
    }

    private void drawBar(Graphics g, int x, int y, int width, int height, double ratio, Color color, String text) {
        ratio = Math.max(0, Math.min(1, ratio));
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(x, y, width, height, height, height);
        if (ratio > 0) {
            g.setColor(color);
            g.fillRoundRect(x, y, Math.max(height, (int) (width * ratio)), height, height, height);
            g.setColor(new Color(255, 255, 255, 60));
            g.fillRoundRect(x + 2, y + 2, Math.max(height - 4, (int) (width * ratio) - 4), height / 3, 4, 4);
        }
        if (text != null) {
            g.setFont(new Font("Arial", Font.BOLD, 11));
            g.setColor(Color.WHITE);
            g.drawString(text, x + 8, y + height - 4);
        }
    }

    private void drawCentered(Graphics g, String text, int centerX, int y) {
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, y);
    }

    private void drawRight(Graphics g, String text, int rightX, int y) {
        g.drawString(text, rightX - g.getFontMetrics().stringWidth(text), y);
    }
}
