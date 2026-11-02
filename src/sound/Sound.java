package sound;

/**
 * รวมเสียง Effect ทั้งหมดของเกม เรียกใช้ได้จากทุกที่ เช่น Sound.SHOOT.play();
 *
 * แต่ละเสียง = รายการโน้ต { ความถี่เริ่ม Hz, ความถี่จบ Hz, ความยาว ms, รูปคลื่น, ความดัง }
 * อยากเปลี่ยนเสียงไหน แก้ตัวเลขในตารางของเสียงนั้นได้เลย
 */
public final class Sound {

    private static final int SQUARE = SoundEffect.SQUARE;
    private static final int TRIANGLE = SoundEffect.TRIANGLE;
    private static final int NOISE = SoundEffect.NOISE;
    private static final int SINE = SoundEffect.SINE;

    private static boolean enabled = true;

    // ===== Player =====
    public static final SoundEffect SHOOT = new SoundEffect(new int[][] {
            { 1400, 700, 70, SQUARE, 35 } });
    public static final SoundEffect DASH = new SoundEffect(new int[][] {
            { 300, 1800, 60, NOISE, 45 },
            { 1800, 900, 70, NOISE, 25 } });
    public static final SoundEffect PLAYER_HURT = new SoundEffect(new int[][] {
            { 500, 120, 180, SQUARE, 55 } });

    // ===== Enemy =====
    public static final SoundEffect ENEMY_HIT = new SoundEffect(new int[][] {
            { 900, 300, 60, NOISE, 45 } });
    public static final SoundEffect ENEMY_DIE = new SoundEffect(new int[][] {
            { 600, 900, 50, SQUARE, 40 },
            { 900, 100, 220, NOISE, 45 } });
    public static final SoundEffect ENEMY_SHOOT = new SoundEffect(new int[][] {
            { 300, 500, 90, TRIANGLE, 50 } });
    public static final SoundEffect ENEMY_WINDUP = new SoundEffect(new int[][] {
            { 180, 320, 160, SQUARE, 22 } });
    public static final SoundEffect ENEMY_STRIKE = new SoundEffect(new int[][] {
            { 700, 150, 110, NOISE, 45 } });
    public static final SoundEffect ENEMY_AIM = new SoundEffect(new int[][] {
            { 1200, 1600, 90, SINE, 20 } });
    public static final SoundEffect BOSS_CHARGE = new SoundEffect(new int[][] {
            { 120, 260, 380, SQUARE, 30 } });
    public static final SoundEffect BOSS_TELEPORT = new SoundEffect(new int[][] {
            { 900, 200, 160, SINE, 45 },
            { 200, 1400, 160, TRIANGLE, 40 } });
    public static final SoundEffect BOSS_SLAM = new SoundEffect(new int[][] {
            { 200, 40, 350, NOISE, 70 } });

    // ===== Skills / Stamina =====
    public static final SoundEffect SHIELD_UP = new SoundEffect(new int[][] {
            { 400, 900, 120, SINE, 45 },
            { 900, 1200, 100, TRIANGLE, 35 } });
    public static final SoundEffect SHIELD_BREAK = new SoundEffect(new int[][] {
            { 1500, 600, 90, SQUARE, 35 },
            { 1200, 200, 200, NOISE, 45 } });
    public static final SoundEffect ICE_SWORD = new SoundEffect(new int[][] {
            { 1800, 2400, 80, SINE, 35 },
            { 2400, 1600, 120, TRIANGLE, 35 } });
    public static final SoundEffect NO_STAMINA = new SoundEffect(new int[][] {
            { 220, 180, 90, SQUARE, 30 } });

    // ===== Items / World =====
    public static final SoundEffect CHEST_OPEN = new SoundEffect(new int[][] {
            { 200, 300, 80, NOISE, 40 },
            { 659, 659, 90, TRIANGLE, 50 },
            { 880, 880, 90, TRIANGLE, 50 },
            { 1319, 1319, 220, TRIANGLE, 50 } });
    public static final SoundEffect SKILL_LEARN = new SoundEffect(new int[][] {
            { 523, 523, 90, SQUARE, 30 },
            { 784, 784, 90, SQUARE, 30 },
            { 1047, 1047, 90, SQUARE, 30 },
            { 1568, 1568, 260, SINE, 45 } });
    public static final SoundEffect LOCKED = new SoundEffect(new int[][] {
            { 300, 250, 70, SQUARE, 30 },
            { 250, 200, 90, SQUARE, 30 } });
    public static final SoundEffect PICKUP = new SoundEffect(new int[][] {
            { 880, 880, 60, SINE, 45 },
            { 1320, 1320, 90, SINE, 45 } });
    public static final SoundEffect KEY = new SoundEffect(new int[][] {
            { 988, 988, 80, SQUARE, 30 },
            { 1319, 1319, 80, SQUARE, 30 },
            { 1976, 1976, 160, SQUARE, 30 } });
    public static final SoundEffect DRINK = new SoundEffect(new int[][] {
            { 400, 800, 80, SINE, 50 },
            { 500, 1000, 80, SINE, 50 },
            { 600, 1200, 120, SINE, 50 } });
    public static final SoundEffect DOOR = new SoundEffect(new int[][] {
            { 523, 523, 100, TRIANGLE, 55 },
            { 659, 659, 100, TRIANGLE, 55 },
            { 784, 784, 100, TRIANGLE, 55 },
            { 1047, 1047, 250, TRIANGLE, 55 } });

    // ===== Game =====
    public static final SoundEffect GAME_OVER = new SoundEffect(new int[][] {
            { 392, 392, 200, TRIANGLE, 60 },
            { 330, 330, 200, TRIANGLE, 60 },
            { 262, 200, 500, TRIANGLE, 60 } });
    public static final SoundEffect VICTORY = new SoundEffect(new int[][] {
            { 523, 523, 120, SQUARE, 35 },
            { 659, 659, 120, SQUARE, 35 },
            { 784, 784, 120, SQUARE, 35 },
            { 1047, 1047, 120, SQUARE, 35 },
            { 784, 784, 120, SQUARE, 35 },
            { 1047, 1047, 450, SQUARE, 35 } });
    public static final SoundEffect CLICK = new SoundEffect(new int[][] {
            { 1000, 1200, 40, SQUARE, 25 } });

    private Sound() { }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean on) { enabled = on; }
}
