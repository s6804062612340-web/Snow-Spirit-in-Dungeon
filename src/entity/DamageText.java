package entity;

import java.awt.*;

import main.GamePanel;

/**
 * ตัวเลขลอยขึ้นแล้วจางหาย เช่น ความเสียหายที่ผู้เล่นทำกับศัตรู ("-1")
 * หรือ HP ที่ฟื้นจากยา ("+1") ใช้เวลาแสดง 45 เฟรม (0.75 วินาที)
 */
public class DamageText {

    private static final int LIFETIME = 45;

    private final GamePanel gamePanel;
    private final String text;
    private final Color color;
    private final int x;           // จุดกลางแนวนอน (พิกัดโลก)
    private double y;              // ฐานของตัวอักษร (พิกัดโลก)
    private int age = 0;

    // =========================
    // Constructor
    // =========================
    public DamageText(GamePanel gamePanel, String text, Color color, int centerX, int topY) {
        this.gamePanel = gamePanel;
        this.text = text;
        this.color = color;
        this.x = centerX;
        this.y = topY;
    }

    // =========================
    // Update / Draw
    // =========================
    public void update() {
        age++;
        y -= 0.8;      // ลอยขึ้นช้า ๆ
    }

    public boolean isFinished() {
        return age >= LIFETIME;
    }

    public void draw(Graphics g) {
        // ช่วงท้ายค่อย ๆ จางลง
        int alpha = 255;
        if (age > LIFETIME / 2) {
            alpha = 255 * (LIFETIME - age) / (LIFETIME / 2);
            alpha = Math.max(0, Math.min(255, alpha));
        }

        g.setFont(new Font("Arial", Font.BOLD, 22));
        int textWidth = g.getFontMetrics().stringWidth(text);
        int screenX = gamePanel.getCamera().screenX(x) - textWidth / 2;
        int screenY = gamePanel.getCamera().screenY((int) y);

        // เงาดำ + ตัวอักษรสี
        g.setColor(new Color(0, 0, 0, alpha));
        g.drawString(text, screenX + 2, screenY + 2);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.drawString(text, screenX, screenY);
    }
}
