package team196;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class PASTR
{
    RobotController rc;

    public PASTR(RobotController rc)
    {
        this.rc = rc;
    }

    public void run() throws GameActionException
    {
        while (true)
        {
            Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, rc.getType().sensorRadiusSquared, rc
                    .getTeam().opponent());
            if(nearbyEnemies.length>0){
                rc.broadcast(200, VectorTools.locationToInt(rc.getLocation()));
            }
            rc.yield();
        }

    }

}
