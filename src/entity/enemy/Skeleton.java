package entity.enemy;

import java.awt.*;

import main.GamePanel;

/**
 * Skeleton: ศัตรูที่อึดกว่าและเร็วกว่า Slime รูปและ Hitbox 64x64 (เท่ากับ 1 เฟรมในไฟล์ Skeleton.png)
 *
 * มี 2 ประเภท:
 *   MELEE  - HP 4, speed 2, วิ่งเข้าใกล้ -> วงแดงเตือน 0.57 วินาที -> ฟาด (ดาเมจ 1) -> คูลดาวน์
 *   RANGED - HP 3, speed 2, เว้นระยะ 260-380px แล้วปากระดูก (เร็ว ลูกเล็ก) ทุก ~1.7 วินาที
 *            มีวงสีครีมใต้ตัวไว้แยกกับแบบตีใกล้
 */
public class Skeleton extends Enemy {

    public static final int SIZE = 64;

    // =========================
    // Constructor
    // =========================
    public Skeleton(GamePanel gamePanel, CombatStyle style) {
        super(gamePanel, style == CombatStyle.RANGED ? 3 : 4, 2, SIZE);
        this.style = style;
        loadSprite("assets/Skeleton.png");

        meleeRadius = 92;          // วงแดงรอบตัว
        meleeWindupFrames = 34;    // เตือน 0.57 วินาที
        meleeDelay = 70;           // ฟาดได้ทุก ~1.7 วินาที

        if (style == CombatStyle.RANGED) {
            keepDistanceMin = 260;
            keepDistanceMax = 380;
            shootDelay = 100;
            projectileSpeed = 7.5;
            projectileSize = 12;
            projectileColor = new Color(240, 225, 180);
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
        return new Color(235, 230, 215);
    }

    // =========================
    // Draw
    // =========================
    @Override
    public void draw(Graphics g) {
        if (!alive) {
            return;
        }

        drawBody(g, Color.GRAY);      // เงา + ตัว (กระพริบตอนอมตะ) + แสงเตือนก่อนยิง
        drawHPBar(g);
        drawChestKeyMark(g);
        drawHitbox(g);
    }
}
