package effect;

import java.awt.*;
import java.util.ArrayList;

import main.Camera;

/**
 * ตัวจัดการ Effect ภาพทั้งหมดในเกม (Composition: GamePanel "มี" EffectManager)
 * - อนุภาค (ประกาย, ระเบิด, เกล็ดหิมะ)
 * - วงคลื่นกระแทก
 * - แสงวาบเต็มจอ (ตอนโดนตี)
 * - จอสั่น (สั่งผ่าน Camera)
 */
public class EffectManager {

    private final Camera camera;
    private final ArrayList<Particle> particles = new ArrayList<Particle>();
    private final ArrayList<Ring> rings = new ArrayList<Ring>();

    private Color flashColor = null;
    private int flashLife = 0;
    private int flashMaxLife = 1;

    public EffectManager(Camera camera) {
        this.camera = camera;
    }

    // =========================
    // สร้าง Effect
    // =========================
    /** ระเบิดกระจายรอบทิศ */
    public void burst(int x, int y, int count, Color color, double speed, double size, int life) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double power = speed * (0.4 + Math.random() * 0.8);
            particles.add(new Particle(x, y, Math.cos(angle) * power, Math.sin(angle) * power,
                    size * (0.6 + Math.random() * 0.8), color, life + (int) (Math.random() * 10), 0));
        }
    }

    /** ประกายลอยขึ้นช้า ๆ (เก็บของ / ฟื้น HP) */
    public void sparkle(int x, int y, int count, Color color) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    x + (Math.random() - 0.5) * 40, y + (Math.random() - 0.5) * 30,
                    (Math.random() - 0.5) * 0.8, -1 - Math.random() * 1.5,
                    4 + Math.random() * 4, color, 30 + (int) (Math.random() * 20), -0.02));
        }
    }

    /** หางเล็ก ๆ ตามหลังสิ่งที่เคลื่อนที่ (กระสุน / Dash) */
    public void trail(int x, int y, Color color, double size) {
        particles.add(new Particle(x + (Math.random() - 0.5) * 6, y + (Math.random() - 0.5) * 6,
                0, 0, size, color, 14, 0));
    }

    /** ฝุ่นตกลงพื้น (ตอนเดิน Dash / ศัตรูตาย) */
    public void debris(int x, int y, int count, Color color) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, (Math.random() - 0.5) * 6, -2 - Math.random() * 3,
                    5 + Math.random() * 4, color, 35, 0.35));
        }
    }

    public void ring(int x, int y, int maxRadius, Color color, int life) {
        rings.add(new Ring(x, y, maxRadius, color, life));
    }

    /** แสงวาบทั้งจอ */
    public void flash(Color color, int frames) {
        flashColor = color;
        flashLife = frames;
        flashMaxLife = frames;
    }

    /** จอสั่น */
    public void shake(int strength, int frames) {
        camera.shake(strength, frames);
    }

    // =========================
    // Update / Draw
    // =========================
    public void update() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            particles.get(i).update();
            if (particles.get(i).isDead()) {
                particles.remove(i);
            }
        }
        for (int i = rings.size() - 1; i >= 0; i--) {
            rings.get(i).update();
            if (rings.get(i).isDead()) {
                rings.remove(i);
            }
        }
        if (flashLife > 0) {
            flashLife--;
        }
    }

    /** วาด Effect ในโลกเกม (เลื่อนตามกล้อง) */
    public void draw(Graphics g) {
        for (Ring ring : rings) {
            ring.draw(g, camera);
        }
        for (Particle particle : particles) {
            particle.draw(g, camera);
        }
    }

    /** วาดแสงวาบทับทั้งจอ (เรียกหลังวาดทุกอย่าง) */
    public void drawScreenFlash(Graphics g, int width, int height) {
        if (flashLife <= 0 || flashColor == null) {
            return;
        }
        int alpha = flashColor.getAlpha() * flashLife / flashMaxLife;
        g.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), alpha));
        g.fillRect(0, 0, width, height);
    }

    public void clear() {
        particles.clear();
        rings.clear();
        flashLife = 0;
    }

    public int getParticleCount() {
        return particles.size();
    }
}
