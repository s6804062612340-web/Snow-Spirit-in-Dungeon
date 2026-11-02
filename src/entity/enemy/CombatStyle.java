package entity.enemy;

/**
 * รูปแบบการต่อสู้ของศัตรู
 *   MELEE  = ตีใกล้: วิ่งเข้าหา Player แล้วชน
 *   RANGED = ตีไกล: เว้นระยะห่างจาก Player แล้วยิงกระสุนใส่
 */
public enum CombatStyle {
    MELEE,
    RANGED
}
