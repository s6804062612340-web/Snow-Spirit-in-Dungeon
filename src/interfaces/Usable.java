package interfaces;

import entity.Player;

/**
 * สิ่งที่ Player "ใช้งานได้" เช่น Potion
 */
public interface Usable {

    void use(Player player);
}
