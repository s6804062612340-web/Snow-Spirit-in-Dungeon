package world;

/**
 * ตั้งค่าของทั้ง 10 ด่าน (ทุกด่านสุ่มแผนที่เขาวงกตด้วย MapGenerator)
 * ค่าในตารางนี้คือระดับ EASY ส่วน NORMAL / HARD จะคูณเพิ่มจาก Difficulty
 *
 * หีบสมบัติต่อด่าน: SMALL 1 / MEDIUM 2 / LARGE 3 ใบ (ศัตรูถือกุญแจหีบเท่ากับจำนวนหีบ)
 *
 * ขนาดแผนที่ 3 แบบ (หน่วย Cell = 128x128 px):
 *   SMALL  =  9 x 7   Cell  (1152 x 896 px)
 *   MEDIUM = 13 x 9   Cell  (1664 x 1152 px)
 *   LARGE  = 15 x 11  Cell  (1920 x 1408 px)
 *
 * ความยากเพิ่มขึ้นทีละขั้น:
 *   ด่าน 1-2   : แผนที่เล็ก ศัตรูน้อย ส่วนใหญ่เป็น Slime (ฝึกเดิน/ยิง/Dash)
 *                ด่าน 2 เจอ Slime ตีไกลตัวแรก
 *   ด่าน 3     : เจอ Boss ครั้งแรก (HP น้อย)
 *   ด่าน 4-6   : Skeleton มากขึ้น แผนที่ใหญ่ขึ้น, ด่าน 4 เจอ Skeleton ตีไกล
 *   ด่าน 7-9   : Slime เร็วขึ้นเท่า Skeleton, Boss HP มากขึ้น, แผนที่ใหญ่
 *   ด่าน 10    : Boss สุดท้าย HP มาก + ลูกน้องเต็มแผนที่
 */
public final class LevelMaps {

    public static final int MAX_LEVELS = 10;

    // ===== ขนาดแผนที่ =====
    public static final int SMALL = 0;
    public static final int MEDIUM = 1;
    public static final int LARGE = 2;
    private static final String[] SIZE_NAMES = { "SMALL", "MEDIUM", "LARGE" };
    private static final int[] SIZE_COLS  = {  9, 13, 15 };    // Cell (ต้องเป็นเลขคี่)
    private static final int[] SIZE_ROWS  = {  7,  9, 11 };
    private static final int[] SIZE_ROOMS = {  1,  2,  3 };    // ห้องโล่งในเขาวงกต
    private static final int[] SIZE_CHESTS = { 1,  2,  3 };    // หีบสมบัติต่อด่าน
    private static final double LOOP_CHANCE = 0.5;            // โอกาสทุบทางตันให้เป็นทางวน

    // ด่าน 1 - 10
    private static final int[] MAP_SIZE     = { SMALL, SMALL, MEDIUM, SMALL, MEDIUM, MEDIUM, LARGE, MEDIUM, LARGE, LARGE };
    private static final int[] SLIMES       = {  3,  3,  3,  4,  4,  4,  4,  5,  5,  4 };
    private static final int[] SKELETONS    = {  0,  1,  2,  2,  3,  3,  4,  4,  5,  5 };
    // จากจำนวนข้างบน มีกี่ตัวที่เป็นแบบตีไกล (RANGED)
    private static final int[] SLIME_RANGED = {  0,  1,  1,  1,  2,  2,  2,  2,  2,  2 };
    private static final int[] SKEL_RANGED  = {  0,  0,  0,  1,  1,  1,  2,  2,  2,  2 };
    private static final int[] BOSS_HP      = {  0,  0,  6,  0, 10,  0, 12,  0, 14, 20 };  // 0 = ไม่มี Boss
    private static final int[] SLIME_SPEED  = {  1,  1,  1,  1,  1,  1,  2,  2,  2,  2 };  // Slime เร็วขึ้นตั้งแต่ด่าน 7
    private static final int[] BOSS_SPEED   = {  1,  1,  1,  1,  1,  1,  1,  1,  1,  1 };

    private LevelMaps() { }

    public static int getMapSize(int level) { return MAP_SIZE[level - 1]; }
    public static String getMapSizeName(int level) { return SIZE_NAMES[MAP_SIZE[level - 1]]; }
    public static int getCellCols(int level) { return SIZE_COLS[MAP_SIZE[level - 1]]; }
    public static int getCellRows(int level) { return SIZE_ROWS[MAP_SIZE[level - 1]]; }
    public static int getRoomCount(int level) { return SIZE_ROOMS[MAP_SIZE[level - 1]]; }
    public static int getChestCount(int level) { return SIZE_CHESTS[MAP_SIZE[level - 1]]; }
    public static double getLoopChance() { return LOOP_CHANCE; }
    public static int getSlimeCount(int level) { return SLIMES[level - 1]; }
    public static int getSkeletonCount(int level) { return SKELETONS[level - 1]; }
    public static int getRangedSlimeCount(int level) { return SLIME_RANGED[level - 1]; }
    public static int getRangedSkeletonCount(int level) { return SKEL_RANGED[level - 1]; }
    public static int getBossHp(int level) { return BOSS_HP[level - 1]; }
    public static int getSlimeSpeed(int level) { return SLIME_SPEED[level - 1]; }
    public static int getBossSpeed(int level) { return BOSS_SPEED[level - 1]; }
    public static boolean hasBoss(int level) { return BOSS_HP[level - 1] > 0; }
}
