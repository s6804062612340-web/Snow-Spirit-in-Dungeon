package sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * เสียง Effect 1 เสียง สังเคราะห์ขึ้นด้วยโค้ด (ไม่ต้องใช้ไฟล์เสียง)
 *
 * เสียง 1 เสียงประกอบด้วย "โน้ต" หลายท่อนต่อกัน แต่ละท่อนกำหนด:
 *   ความถี่เริ่ม -> ความถี่จบ (เสียงไถลขึ้น/ลง), ความยาว (ms), รูปคลื่น, ความดัง
 *
 * ใช้ Clip ของ javax.sound.sampled (เหมือนในเอกสาร) เตรียมไว้หลายตัว
 * เพื่อให้เล่นเสียงเดียวกันซ้อนกันได้ (เช่น ยิงรัว ๆ)
 */
public class SoundEffect {

    // รูปคลื่น
    public static final int SQUARE = 0;     // เสียงแบบเกม 8-bit
    public static final int TRIANGLE = 1;   // นุ่มกว่า
    public static final int NOISE = 2;      // เสียงซ่า เช่น ระเบิด / โดนตี
    public static final int SINE = 3;       // ใส ๆ เช่น เก็บของ

    private static final float SAMPLE_RATE = 22050f;
    private static final int COPIES = 3;    // เล่นซ้อนกันได้สูงสุด 3 เสียง

    private final byte[] data;
    private Clip[] clips;
    private int nextClip = 0;

    // =========================
    // Constructor
    // =========================
    /**
     * @param notes แต่ละแถว = { ความถี่เริ่ม, ความถี่จบ, ความยาว ms, รูปคลื่น, ความดัง 0-100 }
     */
    public SoundEffect(int[][] notes) {
        data = render(notes);
    }

    // =========================
    // สร้างคลื่นเสียง (16-bit, mono)
    // =========================
    private byte[] render(int[][] notes) {
        int totalSamples = 0;
        for (int[] note : notes) {
            totalSamples += (int) (SAMPLE_RATE * note[2] / 1000);
        }

        byte[] bytes = new byte[totalSamples * 2];
        int index = 0;
        double phase = 0;
        int noiseValue = 0;

        for (int[] note : notes) {
            int samples = (int) (SAMPLE_RATE * note[2] / 1000);
            int wave = note[3];
            double volume = note[4] / 100.0 * 0.45;

            for (int i = 0; i < samples; i++) {
                double t = (double) i / samples;
                double freq = note[0] + (note[1] - note[0]) * t;
                phase += freq / SAMPLE_RATE;
                phase -= Math.floor(phase);

                double value;
                if (wave == SQUARE) {
                    value = phase < 0.5 ? 1 : -1;
                } else if (wave == TRIANGLE) {
                    value = 4 * Math.abs(phase - 0.5) - 1;
                } else if (wave == NOISE) {
                    // สุ่มค่าใหม่ทุก ๆ ไม่กี่ sample -> ได้เสียงซ่าที่มีระดับเสียง
                    if (i % Math.max(1, (int) (SAMPLE_RATE / Math.max(freq, 1) / 2)) == 0) {
                        noiseValue = (int) (Math.random() * 2000) - 1000;
                    }
                    value = noiseValue / 1000.0;
                } else {
                    value = Math.sin(phase * 2 * Math.PI);
                }

                // Envelope: เสียงค่อย ๆ เบาลงตอนท้าย และเข้าเร็วตอนต้น (กันเสียงคลิก)
                double attack = Math.min(1, i / (SAMPLE_RATE * 0.003));
                double decay = 1 - t * 0.85;
                short sample = (short) (value * volume * attack * decay * Short.MAX_VALUE);

                bytes[index++] = (byte) (sample & 0xFF);
                bytes[index++] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        return bytes;
    }

    // =========================
    // Play
    // =========================
    public void play() {
        if (!Sound.isEnabled()) {
            return;
        }
        if (clips == null) {
            openClips();
            if (clips == null) {
                return;     // เครื่องนี้เล่นเสียงไม่ได้ -> เงียบ ไม่ทำให้เกมพัง
            }
        }

        Clip clip = clips[nextClip];
        nextClip = (nextClip + 1) % clips.length;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /** เตรียม Clip ครั้งแรกที่เล่น (ถ้าไม่มีอุปกรณ์เสียง จะปิดเสียงทั้งเกม) */
    private void openClips() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            Clip[] newClips = new Clip[COPIES];
            for (int i = 0; i < COPIES; i++) {
                newClips[i] = AudioSystem.getClip();
                newClips[i].open(format, data, 0, data.length);
            }
            clips = newClips;
        } catch (Exception e) {
            System.out.println("Sound disabled: " + e.getMessage());
            Sound.setEnabled(false);
        }
    }

    /** จำนวน byte ของเสียง (ใช้ตอนทดสอบ) */
    public int getLength() {
        return data.length;
    }
}
