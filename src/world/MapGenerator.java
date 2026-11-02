package world;

import java.awt.*;
import java.util.ArrayList;

/**
 * สุ่มสร้างแผนที่แบบ "เขาวงกต" (ใช้ทุกด่าน)
 *
 * แผนที่ออกแบบบนตาราง "Cell" ขนาด 128x128 px (= 2x2 ช่องของ Map)
 * ทุก Entity ตัวเล็กกว่า 1 Cell (Player 96, Boss 112, Slime/Skeleton 64)
 * จึงเดินผ่านทุกทางได้โดยไม่ติด และศัตรูเดินจาก Cell ไป Cell ได้เสมอ
 *
 * ขั้นตอน:
 *   1. เติมกำแพงทั้งหมด (ขนาดเป็นเลขคี่ เช่น 15x11 Cell)
 *   2. ขุดเขาวงกตด้วย Depth-First Search (Recursive Backtracker)
 *   3. ขุดห้องโล่ง ๆ เพิ่ม (ไว้ต่อสู้ / เจอ Boss)
 *   4. ทุบกำแพงทางตันบางส่วน ให้มีทางวน (ไม่ต้องเดินย้อนไกล)
 *   5. หาจุดที่ไกลจากจุดเริ่มที่สุดบนขอบล่าง แล้วเจาะเป็นประตูทางออก
 *   6. ขยาย 1 Cell = 2x2 ช่อง แล้วส่งให้ Map
 */
public class MapGenerator {

    /** 1 Cell = กี่ช่องของ Map */
    public static final int CELL_TILES = 2;

    private final int cellCols;
    private final int cellRows;
    private final int roomCount;
    private final double loopChance;

    private boolean[][] wall;           // ตาราง Cell: true = กำแพง
    private int exitCol;                // Cell ของประตู (อยู่แถวล่างสุด)
    private final ArrayList<Point> roomCells = new ArrayList<Point>();

    // =========================
    // Constructor
    // =========================
    /**
     * @param cellCols   ความกว้างเป็น Cell (เลขคี่)
     * @param cellRows   ความสูงเป็น Cell (เลขคี่)
     * @param roomCount  จำนวนห้องโล่ง
     * @param loopChance โอกาสทุบทางตันให้เป็นทางวน (0-1)
     */
    public MapGenerator(int cellCols, int cellRows, int roomCount, double loopChance) {
        this.cellCols = cellCols | 1;       // บังคับเป็นเลขคี่
        this.cellRows = cellRows | 1;
        this.roomCount = roomCount;
        this.loopChance = loopChance;
    }

    // =========================
    // Random Helpers (ใช้ Math.random แบบในเอกสาร)
    // =========================
    private int randomInt(int bound) {
        return (int) (Math.random() * bound);
    }

    // =========================
    // Generate
    // =========================
    /** สร้างแผนที่ แล้วคืนค่าเป็นตารางช่องของ Map (0 = พื้น, 1 = กำแพง) */
    public int[][] generate() {
        wall = new boolean[cellRows][cellCols];
        for (int r = 0; r < cellRows; r++) {
            for (int c = 0; c < cellCols; c++) {
                wall[r][c] = true;
            }
        }

        carveMaze(1, 1);
        carveRooms();
        addLoops();
        carveExit();

        return toTileMap();
    }

    /** Recursive Backtracker: เดินสุ่มไปเรื่อย ๆ ทีละ 2 Cell แล้วทุบกำแพงตรงกลาง (ใช้ Stack แทน Recursion) */
    private void carveMaze(int startCol, int startRow) {
        ArrayList<Point> stack = new ArrayList<Point>();
        wall[startRow][startCol] = false;
        stack.add(new Point(startCol, startRow));
        int[][] directions = { { 0, -2 }, { 0, 2 }, { -2, 0 }, { 2, 0 } };

        while (!stack.isEmpty()) {
            Point current = stack.get(stack.size() - 1);

            // หาเพื่อนบ้านที่ยังเป็นกำแพง (ยังไม่เคยไป)
            ArrayList<int[]> options = new ArrayList<int[]>();
            for (int[] d : directions) {
                int nc = current.x + d[0];
                int nr = current.y + d[1];
                if (nr > 0 && nr < cellRows - 1 && nc > 0 && nc < cellCols - 1 && wall[nr][nc]) {
                    options.add(d);
                }
            }

            if (options.isEmpty()) {
                stack.remove(stack.size() - 1);     // ทางตัน -> ถอยกลับ
                continue;
            }

            int[] d = options.get(randomInt(options.size()));
            wall[current.y + d[1] / 2][current.x + d[0] / 2] = false;     // ทุบกำแพงตรงกลาง
            wall[current.y + d[1]][current.x + d[0]] = false;
            stack.add(new Point(current.x + d[0], current.y + d[1]));
        }
    }

