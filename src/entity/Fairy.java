package entity;

import java.awt.*;
import javax.swing.*;

import main.GamePanel;

/**
 * ภูตตัวเล็กที่ลอยรอบ Player ตามทิศเมาส์
 * กระสุนของ Player จะยิงออกจากตำแหน่งของ Fairy
 *
 * รูป assets/Fairy.png = 4 เฟรมเรียงแนวนอน เฟรมละ 32x32 (กระพือปีก)
 * Hitbox = กรอบรูปที่วาด (32x32)
 */
public class Fairy {

    // =========================
    // Image
    // =========================
    private final ImageIcon spriteSheet = new ImageIcon("assets/Fairy.png");
    private static final int FRAME_SIZE = 32;       // ขนาด 1 เฟรมในไฟล์
    private static final int FRAME_COUNT = 4;
    private static final int FRAME_DELAY = 8;       // เปลี่ยนเฟรมทุก 8 เฟรมเกม

    private int frame = 0;
    private int frameCounter = 0;

    // =========================
    // Position
    // =========================
    private final GamePanel gamePanel;
    private final Player player;

    private double x;   // มุมซ้ายบนของ Hitbox
    private double y;

    private final int size = 32;            // ขนาดรูป = ขนาด Hitbox
    private final int minRadius = 40;       // ระยะห่างจากกลางตัว Player
    private final int maxRadius = 76;
    private final int orbitRadius = 68;

    // ทิศทางการเล็ง (Unit Vector)
    private double aimX = 1;
    private double aimY = 0;

    private final Rectangle hitbox;

    // =========================
    // Constructor
    // =========================
    public Fairy(GamePanel gamePanel, Player player) {
        this.gamePanel = gamePanel;
        this.player = player;

        hitbox = new Rectangle(0, 0, size, size);

        x = player.getCenterX() + orbitRadius - size / 2.0;
        y = player.getCenterY() - size / 2.0;
    }

    // =========================
    // Update
    // =========================
    public void update(int mouseScreenX, int mouseScreenY) {
        // Mouse: Screen -> World
        double mouseWorldX = mouseScreenX + gamePanel.getCamera().getX();
        double mouseWorldY = mouseScreenY + gamePanel.getCamera().getY();

        // จุดกลางลำตัวของ Player
        double playerCenterX = player.getCenterX();
        double playerCenterY = player.getCenterY();

        // Vector Player -> Mouse
        double dx = mouseWorldX - playerCenterX;
        double dy = mouseWorldY - playerCenterY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // กันกรณี Mouse อยู่ตรงกลาง Player
        if (distance == 0) {
            dx = 1;
            dy = 0;
            distance = 1;
        }

        double dirX = dx / distance;
        double dirY = dy / distance;

        moveAroundPlayer(playerCenterX, playerCenterY, dirX, dirY);
        updateAim(mouseWorldX, mouseWorldY);
        updateAnimation();

        hitbox.x = (int) x;
        hitbox.y = (int) y;
    }

    /** วาง Fairy รอบ Player โดยไม่ให้ทะลุกำแพง */
    private void moveAroundPlayer(double centerX, double centerY, double dirX, double dirY) {
        double targetX = centerX + dirX * orbitRadius - size / 2.0;
        double targetY = centerY + dirY * orbitRadius - size / 2.0;

        if (isSafePosition(targetX, targetY)) {
            x = targetX;
            y = targetY;
            return;
        }

        // ชนกำแพง -> ลองหดระยะเข้าหา Player
        for (int r = maxRadius; r >= minRadius; r -= 2) {
            double testX = centerX + dirX * r - size / 2.0;
            double testY = centerY + dirY * r - size / 2.0;

            if (isSafePosition(testX, testY)) {
                x = testX;
                y = testY;
                return;
            }
        }

        // ไม่มีที่ปลอดภัยเลย -> อยู่กลางตัว Player
        x = centerX - size / 2.0;
        y = centerY - size / 2.0;
    }

    /** คำนวณทิศเล็งจาก Fairy -> Mouse */
    private void updateAim(double mouseWorldX, double mouseWorldY) {
        double aimDX = mouseWorldX - getCenterX();
        double aimDY = mouseWorldY - getCenterY();
        double aimDistance = Math.sqrt(aimDX * aimDX + aimDY * aimDY);

        if (aimDistance > 0) {
            aimX = aimDX / aimDistance;
            aimY = aimDY / aimDistance;
        }
    }

    private void updateAnimation() {
        frameCounter++;
        if (frameCounter > FRAME_DELAY) {
            frame = (frame + 1) % FRAME_COUNT;
            frameCounter = 0;
        }
    }

    private boolean isSafePosition(double testX, double testY) {
        return !gamePanel.getMap().isWallCollision((int) testX, (int) testY, size, size);
    }

    // =========================
    // Draw
    // =========================
    public void draw(Graphics g) {
        // รูปวาดตรงกับ Hitbox พอดี
        int screenX = gamePanel.getCamera().screenX((int) x);
        int screenY = gamePanel.getCamera().screenY((int) y);

        if (spriteSheet.getImageLoadStatus() == MediaTracker.COMPLETE) {
            int sx = frame * FRAME_SIZE;
            g.drawImage(spriteSheet.getImage(),
                    screenX, screenY, screenX + size, screenY + size,
                    sx, 0, sx + FRAME_SIZE, FRAME_SIZE,
                    null);
        } else {
            // ไม่พบรูป -> วาดวงกลมแทน
            g.setColor(Color.CYAN);
            g.fillOval(screenX, screenY, size, size);
            g.setColor(Color.WHITE);
            g.drawOval(screenX, screenY, size, size);
        }

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.CYAN);
            g.drawRect(
                    gamePanel.getCamera().screenX(hitbox.x),
                    gamePanel.getCamera().screenY(hitbox.y),
                    hitbox.width, hitbox.height);
        }
    }

    // =========================
    // Getters
    // =========================
    public double getCenterX() { return x + size / 2.0; }
    public double getCenterY() { return y + size / 2.0; }
    public double getAimX() { return aimX; }
    public double getAimY() { return aimY; }
}
