package world;

import java.awt.*;
import java.util.ArrayList;

import entity.enemy.Boss;
import entity.enemy.CombatStyle;
import entity.enemy.Enemy;
import entity.enemy.Skeleton;
import entity.enemy.Slime;
import item.Chest;
import main.GamePanel;

/**
 * ตั้งค่าแต่ละด่าน (1-10): สุ่มแผนที่เขาวงกต, วาง Player, สุ่มศัตรู, วาง Boss และประตู
 *
 * การสุ่มศัตรู (ทำงานบน Cell ของเขาวงกต):
 *   - หาระยะทางเดินจากจุดเริ่มไปทุก Cell (Breadth-First Search)
 *   - ศัตรูปกติเกิดเฉพาะ Cell ที่เดินจากจุดเริ่มอย่างน้อย 4 Cell
 *   - ศัตรูแต่ละตัวห่างกันอย่างน้อย 2 Cell (กระจายทั่วแผนที่ ไม่กองรวมกัน)
 *   - Boss เกิดในส่วนลึกของแผนที่ (ไกลกว่า 60% ของระยะไกลสุด) และเลือกในห้องโล่งก่อน
 *   - ไม่มีศัตรูเกิดหน้าประตู
 *
 * หีบสมบัติ: วางในทางตันก่อน (ต้องเดินอ้อมไปเปิด) ห่างจุดเริ่มอย่างน้อย 2 Cell
 * แล้วสุ่มศัตรูให้ "ถือกุญแจหีบ" เท่ากับจำนวนหีบ (ตายแล้วดรอปกุญแจหีบ)
 */
public class Level {

    private static final int MIN_START_DISTANCE = 4;    // Cell
    private static final int MIN_SPACING = 2;            // Cell

    private final GamePanel gamePanel;
    private final int levelNumber;
    private final Difficulty difficulty;
    private MapGenerator generator;
    private int[][] distanceFromStart;
    private final ArrayList<Point> usedCells = new ArrayList<Point>();

    // =========================
    // Constructor
    // =========================
    public Level(GamePanel gamePanel, int levelNumber, Difficulty difficulty) {
        this.gamePanel = gamePanel;
        this.levelNumber = levelNumber;
        this.difficulty = difficulty;
    }

    // =========================
    // Load Level
    // =========================
    public void load() {
        generator = new MapGenerator(
                LevelMaps.getCellCols(levelNumber),
                LevelMaps.getCellRows(levelNumber),
                LevelMaps.getRoomCount(levelNumber),
                LevelMaps.getLoopChance());

        gamePanel.setMap(new Map(gamePanel, generator.generate()));
        gamePanel.clearLevelObjects();
        distanceFromStart = generator.distancesFrom(generator.getStartCellCol(), generator.getStartCellRow());
        usedCells.clear();

        gamePanel.getPlayer().setCellPosition(generator.getStartCellCol(), generator.getStartCellRow());
        if (LevelMaps.hasBoss(levelNumber)) {
            spawnBoss();        // วาง Boss ก่อน จะได้ที่ลึก ๆ
        }
        spawnNormalEnemies();
        setupDoor();
        spawnChests();

        gamePanel.setEnemiesRemaining(gamePanel.getEnemies().size());
        gamePanel.getPlayer().setHasKey(false);   // กุญแจใช้ได้แค่ด่านเดียว

        System.out.println("Level " + levelNumber + " Loaded! (" + LevelMaps.getMapSizeName(levelNumber)
                + ", " + difficulty.getLabel() + ")");
    }

    private void setupDoor() {
        // ประตูอยู่แถวล่างของ Cell ที่เจาะทะลุกำแพงขอบล่าง
        int doorX = generator.getExitCellCol() * Map.CELL_SIZE;
        int doorY = generator.getExitCellRow() * Map.CELL_SIZE + Map.TILE_SIZE;
        gamePanel.setDoor(new Door(gamePanel, doorX, doorY));
    }

    // =========================
    // Spawn Enemies
    // =========================
    /** วาง Slime และ Skeleton ตามจำนวนในตาราง LevelMaps (+ เพิ่มตามระดับความยาก) */
    private void spawnNormalEnemies() {
        int extra = difficulty.getExtraEnemies();
        int slimes = LevelMaps.getSlimeCount(levelNumber) + extra;
        int skeletons = LevelMaps.getSkeletonCount(levelNumber) + (levelNumber >= 2 ? extra : 0);
        int rangedSlimes = LevelMaps.getRangedSlimeCount(levelNumber);
        int rangedSkeletons = LevelMaps.getRangedSkeletonCount(levelNumber);

        ArrayList<Point> candidates = findCandidateCells(MIN_START_DISTANCE);
        int spawned = 0;

        for (int i = 0; i < slimes + skeletons; i++) {
            // สร้างศัตรู (Polymorphism: เก็บทั้งคู่ในตัวแปรชนิด Enemy)
            Enemy enemy;
            if (i < slimes) {
                CombatStyle style = i < rangedSlimes ? CombatStyle.RANGED : CombatStyle.MELEE;
                enemy = new Slime(gamePanel, style);
                enemy.addSpeed(LevelMaps.getSlimeSpeed(levelNumber) - 1);   // ด่านหลัง Slime เร็วขึ้น
            } else {
                int skeletonIndex = i - slimes;
                CombatStyle style = skeletonIndex < rangedSkeletons ? CombatStyle.RANGED : CombatStyle.MELEE;
                enemy = new Skeleton(gamePanel, style);
            }
            enemy.applyDifficulty(difficulty);

            Point cell = pickSpreadCell(candidates);
            if (cell == null) {
                break;      // ที่เต็มแล้ว
            }
            enemy.placeAtCell(cell.x, cell.y);
            gamePanel.getEnemies().add(enemy);
            spawned++;
        }

        System.out.println("Normal Enemies Spawned: " + spawned);
    }

