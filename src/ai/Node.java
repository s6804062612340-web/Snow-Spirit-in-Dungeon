package ai;

/**
 * 1 ช่องในการค้นหาเส้นทาง A*
 * gCost = ระยะที่เดินมาแล้ว, hCost = ระยะประมาณถึงเป้าหมาย, fCost = g + h
 */
class Node {

    final int row;
    final int col;
    int gCost;
    int hCost;
    int fCost;
    Node parent;

    Node(int row, int col) {
        this.row = row;
        this.col = col;
    }

    void calculateFCost() {
        fCost = gCost + hCost;
    }
}
