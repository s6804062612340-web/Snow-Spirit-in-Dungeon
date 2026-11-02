package skill;

import java.awt.*;

import entity.DamageText;
import entity.Player;
import main.Camera;
import main.GamePanel;
import sound.Sound;
import world.TileSet;

/**
 * สกิลโล่น้ำแข็ง [Q]
 * กดใช้ -> โล่กางรอบตัวจนกว่าจะโดนโจมตี 1 ครั้ง (กันดาเมจครั้งนั้นได้ทั้งหมด)
 * โล่แตก -> ติดคูลดาวน์ 6 วินาที แล้วกดกางใหม่ได้
 */
public class ShieldSkill extends Skill {

    private static final int STAMINA_COST = 30;
    private static final int COOLDOWN = 360;        // 6 วินาที (นับหลังโล่แตก)
    private static final int RADIUS = 66;

    private boolean active = false;
    private int pulse = 0;

    public ShieldSkill(GamePanel gamePanel) {
        super(gamePanel, "ICE SHIELD", "Q", 0, STAMINA_COST, COOLDOWN, 1);
    }

    @Override
    protected void activate(Player player) {
        active = true;
        gamePanel.getEffects().ring(player.getCenterX(), player.getCenterY(), RADIUS + 20, new Color(150, 220, 255), 16);
        gamePanel.getEffects().sparkle(player.getCenterX(), player.getCenterY(), 12, new Color(180, 235, 255));
        Sound.SHIELD_UP.play();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /** โดนโจมตีตอนโล่กางอยู่ -> โล่รับแทน แล้วแตก */
    @Override
    public boolean absorbHit(Player player) {
        if (!active) {
            return false;
        }
        active = false;
        startCooldown();

        int cx = player.getCenterX();
        int cy = player.getCenterY();
        gamePanel.getEffects().burst(cx, cy, 24, new Color(170, 230, 255), 6, 9, 22);
        gamePanel.getEffects().ring(cx, cy, RADIUS + 30, Color.WHITE, 14);
        gamePanel.getEffects().shake(4, 8);
        gamePanel.addDamageText(new DamageText(gamePanel, "BLOCK!", new Color(150, 220, 255), cx, player.getY()));
        Sound.SHIELD_BREAK.play();
        return true;
    }

    @Override
    public void update(Player player) {
        super.update(player);
        pulse++;
    }

    /** ฟองน้ำแข็งรอบตัว Player */
    @Override
    public void draw(Graphics g, Player player) {
        if (!active) {
            return;
        }
        Camera camera = gamePanel.getCamera();
        int cx = camera.screenX(player.getCenterX());
        int cy = camera.screenY(player.getCenterY());
        int r = RADIUS + (int) (3 * Math.sin(pulse * 0.12));

        g.setColor(new Color(120, 200, 255, 45));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(170, 230, 255, 190));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.drawOval(cx - r + 3, cy - r + 3, r * 2 - 6, r * 2 - 6);

        // ประกายวิ่งรอบโล่
        for (int i = 0; i < 3; i++) {
            double angle = pulse * 0.05 + i * Math.PI * 2 / 3;
            int sx = cx + (int) (Math.cos(angle) * r);
            int sy = cy + (int) (Math.sin(angle) * r);
            g.setColor(Color.WHITE);
            g.fillOval(sx - 3, sy - 3, 6, 6);
        }
    }

    @Override
    public void drawIcon(Graphics g, int x, int y, int size) {
        if (TileSet.isLoaded()) {
            TileSet.draw(g, TileSet.SHIELD, x, y, size);
            g.setColor(new Color(140, 220, 255, 80));
            g.fillOval(x + size / 6, y + size / 6, size * 2 / 3, size * 2 / 3);
            return;
        }
        g.setColor(new Color(150, 220, 255));
        g.fillOval(x + 4, y + 4, size - 8, size - 8);
        g.setColor(new Color(40, 80, 140));
        g.drawOval(x + 4, y + 4, size - 8, size - 8);
    }

    @Override
    public String getDescription() {
        return "Block 1 hit, then 6s cooldown";
    }
}
