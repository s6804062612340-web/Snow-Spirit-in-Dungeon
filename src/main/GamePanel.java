package main;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

import ai.FlowField;
import entity.DamageText;
import entity.Player;
import entity.Projectile;
import effect.EffectManager;
import entity.enemy.Boss;
import entity.enemy.Enemy;
import entity.enemy.EnemyProjectile;
import item.ChestKey;
import item.HealthPotion;
import item.Item;
import item.Key;
import menu.MenuButton;
import sound.Sound;
import world.Difficulty;
import world.Door;
import world.Level;
import world.LevelMaps;
import world.Map;

/**
 * หน้าจอเล่นเกม + Game Loop
 * ทำหน้าที่เก็บ Object ทั้งหมดในเกม แล้วสั่ง update() / draw() ทุกเฟรม
 *
 * ปุ่ม BMenu เป็น public เพื่อให้ Main (JFrame) ดักจับแล้วสลับกลับไปหน้าเมนู
 * (แบบเดียวกับปุ่ม BExithome ของ playstate1 ในเอกสาร)
 */
public class GamePanel extends JPanel implements Runnable {

    // =========================
    // Screen Settings
    // =========================
    public static final int SCREEN_WIDTH = 768;
    public static final int SCREEN_HEIGHT = 640;
    private static final int FPS = 60;

    // =========================
    // Buttons
    // =========================
    public JButton BMenu = new MenuButton("MENU", new Color(150, 200, 255));

    // =========================
    // Game Objects (Composition)
    // =========================
    private Thread gameThread;
    private final Camera camera;
    private Player player;
    private Level currentLevel;
    private int currentLevelNumber = 1;

    private Map map;
    private Door door;
    private ArrayList<Enemy> enemies = new ArrayList<Enemy>();
    private ArrayList<Item> items = new ArrayList<Item>();
    private final ArrayList<Projectile> projectiles = new ArrayList<Projectile>();
    private final ArrayList<DamageText> damageTexts = new ArrayList<DamageText>();
    private final ArrayList<EnemyProjectile> enemyProjectiles = new ArrayList<EnemyProjectile>();
    private EffectManager effects;
    private final Hud hud;                                         // UI บนจอ (Composition)
    private final ArrayList<Item> pendingItems = new ArrayList<Item>();   // ของที่เกิดระหว่าง update (เช่น ของจากหีบ)

    /** แผนที่ระยะทางไปหา Player ที่ศัตรูทุกตัวใช้ร่วมกัน (สร้างใหม่ทุกด่าน) */
    private FlowField flowField;

    // =========================
    // Game State
    // =========================
    private boolean playing = false;     // false = อยู่หน้าเมนู ไม่ต้อง update
    private int enemiesRemaining;
    private int playFrames = 0;          // เวลาที่เล่นไปแล้ว (เฟรม, 60 เฟรม = 1 วินาที)
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean debugMode = false;
    private boolean godMode = false;
    private Difficulty difficulty = Difficulty.EASY;
    private int levelBannerTimer = 0;    // ป้าย "LEVEL N" ตอนเริ่มด่าน (เฟรม)
    private static final int LEVEL_BANNER_FRAMES = 120;
    private int enemiesDefeated = 0;     // สถิติแสดงตอนจบเกม
    private int chestsOpened = 0;
    private String toastTitle = null;    // ข้อความแจ้งกลางจอ (ได้สกิลใหม่)
    private String toastText = null;
    private int toastTimer = 0;

    // =========================
    // Constructor
    // =========================
    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        setLayout(null);

        // ปุ่มกลับเมนู (ไม่รับ Focus เพื่อให้คีย์บอร์ดยังควบคุมตัวละครได้)
        BMenu.setBounds(SCREEN_WIDTH - 162, 12, 150, 32);
        BMenu.setFocusable(false);
        add(BMenu);

