package world;

import java.awt.*;

import main.GamePanel;

/**
 * แผนที่ของด่าน เก็บเป็นตาราง int[][]
 * 0 = พื้น (FLOOR), 1 = กำแพง (WALL)
 *
 * ขนาด:
 *   ช่อง (Tile) = 64x64 px  -> ใช้วาดภาพ และตรวจชนกำแพง
 *   Cell        = 128x128 px (2x2 ช่อง) -> ใช้ออกแบบเขาวงกตและให้ศัตรูหาทาง
 *   ทุก Entity เล็กกว่า 1 Cell จึงเดินผ่านทุกทางได้
 *
 * วาดด้วยภาพจาก Tileset (ดู TileSet.java) และวาดเฉพาะส่วนที่อยู่ในจอ
 */
public class Map {

    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int TILE_SIZE = 64;
    public static final int CELL_SIZE = TILE_SIZE * MapGenerator.CELL_TILES;    // 128

    private static final Color STONE_EDGE = new Color(150, 160, 180);
    private static final Color WALL_SHADOW = new Color(0, 0, 0, 70);

    private final GamePanel gamePanel;
    private final int[][] map;
    private final int[][] sprite;       // เลขภาพ Tileset ของแต่ละช่อง (คำนวณครั้งเดียวตอนสร้าง)

    // =========================
    // Constructor
    // =========================
    public Map(GamePanel gamePanel, int[][] mapData) {
        this.gamePanel = gamePanel;
        this.map = mapData;
        this.sprite = chooseSprites();
    }

