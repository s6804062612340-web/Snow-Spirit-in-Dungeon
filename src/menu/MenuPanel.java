package menu;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import sound.Sound;
import world.Difficulty;

/**
 * หน้าเมนูแรกของเกม: เลือกโหมด (EASY / NORMAL / HARD) แล้วกด START ได้ทันที
 * (แบบเดียวกับคลาส homegames ในเอกสาร: JPanel + JButton + setLayout(null))
 *
 *   ปุ่ม BStart / BHelp / BSetting / BExit เป็น public -> Main (JFrame) ดักจับเพื่อสลับหน้าจอ
 *   ปุ่มการ์ดโหมด 3 ใบ -> MenuPanel ดักจับเอง (implements ActionListener) แล้วจำโหมดที่เลือก
 *
 * javax.swing.Timer (อยู่ใน javax.swing.*) สั่ง repaint ทุก 33 ms ให้หิมะตกและตัวละครขยับ
 */
public class MenuPanel extends JPanel implements ActionListener {

    private final ImageIcon character = new ImageIcon("assets/Snow_spirit.png");

    public JButton BStart = new MenuButton("START", new Color(120, 230, 160));
    public JButton BHelp = new MenuButton("HOW TO PLAY", MenuStyle.ACCENT);
    public JButton BSetting = new MenuButton("SETTING", MenuStyle.ACCENT);
    public JButton BExit = new MenuButton("EXIT", new Color(255, 120, 120));

    // การ์ดเลือกโหมด (1 ใบต่อ 1 ระดับความยาก)
    private final MenuButton[] modeCards = new MenuButton[Difficulty.values().length];
    private Difficulty difficulty = Difficulty.EASY;

    private int frame = 0;
    private final Timer animationTimer;

    // =========================
    // Constructor
    // =========================
    public MenuPanel() {
        setPreferredSize(new Dimension(MenuStyle.WIDTH, MenuStyle.HEIGHT));
        setLayout(null);

        // การ์ดโหมด 3 ใบ
        Difficulty[] modes = Difficulty.values();
        int cardWidth = 196;
        int gap = 18;
        int startX = (MenuStyle.WIDTH - (cardWidth * modes.length + gap * (modes.length - 1))) / 2;
        for (int i = 0; i < modes.length; i++) {
            Difficulty mode = modes[i];
            MenuButton card = new MenuButton(mode.getLabel(), mode.getColor());
            card.setTitleSize(22);
            card.setLines("Player HP " + mode.getPlayerMaxHp() + "   Enemy HP x" + mode.getEnemyHpMultiplier(),
                    "+" + mode.getExtraEnemies() + " enemies   Boss warp " + (mode.getBossTeleportDelay() / 60) + "s",
                    mode.getTagline());
            card.setBounds(startX + i * (cardWidth + gap), 292, cardWidth, 120);
            card.addActionListener(this);
            modeCards[i] = card;
            add(card);
        }
        updateCards();

        // ปุ่มหลัก
        ((MenuButton) BStart).setTitleSize(24);
        BStart.setBounds((MenuStyle.WIDTH - 260) / 2, 430, 260, 58);
        BHelp.setBounds(134, 502, 160, 46);
        BSetting.setBounds(304, 502, 160, 46);
        BExit.setBounds(474, 502, 160, 46);
        add(BStart);
        add(BHelp);
        add(BSetting);
        add(BExit);

        animationTimer = new Timer(33, this);
        animationTimer.start();
    }

    // =========================
    // Events: เลือกโหมด + จังหวะ Animation
    // =========================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == animationTimer) {
            frame++;
            repaint();
            return;
        }
        for (int i = 0; i < modeCards.length; i++) {
            if (e.getSource() == modeCards[i]) {
                difficulty = Difficulty.values()[i];
                Sound.CLICK.play();
                updateCards();
            }
        }
    }

    private void updateCards() {
        for (int i = 0; i < modeCards.length; i++) {
            modeCards[i].setHighlighted(Difficulty.values()[i] == difficulty);
        }
        BStart.setText("START  " + difficulty.getLabel());
    }

    // =========================
    // Draw
    // =========================
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        MenuStyle.drawBackground(g, getWidth(), getHeight(), frame);

        // ตัวละครเดินอยู่กับที่ (สลับ 4 เฟรมของแถวแรกใน Sprite Sheet) + ลอยขึ้นลง
        int column = (frame / 6) % 4;
        int bob = (int) (5 * Math.sin(frame * 0.08));
        int cx = getWidth() / 2;
        g.setColor(new Color(140, 200, 255, 40));
        g.fillOval(cx - 70, 150 + bob, 140, 120);
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(cx - 40, 262, 80, 14);
        g.drawImage(character.getImage(),
                cx - 60, 150 + bob, cx + 60, 270 + bob,
                column * 128, 0, column * 128 + 128, 128, this);

        MenuStyle.drawTitle(g, "SNOW SPIRIT", 82, getWidth());
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(MenuStyle.TEXT_DIM);
        MenuStyle.drawCentered(g, "~  in Dungeon  ~", 124, getWidth());

        MenuStyle.drawSectionLabel(g, "SELECT MODE", 282, getWidth());

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(170, 190, 225));
        MenuStyle.drawCentered(g, "WASD Move  •  Mouse Aim  •  Click Shoot  •  SPACE Dash  •  Q / E Skills  •  1-5 Potions",
                getHeight() - 30, getWidth());
    }

    // =========================
    // Getters
    // =========================
    public Difficulty getDifficulty() {
        return difficulty;
    }
}
