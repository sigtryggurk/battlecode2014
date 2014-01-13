package team196;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer
{
    public static void run(RobotController rc) throws GameActionException
    {
        if (rc.getType() == RobotType.HQ)
        {
            HQ hq = new HQ(rc);
            hq.run();
        } else if (rc.getType() == RobotType.SOLDIER)
        {
            Soldier soldier = new Soldier(rc);
            soldier.run();
        } else if (rc.getType() == RobotType.PASTR)
        {
            PASTR pastr = new PASTR(rc);
            pastr.run();
        } else
        { // NOISETOWER
            Noisetower noisetower = new Noisetower(rc);
            noisetower.run();
        }
    }
}
