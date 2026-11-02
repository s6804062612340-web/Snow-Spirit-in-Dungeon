package menu;

import java.awt.*;
import javax.swing.*;

/**
 * ปุ่มของเกมที่วาดเอง (Inheritance: extends JButton)
 * ยังใช้ ActionListener ได้เหมือน JButton ปกติทุกอย่าง แต่หน้าตาเปลี่ยนไป:
 *   - มุมโค้ง + สีพื้นเข้ม + ขอบสีประจำปุ่ม
 *   - เอาเมาส์ชี้ = สว่างขึ้น, กด = ยุบลง, ถูกเลือก (highlighted) = ขอบเรือง
 *   - ใส่ข้อความย่อยหลายบรรทัดได้ (ใช้ทำการ์ดเลือกโหมด EASY / NORMAL / HARD)
 *
 * Override paintComponent() = Polymorphism: Swing เรียกเมธอดเดิม แต่ได้หน้าตาของเรา
 */
public class MenuButton extends JButton {

    private final Color accent;
    private String[] lines = new String[0];      // ข้อความย่อยใต้ชื่อปุ่ม
    private boolean highlighted = false;
    private int titleSize = 18;

    // =========================
    // Constructor
    // =========================
    public MenuButton(String text, Color accent) {
        super(text);
        this.accent = accent;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFocusable(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public void setLines(String... lines) {
        this.lines = lines;
        repaint();
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        repaint();
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setTitleSize(int size) {
        titleSize = size;
    }

    // =========================
    // Draw
    // =========================
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        boolean hover = getModel().isRollover();
        boolean pressed = getModel().isPressed();
        int offset = pressed ? 2 : 0;
        int arc = Math.min(20, h / 2);

        // เงา
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(2, 5, w - 4, h - 6, arc, arc);

        // พื้นปุ่ม
        Color base = new Color(22, 32, 58);
        if (highlighted) {
            base = mix(base, accent, 0.28);
        } else if (hover) {
            base = mix(base, accent, 0.16);
        }
        g.setColor(base);
        g.fillRoundRect(1, 1 + offset, w - 3, h - 6, arc, arc);

        // แถบสีด้านบน (ไฮไลต์)
        g.setColor(new Color(255, 255, 255, hover || highlighted ? 18 : 9));
        g.fillRoundRect(4, 4 + offset, w - 9, (h - 6) / 3, arc, arc);

        // ขอบ
        Color border = highlighted || hover ? accent : mix(accent, base, 0.55);
        g.setColor(border);
        g.drawRoundRect(1, 1 + offset, w - 3, h - 6, arc, arc);
        if (highlighted) {
            g.drawRoundRect(2, 2 + offset, w - 5, h - 8, arc, arc);
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
            g.drawRoundRect(0, offset, w - 1, h - 4, arc, arc);
        }

        // ข้อความ
        g.setFont(new Font("Arial", Font.BOLD, titleSize));
        FontMetrics fm = g.getFontMetrics();
        int textY;
        if (lines.length == 0) {
            textY = (h - 6) / 2 + fm.getAscent() / 2 - 1 + offset;
        } else {
            textY = 20 + titleSize + offset;
        }
        g.setColor(highlighted || hover ? Color.WHITE : new Color(220, 230, 250));
        if (lines.length > 0) {
            g.setColor(accent);
        }
        g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, textY);

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        fm = g.getFontMetrics();
        for (int i = 0; i < lines.length; i++) {
            g.setColor(i == lines.length - 1 ? new Color(160, 180, 215) : new Color(225, 235, 250));
            g.drawString(lines[i], (w - fm.stringWidth(lines[i])) / 2, textY + 24 + i * 17);
        }

        // เครื่องหมายถูกที่มุมการ์ดที่เลือกอยู่
        if (highlighted && lines.length > 0) {
            g.setColor(accent);
            g.fillOval(w - 30, 10 + offset, 18, 18);
            g.setColor(new Color(20, 30, 50));
            g.drawLine(w - 25, 19 + offset, w - 22, 23 + offset);
            g.drawLine(w - 22, 23 + offset, w - 16, 14 + offset);
        }
    }

    private static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
