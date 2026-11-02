package menu;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import sound.Sound;

/**
 * หน้า Setting: God Mode, Debug Mode, เสียง  (เลือกระดับความยากย้ายไปอยู่หน้าเมนูแรกแล้ว)
 * ค่าจะถูกใช้ตอนกด START ครั้งถัดไป
 *
 * คลาสนี้ implements ActionListener เอง สำหรับปุ่มสลับค่า
 * ส่วนปุ่ม BBack ให้ Main ดักจับเพื่อกลับหน้าเมนู
 */
public class SettingPanel extends JPanel implements ActionListener {

    // ค่า Setting (Encapsulation: อ่านผ่าน getter)
    private boolean godMode = false;
    private boolean debugMode = false;

    private final MenuButton BGodMode = new MenuButton("", new Color(255, 170, 60));
    private final MenuButton BDebug = new MenuButton("", new Color(90, 200, 255));
    private final MenuButton BSound = new MenuButton("", new Color(120, 230, 160));
    public JButton BBack = new MenuButton("BACK", MenuStyle.ACCENT);

    private int frame = 0;
    private final Timer animationTimer = new Timer(33, this);

    // =========================
    // Constructor
    // =========================
    public SettingPanel() {
        setPreferredSize(new Dimension(MenuStyle.WIDTH, MenuStyle.HEIGHT));
        setLayout(null);

        BGodMode.setBounds(214, 190, 340, 56);
        BDebug.setBounds(214, 278, 340, 56);
        BSound.setBounds(214, 366, 340, 56);
        BBack.setBounds(294, 490, 180, 50);

        BGodMode.addActionListener(this);
        BDebug.addActionListener(this);
        BSound.addActionListener(this);

        add(BGodMode);
        add(BDebug);
        add(BSound);
        add(BBack);

        updateButtons();
        animationTimer.start();
    }

    // =========================
    // Button Events
    // =========================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == animationTimer) {
            frame++;
            repaint();
            return;
        }
        if (e.getSource() == BGodMode) {
            godMode = !godMode;
        } else if (e.getSource() == BDebug) {
            debugMode = !debugMode;
        } else if (e.getSource() == BSound) {
            Sound.setEnabled(!Sound.isEnabled());
        }
        Sound.CLICK.play();
        updateButtons();
    }

    private void updateButtons() {
        BGodMode.setText("GOD MODE :  " + (godMode ? "ON" : "OFF"));
        BDebug.setText("DEBUG MODE :  " + (debugMode ? "ON" : "OFF"));
        BSound.setText("SOUND :  " + (Sound.isEnabled() ? "ON" : "OFF"));
        BGodMode.setHighlighted(godMode);
        BDebug.setHighlighted(debugMode);
        BSound.setHighlighted(Sound.isEnabled());
    }

    // =========================
    // Draw
    // =========================
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        MenuStyle.drawBackground(g, getWidth(), getHeight(), frame);
        MenuStyle.drawTitle(g, "SETTING", 110, getWidth());

        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(MenuStyle.TEXT_DIM);
        MenuStyle.drawCentered(g, "HP 1000, no stamina cost, open doors and chests without keys", 266, getWidth());
        MenuStyle.drawCentered(g, "Show every hitbox", 354, getWidth());
        MenuStyle.drawCentered(g, "8-bit sound effects", 442, getWidth());
    }

    // =========================
    // Getters
    // =========================
    public boolean isGodMode() { return godMode; }
    public boolean isDebugMode() { return debugMode; }
}
