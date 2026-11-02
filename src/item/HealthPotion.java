package item;

import java.awt.*;

import entity.Player;
import main.GamePanel;
import world.TileSet;

/**
 * ยาแดง: ฟื้น HP 2 หน่วย (ดื่มไม่ได้ถ้า HP เต็ม)
 */
public class HealthPotion extends Potion {

    private final int healAmount = 2;

    public HealthPotion(GamePanel gamePanel, int centerX, int centerY) {
        super(gamePanel, centerX, centerY, "Health Potion", new Color(220, 40, 60));
    }

    @Override
    protected int getTileIndex() {
        return TileSet.POTION_RED;
    }

    @Override
    public boolean canUse(Player player) {
        return player.getHp() < player.getMaxHp();
    }

    @Override
    public void use(Player player) {
        player.heal(healAmount);
    }
}
