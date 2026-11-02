package world;

import java.awt.*;

/**
 * ระดับความยาก (เลือกในหน้า SETTING)
 * EASY = ค่าที่ปรับสมดุลไว้แล้ว / NORMAL, HARD = คูณความยากเพิ่มจาก EASY
 *
 * enum แต่ละตัวเก็บค่าของตัวเอง แล้วเปิดให้อ่านผ่าน getter (Encapsulation)
 */
public enum Difficulty {

    // (ลำดับค่า)  ชื่อ      HP ผู้เล่น  HP ศัตรู  ศัตรูเพิ่ม  ความเร็วเพิ่ม  ความเร็วกระสุน  ยิงถี่ขึ้น  HP Boss  คูลดาวน์วาร์ป  ดรอปยา  HP ตอนผ่านด่าน  สี  คำอธิบาย
    EASY   ("EASY",     7,       1.0,      0,          0,           1.0,           1.0,      1.0,     360,          0.30,   3,
            new Color(110, 220, 140), "Relaxed run - learn the dungeon"),
    NORMAL ("NORMAL",   5,       1.5,      1,          0,           1.2,           0.8,      1.5,     270,          0.20,   1,
            new Color(250, 200, 80), "Fair challenge - more enemies"),
    HARD   ("HARD",     4,       2.0,      2,          1,           1.4,           0.65,     2.0,     180,          0.15,   1,
            new Color(250, 95, 95), "For veterans - fast and deadly");

    private final String label;
    private final int playerMaxHp;
    private final double enemyHpMultiplier;
    private final int extraEnemies;             // เพิ่มทั้ง Slime และ Skeleton อย่างละเท่านี้
    private final int enemySpeedBonus;
    private final double shotSpeedMultiplier;
    private final double shootDelayMultiplier;  // น้อยกว่า 1 = ยิงถี่ขึ้น
    private final double bossHpMultiplier;
    private final int bossTeleportDelay;        // เฟรม (60 = 1 วินาที)
    private final double potionDropChance;
    private final int levelClearHeal;
    private final Color color;                  // สีประจำระดับ (ใช้ในเมนู / HUD)
    private final String tagline;

    Difficulty(String label, int playerMaxHp, double enemyHpMultiplier, int extraEnemies,
               int enemySpeedBonus, double shotSpeedMultiplier, double shootDelayMultiplier,
               double bossHpMultiplier, int bossTeleportDelay, double potionDropChance, int levelClearHeal,
               Color color, String tagline) {
        this.label = label;
        this.playerMaxHp = playerMaxHp;
        this.enemyHpMultiplier = enemyHpMultiplier;
        this.extraEnemies = extraEnemies;
        this.enemySpeedBonus = enemySpeedBonus;
        this.shotSpeedMultiplier = shotSpeedMultiplier;
        this.shootDelayMultiplier = shootDelayMultiplier;
        this.bossHpMultiplier = bossHpMultiplier;
        this.bossTeleportDelay = bossTeleportDelay;
        this.potionDropChance = potionDropChance;
        this.levelClearHeal = levelClearHeal;
        this.color = color;
        this.tagline = tagline;
    }

    /** ระดับถัดไป (ใช้กับปุ่มสลับในหน้า SETTING) EASY -> NORMAL -> HARD -> EASY */
    public Difficulty next() {
        Difficulty[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public String getLabel() { return label; }
    public int getPlayerMaxHp() { return playerMaxHp; }
    public double getEnemyHpMultiplier() { return enemyHpMultiplier; }
    public int getExtraEnemies() { return extraEnemies; }
    public int getEnemySpeedBonus() { return enemySpeedBonus; }
    public double getShotSpeedMultiplier() { return shotSpeedMultiplier; }
    public double getShootDelayMultiplier() { return shootDelayMultiplier; }
    public double getBossHpMultiplier() { return bossHpMultiplier; }
    public int getBossTeleportDelay() { return bossTeleportDelay; }
    public double getPotionDropChance() { return potionDropChance; }
    public int getLevelClearHeal() { return levelClearHeal; }
    public Color getColor() { return color; }
    public String getTagline() { return tagline; }
}