    /** ห้องขนาด 3x3 หรือ 5x3 Cell วางที่พิกัดคี่ จึงต่อกับทางเดินพอดี */
    private void carveRooms() {
        for (int i = 0; i < roomCount; i++) {
            int width = (randomInt(2) == 0) ? 3 : 5;
            int height = 3;
            if (width + 2 > cellCols || height + 2 > cellRows) {
                continue;
            }
            int col = 1 + 2 * randomInt((cellCols - width - 1) / 2 + 1);
            int row = 1 + 2 * randomInt((cellRows - height - 1) / 2 + 1);
            col = Math.min(col, cellCols - 1 - width);
            row = Math.min(row, cellRows - 1 - height);

            for (int r = row; r < row + height; r++) {
                for (int c = col; c < col + width; c++) {
                    wall[r][c] = false;
                    roomCells.add(new Point(c, r));
                }
            }
        }
    }

    /** ทุบกำแพงที่ทางตันบางส่วน -> มีทางวน ไม่ต้องเดินย้อนกลับไกล */
    private void addLoops() {
        int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

        for (int r = 1; r < cellRows - 1; r++) {
            for (int c = 1; c < cellCols - 1; c++) {
                if (wall[r][c] || countOpenNeighbors(c, r) != 1 || Math.random() > loopChance) {
                    continue;
                }
                // ทางตัน: ลองทุบกำแพงที่ด้านหลังมีทางเดินอยู่แล้ว
                for (int[] d : directions) {
                    int wc = c + d[0];
                    int wr = r + d[1];
                    int bc = c + d[0] * 2;
                    int br = r + d[1] * 2;
                    if (br > 0 && br < cellRows - 1 && bc > 0 && bc < cellCols - 1
                            && wall[wr][wc] && !wall[br][bc]) {
                        wall[wr][wc] = false;
                        break;
                    }
                }
            }
        }
    }

    private int countOpenNeighbors(int c, int r) {
        int count = 0;
        if (!wall[r - 1][c]) count++;
        if (!wall[r + 1][c]) count++;
        if (!wall[r][c - 1]) count++;
        if (!wall[r][c + 1]) count++;
        return count;
    }

    /** ประตูทางออก = จุดบนแถวล่างสุดที่เดินจากจุดเริ่มไกลที่สุด */
    private void carveExit() {
        int[][] distance = distancesFrom(1, 1);
        int bestCol = 1;
        int best = -1;
        for (int c = 1; c < cellCols - 1; c++) {
            if (!wall[cellRows - 2][c] && distance[cellRows - 2][c] > best) {
                best = distance[cellRows - 2][c];
                bestCol = c;
            }
        }
        exitCol = bestCol;
        wall[cellRows - 1][exitCol] = false;      // เจาะกำแพงขอบล่าง
    }

    /** Breadth-First Search: ระยะทาง (จำนวน Cell) จากจุดหนึ่งไปทุก Cell (-1 = ไปไม่ถึง) */
    public int[][] distancesFrom(int startCol, int startRow) {
        int[][] distance = new int[cellRows][cellCols];
        for (int r = 0; r < cellRows; r++) {
            for (int c = 0; c < cellCols; c++) {
                distance[r][c] = -1;
            }
        }
        ArrayList<Point> queue = new ArrayList<Point>();
        queue.add(new Point(startCol, startRow));
        distance[startRow][startCol] = 0;
        int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

        for (int i = 0; i < queue.size(); i++) {
            Point p = queue.get(i);
            for (int[] d : directions) {
                int nc = p.x + d[0];
                int nr = p.y + d[1];
                if (nr >= 0 && nr < cellRows && nc >= 0 && nc < cellCols
                        && !wall[nr][nc] && distance[nr][nc] < 0) {
                    distance[nr][nc] = distance[p.y][p.x] + 1;
                    queue.add(new Point(nc, nr));
                }
            }
        }
        return distance;
    }

    /** ขยาย Cell เป็นช่องของ Map (1 Cell = 2x2 ช่อง) */
    private int[][] toTileMap() {
        int[][] tiles = new int[cellRows * CELL_TILES][cellCols * CELL_TILES];
        for (int r = 0; r < cellRows; r++) {
            for (int c = 0; c < cellCols; c++) {
                for (int tr = 0; tr < CELL_TILES; tr++) {
                    for (int tc = 0; tc < CELL_TILES; tc++) {
                        tiles[r * CELL_TILES + tr][c * CELL_TILES + tc] = wall[r][c] ? Map.WALL : Map.FLOOR;
                    }
                }
            }
        }
        return tiles;
    }

    // =========================
    // Getters
    // =========================
    public int getStartCellCol() { return 1; }
    public int getStartCellRow() { return 1; }
    public int getExitCellCol() { return exitCol; }
    public int getExitCellRow() { return cellRows - 1; }
    public int getCellCols() { return cellCols; }
    public int getCellRows() { return cellRows; }
    public boolean isCellWall(int col, int row) { return wall[row][col]; }
    public ArrayList<Point> getRoomCells() { return roomCells; }
}