    /**
     * เลือกภาพให้แต่ละช่อง:
     *   กำแพงที่มีพื้นอยู่ข้างล่าง = หน้ากำแพงอิฐ (บางช่องมีหน้าต่าง / ธง)
     *   กำแพงอื่น ๆ              = ด้านบนกำแพง (มืด)
     *   พื้น                    = พื้นทราย สลับลายจุด / หินแตก
     */
    private int[][] chooseSprites() {
        int[][] result = new int[map.length][map[0].length];
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[0].length; col++) {
                int random = Math.abs((row * 7919 + col * 104729) * 31 % 100);   // สุ่มแบบคงที่ตามตำแหน่ง
                if (map[row][col] == WALL) {
                    if (!isWall(row + 1, col)) {
                        result[row][col] = random < 6 ? TileSet.WALL_WINDOW
                                : random < 10 ? TileSet.WALL_BANNER : TileSet.WALL_FACE;
                    } else {
                        result[row][col] = TileSet.WALL_TOP;
                    }
                } else {
                    result[row][col] = random < 70 ? TileSet.FLOOR
                            : random < 85 ? TileSet.FLOOR_DOTS
                            : random < 94 ? TileSet.FLOOR_CRACK : TileSet.FLOOR_RUBBLE;
                }
            }
        }
        return result;
    }

    // =========================
    // Draw
    // =========================
    public void draw(Graphics g) {
        // วาดเฉพาะช่องที่อยู่ในจอ (แผนที่ใหญ่ก็ไม่ช้า)
        int firstCol = Math.max(0, toTile(gamePanel.getCamera().getX()) - 1);
        int firstRow = Math.max(0, toTile(gamePanel.getCamera().getY()) - 1);
        int lastCol = Math.min(map[0].length - 1, firstCol + gamePanel.getWidth() / TILE_SIZE + 3);
        int lastRow = Math.min(map.length - 1, firstRow + gamePanel.getHeight() / TILE_SIZE + 3);
        boolean useTiles = TileSet.isLoaded();

        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                int screenX = gamePanel.getCamera().screenX(col * TILE_SIZE);
                int screenY = gamePanel.getCamera().screenY(row * TILE_SIZE);

                if (useTiles) {
                    TileSet.draw(g, sprite[row][col], screenX, screenY, TILE_SIZE);
                } else {
                    g.setColor(map[row][col] == WALL ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                    g.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                }

                if (map[row][col] == WALL) {
                    drawWallEdges(g, row, col, screenX, screenY);
                } else if (isWall(row - 1, col)) {
                    // เงาของกำแพงที่ตกลงบนพื้น
                    g.setColor(WALL_SHADOW);
                    g.fillRect(screenX, screenY, TILE_SIZE, 12);
                }
            }
        }
    }

    /** ขอบหินรอบด้านบนกำแพง ให้เห็นทางเดินชัด */
    private void drawWallEdges(Graphics g, int row, int col, int screenX, int screenY) {
        if (sprite[row][col] != TileSet.WALL_TOP) {
            return;
        }
        g.setColor(STONE_EDGE);
        if (!isWall(row - 1, col)) {
            g.fillRect(screenX, screenY, TILE_SIZE, 5);
        }
        if (!isWall(row, col - 1)) {
            g.fillRect(screenX, screenY, 5, TILE_SIZE);
        }
        if (!isWall(row, col + 1)) {
            g.fillRect(screenX + TILE_SIZE - 5, screenY, 5, TILE_SIZE);
        }
    }

    // =========================
    // Tile Helpers
    // =========================
    /** ช่อง (row, col) เป็นกำแพงหรือไม่ — นอกแผนที่ถือว่าเป็นกำแพง */
    public boolean isWall(int row, int col) {
        if (row < 0 || row >= map.length || col < 0 || col >= map[0].length) {
            return true;
        }
        return map[row][col] == WALL;
    }

    /** แปลงพิกัด Pixel -> เลขช่อง (ปัดลงเสมอ แม้ค่าติดลบ) */
    public static int toTile(int pixel) {
        return Math.floorDiv(pixel, TILE_SIZE);
    }

    // =========================
    // Cell Helpers (1 Cell = 2x2 ช่อง = 128 px)
    // =========================
    public static int toCell(int pixel) {
        return Math.floorDiv(pixel, CELL_SIZE);
    }

    /** จุดกลาง (Pixel) ของ Cell */
    public static int cellCenter(int index) {
        return index * CELL_SIZE + CELL_SIZE / 2;
    }

    /** Cell เป็นกำแพงหรือไม่ (นอกแผนที่ = กำแพง) */
    public boolean isCellWall(int cellCol, int cellRow) {
        return isWall(cellRow * MapGenerator.CELL_TILES, cellCol * MapGenerator.CELL_TILES);
    }

    public int getCellCols() { return map[0].length / MapGenerator.CELL_TILES; }
    public int getCellRows() { return map.length / MapGenerator.CELL_TILES; }

    // =========================
    // Block Helpers (สำหรับ Object ที่ใหญ่กว่า 1 ช่อง)
    // =========================
    /** Object ขนาด sizePx ต้องใช้กล่องกี่ช่อง เช่น 64px = 1 ช่อง, 96px = 2 ช่อง */
    public static int blocksFor(int sizePx) {
        return (sizePx + TILE_SIZE - 1) / TILE_SIZE;
    }

    /** จุดกลาง (Pixel) ของกล่อง blockSize ช่อง ที่เริ่มที่ช่อง index */
    public static int blockCenter(int index, int blockSize) {
        return index * TILE_SIZE + blockSize * TILE_SIZE / 2;
    }

    /** ย้อนกลับ: Object ที่มีจุดกลางที่ centerPx อยู่ในกล่องที่เริ่มช่องไหน */
    public static int blockIndex(int centerPx, int blockSize) {
        return Math.floorDiv(centerPx - blockSize * TILE_SIZE / 2 + TILE_SIZE / 2, TILE_SIZE);
    }

    // =========================
    // Collision
    // =========================
    /** ตรวจว่ากล่อง (x, y, w, h) ทับช่องกำแพงหรือออกนอกแผนที่หรือไม่ */
    public boolean isWallCollision(int x, int y, int width, int height) {
        // ตรวจเฉพาะช่องที่กล่องทับอยู่ (ไม่ต้องวนทั้งแผนที่)
        int firstCol = toTile(x);
        int lastCol = toTile(x + width - 1);
        int firstRow = toTile(y);
        int lastRow = toTile(y + height - 1);

        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                if (isWall(row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isWallCollision(Rectangle box) {
        return isWallCollision(box.x, box.y, box.width, box.height);
    }

    // =========================
    // Getters
    // =========================
    public int[][] getMap() { return map; }
    public int getTileSize() { return TILE_SIZE; }
    public int getRows() { return map.length; }
    public int getCols() { return map[0].length; }
    public int getWorldWidth() { return map[0].length * TILE_SIZE; }
    public int getWorldHeight() { return map.length * TILE_SIZE; }
}
