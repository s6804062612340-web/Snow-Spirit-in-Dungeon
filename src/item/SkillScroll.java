package item;

import java.awt.*;

import entity.DamageText;
import main.Camera;
import main.GamePanel;
import skill.Skill;
import sound.Sound;

/**
 * ม้วนสกิล: ได้จากหีบสมบัติ เดินชนแล้วเรียนสกิล (หรืออัปเลเวลถ้ามีสกิลนี้อยู่แล้ว)
 * Composition: ม้วนสกิล "มี" Skill อยู่ข้างใน แล้วส่งต่อให้ Player
 */
public class SkillScroll extends Item {

    public static final int SIZE = 40;

    private final Skill skill;
    private int age = 0;

    public SkillScroll(GamePanel gamePanel, int centerX, int centerY, Skill skill) {
        super(gamePanel, centerX - SIZE / 2, centerY - SIZE / 2, SIZE, SIZE);
        this.skill = skill;
    }

    @Override
    public void update() {
        if (collected) {
            return;
        }
        age++;
        updateHitbox();

        if (isPlayerTouching()) {
            collected = true;
            boolean upgraded = gamePanel.getPlayer().learnSkill(skill);
            String text = upgraded
                    ? skill.getName() + " Lv" + gamePanel.getPlayer().getSkill(skill.getSlot()).getLevel() + "!"
                    : "NEW SKILL: " + skill.getName() + " [" + skill.getKeyLabel() + "]";
            gamePanel.addDamageText(new DamageText(gamePanel, text, new Color(150, 230, 255),
                    gamePanel.getPlayer().getCenterX(), gamePanel.getPlayer().getY() - 10));
            gamePanel.showToast(text, skill.getDescription());
            gamePanel.getEffects().ring(x + width / 2, y + height / 2, 90, new Color(150, 230, 255), 22);
            gamePanel.getEffects().sparkle(x + width / 2, y + height / 2, 24, new Color(200, 240, 255));
            Sound.SKILL_LEARN.play();
        }
    }

    @Override
    public void draw(Graphics g) {
        if (collected) {
            return;
        }
        Camera camera = gamePanel.getCamera();
        int sx = camera.screenX(x);
        int sy = camera.screenY(y) + (int) (4 * Math.sin(age * 0.08));

        // วงแสงหมุน
        int glow = SIZE / 2 + 10 + (int) (4 * Math.sin(age * 0.15));
        g.setColor(new Color(130, 210, 255, 70));
        g.fillOval(sx + SIZE / 2 - glow, sy + SIZE / 2 - glow, glow * 2, glow * 2);
        g.setColor(new Color(200, 240, 255, 180));
        g.drawOval(sx + SIZE / 2 - glow, sy + SIZE / 2 - glow, glow * 2, glow * 2);

        skill.drawIcon(g, sx, sy, SIZE);

        if (gamePanel.isDebugMode()) {
            g.setColor(Color.WHITE);
            g.drawRect(camera.screenX(hitbox.x), camera.screenY(hitbox.y), hitbox.width, hitbox.height);
        }
    }

    public Skill getSkill() {
        return skill;
    }
}
