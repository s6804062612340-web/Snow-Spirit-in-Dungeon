package skill;

import java.awt.*;

import entity.Player;
import main.GamePanel;

/**
 * คลาสแม่ (Abstract) ของสกิลทุกตัว
 * ได้สกิลจากหีบสมบัติ แล้วกดปุ่มประจำช่องเพื่อใช้ (Q = ช่อง 0, E = ช่อง 1)
 *
 * ทุกสกิลมีเหมือนกัน: ชื่อ, ปุ่ม, ค่า Stamina ที่ใช้, คูลดาวน์, เลเวล
 * ส่วน "ใช้แล้วเกิดอะไร" ให้คลาสลูกเขียนเองใน activate() (Polymorphism)
 *
 * tryUse() เป็นขั้นตอนตายตัวที่ทุกสกิลใช้ร่วมกัน (ตรวจคูลดาวน์ -> หัก Stamina -> activate)
 * คลาสลูกเปลี่ยนได้แค่ขั้น activate() เท่านั้น
 */
public abstract class Skill {

    protected final GamePanel gamePanel;

    private final String name;
    private final String keyLabel;      // ปุ่มที่กดใช้ เช่น "Q"
    private final int slot;             // ช่องในแถบสกิล (0 = Q, 1 = E)
    private final int staminaCost;
    private final int cooldownFrames;   // 60 เฟรม = 1 วินาที
    private final int maxLevel;

    protected int level = 1;
    protected int cooldownTimer = 0;    // > 0 = ยังใช้ไม่ได้

    // =========================
    // Constructor
    // =========================
    public Skill(GamePanel gamePanel, String name, String keyLabel, int slot,
                 int staminaCost, int cooldownFrames, int maxLevel) {
        this.gamePanel = gamePanel;
        this.name = name;
        this.keyLabel = keyLabel;
        this.slot = slot;
        this.staminaCost = staminaCost;
        this.cooldownFrames = cooldownFrames;
        this.maxLevel = maxLevel;
    }

    // =========================
    // ใช้สกิล
    // =========================
    /**
     * พยายามใช้สกิล: ติดคูลดาวน์ / กำลังทำงานอยู่ / Stamina ไม่พอ = ใช้ไม่ได้
     * @return true ถ้าใช้สำเร็จ
     */
    public final boolean tryUse(Player player) {
        if (isActive() || cooldownTimer > 0) {
            return false;
        }
        if (!player.useStamina(staminaCost)) {
            return false;
        }
        activate(player);
        return true;
    }

    /** ผลของสกิล (คลาสลูกเขียนเอง) */
    protected abstract void activate(Player player);

    /** สกิลกำลังทำงานอยู่หรือไม่ (เช่น โล่ยังกางอยู่ / ดาบยังหมุนอยู่) */
    public abstract boolean isActive();

    /** คำอธิบายสั้น ๆ (แสดงตอนเก็บสกิล และในหน้า HOW TO PLAY) */
    public abstract String getDescription();

    /** วาดไอคอนสกิลขนาด size x size ที่ตำแหน่งบนจอ (ใช้ในแถบสกิล / ตอนตกบนพื้น) */
    public abstract void drawIcon(Graphics g, int x, int y, int size);

    // =========================
    // Overridable
    // =========================
    /** ทุกเฟรม: นับคูลดาวน์ (นับเฉพาะตอนสกิลไม่ได้ทำงาน) */
    public void update(Player player) {
        if (!isActive() && cooldownTimer > 0) {
            cooldownTimer--;
        }
    }

    /** วาด Effect ของสกิลในโลกเกม (ค่าเริ่มต้น = ไม่วาดอะไร) */
    public void draw(Graphics g, Player player) {
    }

    /** ถูกโจมตี: สกิลกันดาเมจได้หรือไม่ (ค่าเริ่มต้น = ไม่กัน, ShieldSkill Override) */
    public boolean absorbHit(Player player) {
        return false;
    }

    // =========================
    // Level / Cooldown Helpers
    // =========================
    public boolean canUpgrade() {
        return level < maxLevel;
    }

    public void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }

    protected void startCooldown() {
        cooldownTimer = cooldownFrames;
    }

    /** 1 = เพิ่งเริ่มคูลดาวน์, 0 = พร้อมใช้ */
    public double getCooldownRatio() {
        return cooldownFrames == 0 ? 0 : (double) cooldownTimer / cooldownFrames;
    }

    // =========================
    // Getters
    // =========================
    public String getName() { return name; }
    public String getKeyLabel() { return keyLabel; }
    public int getSlot() { return slot; }
    public int getStaminaCost() { return staminaCost; }
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }
    public int getCooldownTimer() { return cooldownTimer; }
    public boolean isReady() { return !isActive() && cooldownTimer == 0; }
}
