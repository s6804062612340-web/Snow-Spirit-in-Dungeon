package item;

import java.awt.*;

import entity.Player;
import main.GamePanel;
import world.TileSet;

/**
 * ยาเขียว: ฟื้น Stamina 60 หน่วย (ดื่มไม่ได้ถ้า Stamina เต็ม)
 * ได้จากหีบสมบัติเท่านั้น
 */
public class StaminaPotion extends Potion {

    private final int restoreAmount = 60;

    public StaminaPotion(GamePanel gamePanel, int centerX, int centerY) {
        super(gamePanel, centerX, centerY, "Stamina Potion", new Color(90, 220, 110));
    }

    @Override
    protected int getTileIndex() {
        return TileSet.POTION_GREEN;
    }

    @Override
    public boolean canUse(Player player) {
        return player.getStamina() < player.getMaxStamina();
    }

    @Override
    public void use(Player player) {
        player.restoreStamina(restoreAmount);
    }
}