        // ENTER = เริ่มใหม่ตอน Game Over / ESC = กลับเมนู
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && (gameOver || victory)) {
                    newGame(godMode, debugMode, difficulty);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    BMenu.doClick();
                }
            }
        });

        camera = new Camera(this);
        effects = new EffectManager(camera);
        hud = new Hud(this);
        player = new Player(this);
        loadLevel(1);
    }

    // =========================
    // Game Flow
    // =========================
    /** เริ่ม Thread ของ Game Loop (เรียกครั้งเดียวตอนเปิดโปรแกรม) */
    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    /** เริ่มเกมใหม่ตั้งแต่ด่าน 1 พร้อมค่าจากเมนู Setting */
    public void newGame(boolean godMode, boolean debugMode, Difficulty difficulty) {
        this.godMode = godMode;
        this.debugMode = debugMode;
        this.difficulty = difficulty;

        removeKeyListener(player);
        removeMouseListener(player);
        removeMouseMotionListener(player);

        player = new Player(this);
        player.setMaxHp(difficulty.getPlayerMaxHp());
        player.setGodMode(godMode);
        addKeyListener(player);
        addMouseListener(player);
        addMouseMotionListener(player);

        loadLevel(1);

        effects.clear();
        playFrames = 0;
        enemiesDefeated = 0;
        chestsOpened = 0;
        toastTimer = 0;
        gameOver = false;
        victory = false;
        playing = true;
        requestFocusInWindow();
    }

    /** หยุดเกม (ตอนกลับไปหน้าเมนู) */
    public void stopGame() {
        playing = false;
    }

    /** ถูกเรียกจาก Door เมื่อผู้เล่นเปิดประตูสำเร็จ */
    public void completeLevel() {
        if (currentLevelNumber < LevelMaps.MAX_LEVELS) {
            loadLevel(currentLevelNumber + 1);
            player.heal(difficulty.getLevelClearHeal());     // รางวัลผ่านด่าน: ฟื้น HP
            effects.flash(new Color(255, 255, 255, 160), 25);
            effects.sparkle(player.getCenterX(), player.getCenterY(), 20, new Color(90, 255, 120));
            System.out.println("Moving to Level " + currentLevelNumber);
        } else {
            victory = true;
            Sound.VICTORY.play();
            System.out.println("All Levels Completed!");
        }
    }

    private void loadLevel(int levelNumber) {
        currentLevelNumber = levelNumber;
        currentLevel = new Level(this, levelNumber, difficulty);
        currentLevel.load();
        buildFlowField();
        levelBannerTimer = LEVEL_BANNER_FRAMES;
    }

    /** สร้าง Flow Field จากตาราง Cell ของแผนที่ใหม่ */
    private void buildFlowField() {
        boolean[][] blocked = new boolean[map.getCellRows()][map.getCellCols()];
        for (int r = 0; r < blocked.length; r++) {
            for (int c = 0; c < blocked[0].length; c++) {
                blocked[r][c] = map.isCellWall(c, r);
            }
        }
        flowField = new FlowField(blocked);
        updateFlowField();
    }

    /** คำนวณระยะทางใหม่เมื่อ Player เดินเข้า Cell ใหม่ (ถ้า Cell เดิมจะไม่คำนวณซ้ำ) */
    private void updateFlowField() {
        flowField.setTarget(Map.toCell(player.getCenterX()), Map.toCell(player.getCenterY()));
    }

    // =========================
    // Game Loop (60 FPS)
    // =========================
    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while (gameThread != null) {
            if (playing) {
                update();
                repaint();
            }

            try {
                double remainingMs = (nextDrawTime - System.nanoTime()) / 1000000;
                if (remainingMs < 0) {
                    remainingMs = 0;
                }
                Thread.sleep((long) remainingMs);
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // =========================
    // Update
    // =========================
    public void update() {
        if (gameOver || victory) {
            return;
        }

        playFrames++;
        player.update();
        updateFlowField();
        camera.update();
        door.update();

        // Polymorphism: Slime / Skeleton / Boss ถูกเรียกผ่านชนิด Enemy
        for (Enemy enemy : enemies) {
            enemy.update();
        }
        for (Item item : items) {
            item.update();
        }
        items.addAll(pendingItems);     // ของที่เพิ่งเกิด (เพิ่มหลังวนลูปเสร็จ กันแก้ List ระหว่างวน)
        pendingItems.clear();

        updateProjectiles();
        updateEnemyProjectiles();
        removeDeadEnemies();
        updateDamageTexts();
        effects.update();
        if (levelBannerTimer > 0) {
            levelBannerTimer--;
        }
        if (toastTimer > 0) {
            toastTimer--;
        }
    }

    /** กระสุนศัตรู: โดนกำแพง = แตก, โดน Player = ดาเมจ (Player Dash / อมตะอยู่ จะทะลุผ่าน) */
    private void updateEnemyProjectiles() {
        for (int i = enemyProjectiles.size() - 1; i >= 0; i--) {
            EnemyProjectile shot = enemyProjectiles.get(i);
            shot.update();

            boolean hitWall = map.isWallCollision(shot.getHitbox());
            boolean hitPlayer = shot.getHitbox().intersects(player.getHitbox())
                    && !player.isDashing();

            if (hitPlayer) {
                player.takeDamage(shot.getDamage());
            }
            if (hitWall || hitPlayer) {
                effects.burst(shot.getCenterX(), shot.getCenterY(), 8, shot.getColor(), 3, 6, 12);
                enemyProjectiles.remove(i);
            }
        }
    }

    private void updateDamageTexts() {
        for (int i = damageTexts.size() - 1; i >= 0; i--) {
            damageTexts.get(i).update();
            if (damageTexts.get(i).isFinished()) {
                damageTexts.remove(i);
            }
        }
    }

    private void updateProjectiles() {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update();
            if (playFrames % 2 == 0) {
                effects.trail((int) projectile.getX() + 6, (int) projectile.getY() + 6, new Color(120, 230, 255), 9);
            }

            boolean hitWall = map.isWallCollision(projectile.getHitbox());
            boolean hitEnemy = false;

            // ศัตรูที่กำลังอมตะ (กระพริบ) กระสุนจะทะลุผ่านไป
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && !enemy.isInvincible()
                        && projectile.getHitbox().intersects(enemy.getHitbox())) {
                    enemy.takeDamage(projectile.getDamage());
                    hitEnemy = true;
                    break;
                }
            }

            if (hitWall) {
                effects.burst((int) projectile.getX() + 6, (int) projectile.getY() + 6, 6,
                        new Color(180, 240, 255), 3, 5, 10);
            }
            if (hitEnemy || hitWall) {
                projectiles.remove(i);
            }
        }
    }

    private void removeDeadEnemies() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.isAlive()) {
                continue;
            }

            // จุดที่ของตก: เลื่อนไปจุดที่ Player เดินไปถึงได้แน่นอน
            Point drop = findDropPoint(enemy.getCenterX(), enemy.getCenterY());
            int deadX = drop.x;
            int deadY = drop.y;

            dropLoot(enemy, deadX, deadY);
            enemies.remove(i);
            enemiesRemaining--;
            enemiesDefeated++;
            System.out.println("Enemies Remaining: " + enemiesRemaining);

            // ศัตรูตัวสุดท้ายตาย -> ดรอปกุญแจ
            if (enemiesRemaining == 0) {
                items.add(new Key(this, deadX, deadY));
                System.out.println("Key Dropped!");
            }
        }
    }

    /**
     * ของที่ศัตรูดรอป
     *   - ศัตรูที่ถือกุญแจหีบ (ไอคอนกุญแจเหนือหัว) -> กุญแจหีบ
     *   - Boss -> ยาแดงเสมอ / ศัตรูปกติ -> ยาแดงตามโอกาสของระดับความยาก
     * วางเยื้องกันคนละจุด ไม่ให้ทับกับกุญแจประตูที่อาจตกตรงกลาง (ยังอยู่ใน Cell เดียวกัน = พื้นแน่นอน)
     */
    private void dropLoot(Enemy enemy, int centerX, int centerY) {
        if (enemy.carriesChestKey()) {
            items.add(new ChestKey(this, centerX - 34, centerY + 22));
        }
        if (enemy instanceof Boss || Math.random() < difficulty.getPotionDropChance()) {
            items.add(new HealthPotion(this, centerX + 30, centerY + 22));
        }
    }

    /**
     * จุดที่ของตก = กลาง Cell ที่ศัตรูตาย (Cell เป็นพื้นทั้งช่อง ใหญ่กว่าตัว Player จึงเก็บได้เสมอ)
     * ถ้าตายคร่อมกำแพง ให้ใช้ Cell พื้นที่ใกล้ที่สุด
     */
    private Point findDropPoint(int x, int y) {
        int baseCol = Map.toCell(x);
        int baseRow = Map.toCell(y);
        Point best = new Point(x, y);
        double bestDistance = Double.MAX_VALUE;

        for (int row = baseRow - 1; row <= baseRow + 1; row++) {
            for (int col = baseCol - 1; col <= baseCol + 1; col++) {
                if (map.isCellWall(col, row)) {
                    continue;
                }
                double distance = Math.hypot(Map.cellCenter(col) - x, Map.cellCenter(row) - y);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new Point(Map.cellCenter(col), Map.cellCenter(row));
                }
            }
        }
        return best;
    }

    // =========================
    // Draw
    // =========================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // เปิด Anti-aliasing ให้รูปทรง / ตัวอักษรเรียบ (RenderingHints อยู่ใน java.awt.*)
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        map.draw(g);
        door.draw(g);
        for (Item item : items) {
            item.draw(g);       // หีบ / ยา / กุญแจ อยู่บนพื้น ใต้ตัวศัตรู
        }
        for (Enemy enemy : enemies) {
            enemy.draw(g);
        }
        player.draw(g);
        for (Projectile projectile : projectiles) {
            projectile.draw(g);
        }
        for (EnemyProjectile shot : enemyProjectiles) {
            shot.draw(g);
        }
        effects.draw(g);
        for (DamageText text : damageTexts) {
            text.draw(g);
        }

        effects.drawScreenFlash(g, getWidth(), getHeight());
        hud.draw(g);
    }

    /** แปลงจำนวนเฟรมเป็น "นาที:วินาที" เช่น 3:07 */
    public String formatTime(int frames) {
        int totalSeconds = frames / FPS;
        int seconds = totalSeconds % 60;
        return (totalSeconds / 60) + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    // =========================
    // ของจากหีบ / ข้อความแจ้ง / สถิติ
    // =========================
    /** เพิ่มไอเทมระหว่างที่ไอเทมอื่นกำลัง update (เช่น หีบปล่อยของรางวัล) */
    public void spawnItem(Item item) {
        pendingItems.add(item);
    }

    public void onChestOpened() {
        chestsOpened++;
    }

    /** ข้อความแจ้งกลางจอ 3 วินาที */
    public void showToast(String title, String text) {
        toastTitle = title;
        toastText = text;
        toastTimer = 180;
    }

    // =========================
    // Getters / Setters (Encapsulation)
    // =========================
    public Player getPlayer() { return player; }
    public Camera getCamera() { return camera; }
    public Map getMap() { return map; }
    public ArrayList<Enemy> getEnemies() { return enemies; }
    public ArrayList<Item> getItems() { return items; }
    public boolean isDebugMode() { return debugMode; }
    public boolean isGameOver() { return gameOver; }
    public boolean isVictory() { return victory; }
    public int getPlayFrames() { return playFrames; }
    public int getCurrentLevelNumber() { return currentLevelNumber; }
    public int getEnemiesRemaining() { return enemiesRemaining; }
    public int getEnemiesDefeated() { return enemiesDefeated; }
    public int getChestsOpened() { return chestsOpened; }
    public int getLevelBannerTimer() { return levelBannerTimer; }
    public String getToastTitle() { return toastTitle; }
    public String getToastText() { return toastText; }
    public int getToastTimer() { return toastTimer; }

    public void setGameOver(boolean gameOver) {
        if (gameOver && !this.gameOver) {
            Sound.GAME_OVER.play();
        }
        this.gameOver = gameOver;
    }

    public void addProjectile(Projectile projectile) { projectiles.add(projectile); }
    public void addDamageText(DamageText text) { damageTexts.add(text); }
    public void addEnemyProjectile(EnemyProjectile shot) { enemyProjectiles.add(shot); }
    public EffectManager getEffects() { return effects; }
    public FlowField getFlowField() { return flowField; }
    public Difficulty getDifficulty() { return difficulty; }
    public Door getDoor() { return door; }
    public ArrayList<EnemyProjectile> getEnemyProjectiles() { return enemyProjectiles; }

    // ----- ใช้โดย Level ตอนโหลดด่าน -----
    public void setMap(Map map) { this.map = map; }
    public void setDoor(Door door) { this.door = door; }
    public void setEnemiesRemaining(int count) { this.enemiesRemaining = count; }

    /** ล้างศัตรู / ไอเทม / กระสุน ของด่านเก่า */
    public void clearLevelObjects() {
        enemies = new ArrayList<Enemy>();
        items = new ArrayList<Item>();
        projectiles.clear();
        damageTexts.clear();
        enemyProjectiles.clear();
        pendingItems.clear();
        effects.clear();
    }
}
