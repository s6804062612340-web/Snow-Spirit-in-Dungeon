package main;

import world.Map;

/**
 * กล้องที่เลื่อนตาม Player
 * แปลงพิกัดในโลก (World) -> พิกัดบนหน้าจอ (Screen)
 */
public class Camera {

    private final GamePanel gamePanel;
    private int x = 0;
    private int y = 0;

    // จอสั่น: เลื่อนภาพแบบสุ่มเล็กน้อยชั่วคราว (ไม่กระทบตำแหน่งจริงของกล้อง)
    private int shakeStrength = 0;
    private int shakeFrames = 0;
    private int shakeX = 0;
    private int shakeY = 0;

    // =========================
    // Constructor
    // =========================
    public Camera(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    // =========================
    // Update
    // =========================
    public void update() {
        int screenWidth = gamePanel.getWidth();
        int screenHeight = gamePanel.getHeight();

        // ให้ลำตัว Player อยู่กึ่งกลางหน้าจอ
        x = gamePanel.getPlayer().getCenterX() - screenWidth / 2;
        y = gamePanel.getPlayer().getCenterY() - screenHeight / 2;

        // จำกัด Camera ไม่ให้ออกนอก Map
        Map map = gamePanel.getMap();
        int maxCameraX = map.getWorldWidth() - screenWidth;
        int maxCameraY = map.getWorldHeight() - screenHeight;

        x = clamp(x, 0, maxCameraX);
        y = clamp(y, 0, maxCameraY);

        updateShake();
    }

    /** สั่นจอ strength พิกเซล เป็นเวลา frames เฟรม (ถ้ากำลังสั่นแรงกว่าอยู่ จะไม่ลดลง) */
    public void shake(int strength, int frames) {
        if (strength >= shakeStrength || shakeFrames == 0) {
            shakeStrength = strength;
            shakeFrames = frames;
        }
    }

    private void updateShake() {
        if (shakeFrames > 0) {
            shakeFrames--;
            int power = shakeStrength * shakeFrames / Math.max(1, shakeFrames + 4) + 1;
            shakeX = (int) ((Math.random() * 2 - 1) * power);
            shakeY = (int) ((Math.random() * 2 - 1) * power);
        } else {
            shakeStrength = 0;
            shakeX = 0;
            shakeY = 0;
        }
    }

    // เหมือนโค้ดเดิม: เช็คขอบล่างก่อน แล้วค่อยเช็คขอบบน
    private int clamp(int value, int min, int max) {
        if (value < min) {
            value = min;
        }
        if (value > max) {
            value = max;
        }
        return value;
    }

    // =========================
    // World -> Screen
    // =========================
    public int screenX(int worldX) {
        return worldX - x + shakeX;
    }

    public int screenY(int worldY) {
        return worldY - y + shakeY;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
