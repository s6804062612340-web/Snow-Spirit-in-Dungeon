package ai;

import java.awt.*;
import java.util.ArrayList;

/**
 * ค้นหาเส้นทางด้วย A* บนแผนที่แบบ Grid (เดิน 4 ทิศ)
 *
 * รองรับ Object ที่ใหญ่กว่า 1 ช่องด้วย blockSize:
 *   ตำแหน่ง (row, col) หมายถึง "มุมซ้ายบนของกล่อง blockSize x blockSize ช่อง"
 *   ตำแหน่งนั้นเดินได้เมื่อทุกช่องในกล่องเป็นพื้น
 *   เช่น Slime (64px) ใช้ blockSize = 1, Boss / Player (ใหญ่กว่า 64px) ใช้ blockSize = 2
 *
 * ผลลัพธ์เป็น ArrayList ของ Point(col, row) จากจุดเริ่ม -> เป้าหมาย
 */
public class Pathfinding {

    private static final int WALL = 1;
    private static final int[][] DIRECTIONS = {
            { -1, 0 },  // Up
            { 1, 0 },   // Down
            { 0, -1 },  // Left
            { 0, 1 }    // Right
    };

    private final int[][] map;
    private final int blockSize;

    // =========================
    // Constructors
    // =========================
    public Pathfinding(int[][] map) {
        this(map, 1);
    }

    public Pathfinding(int[][] map, int blockSize) {
        this.map = map;
        this.blockSize = blockSize;
    }

    // =========================
    // Find Path
    // =========================
    public ArrayList<Point> findPath(int startRow, int startCol, int targetRow, int targetCol) {
        int rows = map.length;
        int cols = map[0].length;

        boolean startInside = startRow >= 0 && startRow < rows && startCol >= 0 && startCol < cols;
        if (!startInside || !isWalkable(targetRow, targetCol)) {
            return new ArrayList<Point>();
        }

        ArrayList<Node> openList = new ArrayList<Node>();
        boolean[][] closed = new boolean[rows][cols];
        int[][] bestGCost = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                bestGCost[r][c] = Integer.MAX_VALUE;
            }
        }

        Node startNode = new Node(startRow, startCol);
        startNode.gCost = 0;
        startNode.hCost = manhattanDistance(startRow, startCol, targetRow, targetCol);
        startNode.calculateFCost();
        openList.add(startNode);
        bestGCost[startRow][startCol] = 0;

        while (!openList.isEmpty()) {
            Node current = removeLowestFCost(openList);

            if (closed[current.row][current.col]) {
                continue;
            }
            closed[current.row][current.col] = true;

            // เจอเป้าหมายแล้ว
            if (current.row == targetRow && current.col == targetCol) {
                return buildPath(current);
            }

            // ตรวจตำแหน่งรอบข้าง 4 ทิศ
            for (int[] dir : DIRECTIONS) {
                int row = current.row + dir[0];
                int col = current.col + dir[1];

                if (!isWalkable(row, col) || closed[row][col]) {
                    continue;
                }

                int newGCost = current.gCost + 1;
                if (newGCost >= bestGCost[row][col]) {
                    continue;   // เคยเจอทางที่สั้นกว่าแล้ว
                }
                bestGCost[row][col] = newGCost;

                Node neighbor = new Node(row, col);
                neighbor.gCost = newGCost;
                neighbor.hCost = manhattanDistance(row, col, targetRow, targetCol);
                neighbor.calculateFCost();
                neighbor.parent = current;
                openList.add(neighbor);
            }
        }

        // หาเส้นทางไม่ได้
        return new ArrayList<Point>();
    }

    // =========================
    // Helpers
    // =========================
    /** กล่อง blockSize x blockSize ที่มุมซ้ายบน (row, col) อยู่บนพื้นทั้งหมดหรือไม่ */
    public boolean isWalkable(int row, int col) {
        for (int r = row; r < row + blockSize; r++) {
            for (int c = col; c < col + blockSize; c++) {
                if (r < 0 || r >= map.length || c < 0 || c >= map[0].length || map[r][c] == WALL) {
                    return false;
                }
            }
        }
        return true;
    }

    /** หยิบ Node ที่มี fCost น้อยที่สุดออกจาก openList (ถ้าเท่ากัน เลือก hCost น้อยกว่า) */
    private Node removeLowestFCost(ArrayList<Node> openList) {
        int bestIndex = 0;
        for (int i = 1; i < openList.size(); i++) {
            Node n = openList.get(i);
            Node best = openList.get(bestIndex);
            if (n.fCost < best.fCost || (n.fCost == best.fCost && n.hCost < best.hCost)) {
                bestIndex = i;
            }
        }
        return openList.remove(bestIndex);
    }

    private int manhattanDistance(int row1, int col1, int row2, int col2) {
        return Math.abs(row1 - row2) + Math.abs(col1 - col2);
    }

    /** ไล่ย้อนจาก Node ปลายทางผ่าน parent แล้วใส่ไว้ด้านหน้าเรื่อย ๆ (ได้ลำดับ Start -> Target) */
    private ArrayList<Point> buildPath(Node endNode) {
        ArrayList<Point> path = new ArrayList<Point>();
        for (Node node = endNode; node != null; node = node.parent) {
            path.add(0, new Point(node.col, node.row));
        }
        return path;
    }
}
