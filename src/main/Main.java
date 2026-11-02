package main;

import java.awt.event.*;
import javax.swing.*;

import menu.HelpPanel;
import menu.MenuPanel;
import menu.SettingPanel;
import sound.Sound;

/**
 * หน้าต่างหลักของเกม (แบบเดียวกับคลาส PlayGames ในเอกสาร)
 * extends JFrame + implements ActionListener เพื่อสลับหน้าจอเมื่อกดปุ่ม
 *
 *   MenuPanel  (เลือกโหมด EASY / NORMAL / HARD)  --START-->   GamePanel
 *   MenuPanel  --HOW TO PLAY--> HelpPanel     --BACK--> MenuPanel
 *   MenuPanel  --SETTING-->     SettingPanel  --BACK--> MenuPanel
 *   GamePanel  --MENU / ESC-->  MenuPanel
 *   MenuPanel  --EXIT-->        ปิดโปรแกรม
 */
public class Main extends JFrame implements ActionListener {

    private final MenuPanel menuPanel = new MenuPanel();
    private final SettingPanel settingPanel = new SettingPanel();
    private final HelpPanel helpPanel = new HelpPanel();
    private final GamePanel gamePanel = new GamePanel();

    // =========================
    // Constructor
    // =========================
    public Main() {
        setTitle("Snow Spirit in Dungeon");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        menuPanel.BStart.addActionListener(this);
        menuPanel.BHelp.addActionListener(this);
        menuPanel.BSetting.addActionListener(this);
        menuPanel.BExit.addActionListener(this);
        settingPanel.BBack.addActionListener(this);
        helpPanel.BBack.addActionListener(this);
        gamePanel.BMenu.addActionListener(this);

        add(menuPanel);
        pack();
        setLocationRelativeTo(null);

        gamePanel.startGameThread();
    }

    // =========================
    // Button Events
    // =========================
    @Override
    public void actionPerformed(ActionEvent e) {
        Sound.CLICK.play();

        if (e.getSource() == menuPanel.BStart) {
            gamePanel.newGame(settingPanel.isGodMode(), settingPanel.isDebugMode(), menuPanel.getDifficulty());
            showScreen(gamePanel);
            gamePanel.requestFocusInWindow();

        } else if (e.getSource() == menuPanel.BHelp) {
            showScreen(helpPanel);

        } else if (e.getSource() == menuPanel.BSetting) {
            showScreen(settingPanel);

        } else if (e.getSource() == settingPanel.BBack || e.getSource() == helpPanel.BBack) {
            showScreen(menuPanel);

        } else if (e.getSource() == gamePanel.BMenu) {
            gamePanel.stopGame();
            showScreen(menuPanel);

        } else if (e.getSource() == menuPanel.BExit) {
            System.exit(0);
        }
    }

    /** เปลี่ยนหน้าจอ: เอาหน้าเดิมออก แล้วใส่หน้าใหม่ */
    private void showScreen(JPanel panel) {
        getContentPane().removeAll();
        add(panel);
        revalidate();
        repaint();
    }

    // =========================
    // Main
    // =========================
    public static void main(String[] args) {
        Main window = new Main();
        window.setVisible(true);
    }
}
