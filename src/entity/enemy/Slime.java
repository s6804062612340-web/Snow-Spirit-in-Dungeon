package entity.enemy;

import java.awt.*;

import main.GamePanel;

/**
 * Slime: ศัตรูพื้นฐาน รูปและ Hitbox 64x64 (เท่ากับ 1 เฟรมในไฟล์ Slime.png)
 *
 * มี 2 ประเภท:
 *   MELEE  - HP 2, เดินเข้าใกล้ -> วงแดงเตือน 0.67 วินาที -> ฟาด (ดาเมจ 1) -> คูลดาวน์
 *   RANGED - HP 2, เว้นระยะ 220-340px แล้วพ่นเมือกสีเขียว (ช้า ลูกใหญ่) ทุก ~2 วินาที
 *            มีวงสีเขียวใต้ตัวไว้แยกกับแบบตีใกล้
 */
public class Slime extends Enemy {

    public static final int SIZE = 64;

    // =========================
    // Constructor
    // =========================
    public Slime(GamePanel gamePanel, CombatStyle style) {
        super(gamePanel, 2, 1, SIZE);   // HP = 2, Speed = 1
        this.style = style;
        loadSprite("assets/Slime.png");

        meleeRadius = 80;          // วงแดงรอบตัว
        meleeWindupFrames = 40;    // เตือน 0.67 วินาที
        meleeDelay = 80;           // ฟาดได้ทุก ~2.0 วินาที

        if (style == CombatStyle.RANGED) {
            keepDistanceMin = 220;
            keepDistanceMax = 340;
            shootDelay = 130;
            projectileSpeed = 6.5;
            projectileSize = 18;
            projectileColor = new Color(110, 230, 90);
        }
    }

    // =========================
    // Update
    // =========================
    @Override
    public void update() {
        if (!alive) {
            return;
        }

        int oldX = x;
        int oldY = y;

        // Polymorphism ภายในคลาส: ประเภทต่างกัน พฤติกรรมต่างกัน
        if (style == CombatStyle.RANGED) {
            keepDistanceAndShoot();
        } else {
            chaseAndAttack();       // เข้าใกล้ -> วงแดงเตือน -> ฟาด -> คูลดาวน์
        }
        checkStuck();               // กันติดกำแพงถาวร

        updateMovementAnimation(oldX, oldY);
        updateAnimation();
        updateInvincibility();
    }

    @Override
    protected Color getDeathColor() {
        return new Color(240, 140, 120);
    }

    // =========================
    // Draw
    // =========================
    @Override
    public void draw(Graphics g) {
        if (!alive) {
            return;
        }

        drawBody(g, Color.RED);      // เงา + ตัว (กระพริบตอนอมตะ) + แสงเตือนก่อนยิง
        drawHPBar(g);
        drawChestKeyMark(g);
        drawHitbox(g);
    }
}