    // =========================
    // Chests
    // =========================
    private void spawnChests() {
        int count = LevelMaps.getChestCount(levelNumber);

        // ทางตัน (มีทางออกทางเดียว) ก่อน ถ้าไม่พอใช้ Cell ไหนก็ได้
        ArrayList<Point> all = findCandidateCells(2);
        ArrayList<Point> deadEnds = new ArrayList<Point>();
        for (Point p : all) {
            if (countOpenNeighbors(p.x, p.y) == 1) {
                deadEnds.add(p);
            }
        }

        int placed = 0;
        for (int i = 0; i < count; i++) {
            Point cell = pickSpreadCell(deadEnds);
            if (cell == null) {
                cell = pickSpreadCell(all);
            }
            if (cell == null) {
                break;
            }
            deadEnds.remove(cell);
            all.remove(cell);
            gamePanel.getItems().add(new Chest(gamePanel, Map.cellCenter(cell.x), Map.cellCenter(cell.y)));
            placed++;
        }

        // สุ่มศัตรูที่ถือกุญแจหีบ (ไม่ซ้ำตัว) จำนวนเท่ากับหีบที่วาง
        ArrayList<Enemy> pool = new ArrayList<Enemy>(gamePanel.getEnemies());
        for (int i = 0; i < placed && !pool.isEmpty(); i++) {
            Enemy carrier = pool.remove((int) (Math.random() * pool.size()));
            carrier.setCarriesChestKey(true);
        }
        System.out.println("Chests Placed: " + placed);
    }

    private int countOpenNeighbors(int col, int row) {
        int open = 0;
        int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
        for (int[] d : directions) {
            int c = col + d[0];
            int r = row + d[1];
            if (r >= 0 && c >= 0 && r < distanceFromStart.length && c < distanceFromStart[0].length
                    && !generator.isCellWall(c, r)) {
                open++;
            }
        }
        return open;
    }

    private void spawnBoss() {
        int bossHp = (int) Math.round(LevelMaps.getBossHp(levelNumber) * difficulty.getBossHpMultiplier());
        Boss boss = new Boss(gamePanel, bossHp, LevelMaps.getBossSpeed(levelNumber));
        boss.setTeleportDelay(difficulty.getBossTeleportDelay());

        // Cell ที่ลึก (ไกลกว่า 60% ของระยะไกลสุด) และเลือกในห้องโล่งก่อน
        int farthest = 0;
        for (int[] row : distanceFromStart) {
            for (int d : row) {
                farthest = Math.max(farthest, d);
            }
        }
        ArrayList<Point> deep = findCandidateCells(Math.max(MIN_START_DISTANCE, farthest * 6 / 10));
        ArrayList<Point> deepRooms = new ArrayList<Point>();
        for (Point p : deep) {
            if (generator.getRoomCells().contains(p)) {
                deepRooms.add(p);
            }
        }

        Point cell = pickSpreadCell(deepRooms.isEmpty() ? deep : deepRooms);
        if (cell == null) {
            cell = pickSpreadCell(findCandidateCells(MIN_START_DISTANCE));
        }
        if (cell == null) {
            System.out.println("WARNING: Could not find Boss spawn!");
            return;
        }
        boss.placeAtCell(cell.x, cell.y);
        gamePanel.getEnemies().add(boss);
        System.out.println("Boss Spawned at Cell: " + cell.x + ", " + cell.y);
    }

    /** Cell พื้นที่เดินจากจุดเริ่มได้ อย่างน้อย minDistance Cell และไม่ใช่หน้าประตู */
    private ArrayList<Point> findCandidateCells(int minDistance) {
        ArrayList<Point> result = new ArrayList<Point>();
        int exitCol = generator.getExitCellCol();
        int exitRow = generator.getExitCellRow();

        for (int r = 0; r < distanceFromStart.length; r++) {
            for (int c = 0; c < distanceFromStart[0].length; c++) {
                int d = distanceFromStart[r][c];
                boolean nearDoor = Math.abs(c - exitCol) + Math.abs(r - exitRow) <= 1;
                if (d >= minDistance && !nearDoor) {
                    result.add(new Point(c, r));
                }
            }
        }
        return result;
    }

    /** สุ่ม Cell ที่ห่างจากศัตรูตัวอื่นอย่างน้อย MIN_SPACING (ถ้าไม่มี ยอมให้ใกล้กันได้แต่ไม่ซ้ำ Cell) */
    private Point pickSpreadCell(ArrayList<Point> candidates) {
        for (int spacing = MIN_SPACING; spacing >= 1; spacing--) {
            ArrayList<Point> options = new ArrayList<Point>();
            for (Point p : candidates) {
                if (isFarFromOthers(p, spacing)) {
                    options.add(p);
                }
            }
            if (!options.isEmpty()) {
                Point chosen = options.get((int) (Math.random() * options.size()));
                usedCells.add(chosen);
                return chosen;
            }
        }
        return null;
    }

    private boolean isFarFromOthers(Point cell, int spacing) {
        for (Point used : usedCells) {
            if (Math.abs(used.x - cell.x) + Math.abs(used.y - cell.y) < spacing) {
                return false;
            }
        }
        return true;
    }

    public int getLevelNumber() {
        return levelNumber;
    }
}
