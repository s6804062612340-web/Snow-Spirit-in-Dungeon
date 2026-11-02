package ai;

import java.awt.*;
import java.util.ArrayList;

/**
 * Flow Field (แผนที่ระยะทาง) สำหรับให้ศัตรูทุกตัวหาทางไปหา Player
 *
 * วิธีทำงาน:
 *   - ใช้ Breadth-First Search เริ่มจาก Cell ที่ Player ยืนอยู่ กระจายออกไปทุก Cell
 *   - แต่ละ Cell จะรู้ว่า "ห่างจาก Player กี่ก้าว"
 *   - ศัตรูแค่เดินไป Cell ข้าง ๆ ที่ตัวเลขน้อยกว่า ก็จะถึง Player เสมอ (ไม่มีทางหลง / ไม่ติด)
 *   - อยากหนี Player (ศัตรูตีไกล) ก็เดินไป Cell ที่ตัวเลขมากกว่า
 *
 * คำนวณครั้งเดียวต่อการที่ Player เปลี่ยน Cell แล้วศัตรูทุกตัวใช้ร่วมกัน (เร็วกว่า A* ทีละตัวมาก)
 */
public class FlowField {

    public static final int UNREACHABLE = Integer.MAX_VALUE;
    private static final int[][] DIRECTIONS = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

    private final boolean[][] blocked;      // true = Cell กำแพง
    private final int cols;
    private final int rows;
    private int[][] distance;
    private int targetCol = -1;
    private int targetRow = -1;

    // =========================
    // Constructor
    // =========================
    public FlowField(boolean[][] blocked) {
        this.blocked = blocked;
        this.rows = blocked.length;
        this.cols = blocked[0].length;
        this.distance = new int[rows][cols];
    }

    // =========================
    // Update
    // =========================
    /** คำนวณใหม่เมื่อเป้าหมายย้าย Cell (ถ้า Cell เดิม ไม่ต้องคำนวณซ้ำ) */
    public void setTarget(int col, int row) {
        if (col == targetCol && row == targetRow) {
            return;
        }
        targetCol = col;
        targetRow = row;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                distance[r][c] = UNREACHABLE;
            }
        }
        if (!isOpen(col, row)) {
            return;
        }

        ArrayList<Point> queue = new ArrayList<Point>();
        queue.add(new Point(col, row));
        distance[row][col] = 0;

        for (int i = 0; i < queue.size(); i++) {
            Point p = queue.get(i);
            for (int[] d : DIRECTIONS) {
                int nc = p.x + d[0];
                int nr = p.y + d[1];
                if (isOpen(nc, nr) && distance[nr][nc] == UNREACHABLE) {
                    distance[nr][nc] = distance[p.y][p.x] + 1;
                    queue.add(new Point(nc, nr));
                }
            }
        }
    }

    // =========================
    // Query
    // =========================
    /** Cell ถัดไปที่ควรเดินเพื่อเข้าใกล้เป้าหมาย (null = อยู่ที่เป้าหมายแล้ว / ไปไม่ถึง) */
    public Point nextStepToward(int col, int row) {
        return bestNeighbor(col, row, true);
    }

    /** Cell ถัดไปที่ควรเดินเพื่อหนีห่างจากเป้าหมาย (null = หนีต่อไม่ได้ / ทางตัน) */
    public Point nextStepAway(int col, int row) {
        return bestNeighbor(col, row, false);
    }

    private Point bestNeighbor(int col, int row, boolean toward) {
        int current = getDistance(col, row);
        if (current == UNREACHABLE) {
            return null;
        }

        Point best = null;
        int bestValue = current;
        for (int[] d : DIRECTIONS) {
            int nc = col + d[0];
            int nr = row + d[1];
            int value = getDistance(nc, nr);
            if (value == UNREACHABLE) {
                continue;
            }
            if (toward ? value < bestValue : value > bestValue) {
                bestValue = value;
                best = new Point(nc, nr);
            }
        }
        return best;
    }

    public int getDistance(int col, int row) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return UNREACHABLE;
        }
        return distance[row][col];
    }

    public boolean isOpen(int col, int row) {
        return row >= 0 && row < rows && col >= 0 && col < cols && !blocked[row][col];
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
