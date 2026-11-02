package menu;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import item.ChestKey;
import world.TileSet;

/**
 * หน้า HOW TO PLAY: ปุ่มควบคุม + ไอเทม / สกิล / สัญญาณเตือนของศัตรู
 * ปุ่ม BBack ให้ Main ดักจับเพื่อกลับหน้าเมนู
 */
public class HelpPanel extends JPanel implements ActionListener {

    public JButton BBack = new MenuButton("BACK", MenuStyle.ACCENT);

    private int frame = 0;
    private final Timer animationTimer = new Timer(33, this);

    private static final String[][] CONTROLS = {
            { "WASD", "Move" },
            { "MOUSE", "Aim" },
            { "CLICK", "Shoot  (-4 stamina)" },
            { "SPACE", "Dash  (-25 stamina)" },
            { "Q", "Ice Shield skill" },
            { "E", "Ice Sword skill" },
            { "1 - 5", "Drink potion" },
            { "ESC", "Back to menu" } };

    public HelpPanel() {
        setPreferredSize(new Dimension(MenuStyle.WIDTH, MenuStyle.HEIGHT));
        setLayout(null);
        BBack.setBounds(294, 540, 180, 48);
        add(BBack);
        animationTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frame++;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        MenuStyle.drawBackground(g, getWidth(), getHeight(), frame);
        MenuStyle.drawTitle(g, "HOW TO PLAY", 84, getWidth());

        // ===== ซ้าย: ปุ่มควบคุม =====
        MenuStyle.drawPanel(g, 40, 120, 320, 400);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(MenuStyle.ACCENT);
        g.drawString("CONTROLS", 62, 150);
        for (int i = 0; i < CONTROLS.length; i++) {
            int y = 182 + i * 40;
            drawKeyCap(g, CONTROLS[i][0], 62, y - 18);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.setColor(MenuStyle.TEXT);
            g.drawString(CONTROLS[i][1], 152, y);
        }

        // ===== ขวา: เป้าหมาย / ของ / ศัตรู =====
        MenuStyle.drawPanel(g, 380, 120, 348, 400);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(MenuStyle.ACCENT);
        g.drawString("DUNGEON GUIDE", 402, 150);

        int y = 172;
        // เป้าหมาย
        drawDoorKeyIcon(g, 402, y + 4);
        drawRow(g, y, "Defeat every enemy", "The last one drops the EXIT key");
        y += 50;
        // หีบ
        if (TileSet.isLoaded()) {
            TileSet.draw(g, TileSet.CHEST_CLOSED, 400, y - 2, 32);
        }
        drawRow(g, y, "Treasure chest", "Open with a chest key: potion or skill");
        y += 50;
        // กุญแจหีบ
        ChestKey.drawShape(g, 402, y + 6, 28);
        drawRow(g, y, "Chest key", "Dropped by enemies marked with a key");
        y += 50;
        // ยา
        if (TileSet.isLoaded()) {
            TileSet.draw(g, TileSet.POTION_RED, 396, y - 4, 20);
            TileSet.draw(g, TileSet.POTION_GREEN, 414, y - 4, 20);
        }
        drawRow(g, y, "Potions", "Red = +2 HP     Green = +60 stamina");
        y += 50;
        // สกิล
        if (TileSet.isLoaded()) {
            TileSet.draw(g, TileSet.SHIELD, 398, y - 4, 20);
            TileSet.draw(g, TileSet.SWORD, 414, y - 4, 20);
        }
        drawRow(g, y, "Skills (from chests)", "Q blocks 1 hit / E spins ice blades");
        y += 50;
        // วงแดง
        g.setColor(new Color(255, 40, 40, 90));
        g.fillOval(400, y - 2, 30, 30);
        g.setColor(new Color(255, 70, 70));
        g.drawOval(400, y - 2, 30, 30);
        g.drawOval(408, y + 6, 14, 14);
        drawRow(g, y, "Red warning", "Enemy is about to strike - move or dash!");
        y += 50;
        // เส้นเล็ง
        g.setColor(new Color(255, 70, 70));
        for (int i = 0; i < 4; i++) {
            g.drawLine(400 + i * 9, y + 22 - i * 6, 405 + i * 9, y + 19 - i * 6);
        }
        drawRow(g, y, "Red aim line", "A shot is coming along that line");
    }

    private void drawRow(Graphics g, int y, String title, String text) {
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(MenuStyle.TEXT);
        g.drawString(title, 446, y + 12);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(MenuStyle.TEXT_DIM);
        g.drawString(text, 446, y + 30);
    }

    private void drawKeyCap(Graphics g, String key, int x, int y) {
        g.setFont(new Font("Arial", Font.BOLD, 12));
        int width = Math.max(34, g.getFontMetrics().stringWidth(key) + 18);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(x, y + 3, width, 24, 8, 8);
        g.setColor(new Color(220, 232, 255));
        g.fillRoundRect(x, y, width, 24, 8, 8);
        g.setColor(new Color(20, 30, 60));
        g.drawString(key, x + (width - g.getFontMetrics().stringWidth(key)) / 2, y + 16);
    }

    private void drawDoorKeyIcon(Graphics g, int x, int y) {
        g.setColor(new Color(255, 210, 70));
        g.fillOval(x, y, 16, 16);
        g.fillRect(x + 14, y + 6, 16, 5);
        g.fillRect(x + 25, y + 10, 3, 6);
        g.setColor(new Color(90, 60, 10));
        g.fillOval(x + 5, y + 5, 6, 6);
    }
}
