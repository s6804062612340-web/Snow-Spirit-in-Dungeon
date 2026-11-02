package world;

import java.awt.*;
import javax.swing.*;

/**
 * ภาพของช่องแผนที่จาก Tileset (assets/tilemap_packed.png)
 * ไฟล์เป็นตาราง 12 คอลัมน์ ช่องละ 16x16 px -> เลขช่อง = แถว * 12 + คอลัมน์
 *
 * วาดขยายเป็นขนาดใดก็ได้ (เช่น 64x64) ด้วย drawImage แบบตัดส่วน
 */
public final class TileSet {

    public static final int SOURCE_SIZE = 16;
    private static final int COLUMNS = 12;

    // ===== เลขช่องที่ใช้ในเกม =====
    public static final int FLOOR = 48;             // พื้นทราย
    public static final int FLOOR_DOTS = 49;        // พื้นทรายมีจุด
    public static final int FLOOR_RUBBLE = 42;      // พื้นมีหินแตก
    public static final int FLOOR_CRACK = 51;
    public static final int WALL_FACE = 40;         // หน้ากำแพงอิฐ (เห็นจากด้านหน้า)
    public static final int WALL_WINDOW = 28;       // หน้ากำแพงมีหน้าต่าง
    public static final int WALL_BANNER = 29;       // หน้ากำแพงมีธง
    public static final int WALL_TOP = 0;           // ด้านบนกำแพง (มืด)
    public static final int DOOR_LEFT = 46;         // ประตูคู่ บานซ้าย
    public static final int DOOR_RIGHT = 47;        // ประตูคู่ บานขวา
    public static final int DOOR_OPEN_LEFT = 10;    // ช่องประตูเปิด
    public static final int DOOR_OPEN_RIGHT = 11;
    public static final int CHEST_CLOSED = 89;      // หีบสมบัติ (ปิด)
    public static final int CHEST_OPEN = 91;        // หีบสมบัติ (เปิดแล้ว)
    public static final int SHIELD = 102;           // โล่กลม (ไอคอนสกิลโล่)
    public static final int SWORD = 104;            // ดาบ (ไอคอนสกิลดาบน้ำแข็ง)
    public static final int POTION_GREEN = 114;     // ยาเขียว = ฟื้น Stamina
    public static final int POTION_RED = 115;       // ยาแดง = ฟื้น HP

    private static final ImageIcon SHEET = new ImageIcon("assets/tilemap_packed.png");

    private TileSet() { }

    /** โหลดรูป Tileset ได้หรือไม่ (ถ้าไม่ได้ เกมจะวาดสีพื้นแทน) */
    public static boolean isLoaded() {
        return SHEET.getImageLoadStatus() == MediaTracker.COMPLETE;
    }

    /** วาดช่องหมายเลข index ที่ตำแหน่งบนจอ (x, y) ขนาด size x size */
    public static void draw(Graphics g, int index, int x, int y, int size) {
        int sx = (index % COLUMNS) * SOURCE_SIZE;
        int sy = (index / COLUMNS) * SOURCE_SIZE;
        g.drawImage(SHEET.getImage(),
                x, y, x + size, y + size,
                sx, sy, sx + SOURCE_SIZE, sy + SOURCE_SIZE,
                null);
    }
}
