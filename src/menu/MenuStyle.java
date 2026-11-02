package menu;

import java.awt.*;

/**
 * สีและฟอนต์ที่หน้าเมนูทุกหน้าใช้ร่วมกัน (ไม่ต้องเขียนซ้ำในแต่ละ Panel)
 */
public final class MenuStyle {

    public static final int WIDTH = 768;
    public static final int HEIGHT = 640;

    public static final Color ACCENT = new Color(140, 200, 255);
    public static final Color TEXT = new Color(235, 242, 255);
    public static final Color TEXT_DIM = new Color(150, 170, 205);

    private static final Color BACKGROUND_TOP = new Color(8, 12, 28);
    private static final Color BACKGROUND_BOTTOM = new Color(30, 50, 88);

    private MenuStyle() { }

    /** เปิด Anti-aliasing ให้ตัวอักษร / รูปทรงเรียบ */
    public static void smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * พื้นหลัง: ไล่สีจากบนลงล่าง + หิมะตกช้า ๆ + กำแพงดันเจี้ยนด้านล่าง
     * @param frame ตัวนับเฟรม (ทำให้หิมะเคลื่อนที่)
     */
    public static void drawBackground(Graphics g, int width, int height, int frame) {
        smooth(g);
        for (int y = 0; y < height; y += 4) {
            float t = (float) y / height;
            int r = (int) (BACKGROUND_TOP.getRed() + t * (BACKGROUND_BOTTOM.getRed() - BACKGROUND_TOP.getRed()));
            int gr = (int) (BACKGROUND_TOP.getGreen() + t * (BACKGROUND_BOTTOM.getGreen() - BACKGROUND_TOP.getGreen()));
            int b = (int) (BACKGROUND_TOP.getBlue() + t * (BACKGROUND_BOTTOM.getBlue() - BACKGROUND_TOP.getBlue()));
            g.setColor(new Color(r, gr, b));
            g.fillRect(0, y, width, 4);
        }

        // แสงจันทร์
        g.setColor(new Color(180, 210, 255, 18));
        g.fillOval(width - 260, -120, 360, 360);
        g.setColor(new Color(220, 235, 255, 40));
        g.fillOval(width - 150, 40, 70, 70);

        // กำแพงอิฐด้านล่าง (เงาดันเจี้ยน)
        int wallTop = height - 70;
        g.setColor(new Color(10, 14, 30));
        g.fillRect(0, wallTop, width, 70);
        g.setColor(new Color(30, 40, 66));
        for (int row = 0; row < 3; row++) {
            int y = wallTop + row * 24;
            int shift = (row % 2) * 24;
            for (int x = -shift; x < width; x += 48) {
                g.drawRect(x, y, 48, 24);
            }
        }
        // ยอดกำแพงเป็นฟันปลา
        g.setColor(new Color(10, 14, 30));
        for (int x = 0; x < width; x += 64) {
            g.fillRect(x, wallTop - 18, 36, 18);
        }

        // หิมะตก (แต่ละเกล็ดคำนวณจากเลขลำดับ ไม่ต้องเก็บตำแหน่ง)
        for (int i = 0; i < 90; i++) {
            int speed = 1 + i % 3;
            int baseX = (i * 97) % width;
            int x = baseX + (int) (12 * Math.sin((frame + i * 37) * 0.02));
            int y = ((i * 53 + i * i * 7) + frame * speed / 2) % height;
            int size = 2 + i % 3;
            g.setColor(new Color(255, 255, 255, 90 + (i % 3) * 50));
            g.fillOval(x, y, size, size);
        }
    }

    /** หัวข้อใหญ่ มีเงา + เส้นเรืองใต้ตัวอักษร */
    public static void drawTitle(Graphics g, String text, int y, int width) {
        g.setFont(new Font("Arial", Font.BOLD, 56));
        g.setColor(new Color(60, 120, 220, 90));
        drawCentered(g, text, y + 5, width);
        g.setColor(Color.WHITE);
        drawCentered(g, text, y, width);
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.setColor(ACCENT);
        g.fillRoundRect((width - textWidth) / 2, y + 12, textWidth, 3, 3, 3);
    }

    /** หัวข้อเล็กของแต่ละส่วน เช่น "SELECT MODE" */
    public static void drawSectionLabel(Graphics g, String text, int y, int width) {
        g.setFont(new Font("Arial", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = (width - textWidth) / 2;
        g.setColor(TEXT_DIM);
        g.drawString(text, x, y);
        g.setColor(new Color(150, 170, 205, 90));
        g.fillRect(x - 70, y - 5, 56, 1);
        g.fillRect(x + textWidth + 14, y - 5, 56, 1);
    }

    /** กล่องพื้นหลังโปร่งแสงมุมโค้ง */
    public static void drawPanel(Graphics g, int x, int y, int width, int height) {
        g.setColor(new Color(12, 18, 34, 200));
        g.fillRoundRect(x, y, width, height, 20, 20);
        g.setColor(new Color(150, 190, 255, 70));
        g.drawRoundRect(x, y, width, height, 20, 20);
    }

    public static void drawCentered(Graphics g, String text, int y, int width) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (width - textWidth) / 2, y);
    }
}
