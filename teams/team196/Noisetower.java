package team196;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Noisetower
{
    RobotController rc;

    public Noisetower(RobotController rc)
    {
        this.rc = rc;
    }

    public void run() throws GameActionException
    {
        while (true)
        {
            if (rc.isActive() && rc.getActionDelay() < 1)
            {
                MapLocation[] PASTRLocs = rc.sensePastrLocations(rc.getTeam().opponent());
                if (PASTRLocs.length > 0)
                {
                    MapLocation target = getRandomPASTRInRange(PASTRLocs);
                    if (target != null)
                    {
                        rc.attackSquare(target);
                    }
                }
            }
            rc.yield();
        }

    }

    private MapLocation getRandomPASTRInRange(MapLocation[] pastrLocs)
    {
        List<MapLocation> locsInRange = new ArrayList<MapLocation>();
        for (MapLocation loc : pastrLocs)
        {
            int distance = rc.getLocation().distanceSquaredTo(loc);
            if (distance <= RobotType.NOISETOWER.attackRadiusMaxSquared)
            {
                locsInRange.add(loc);
            }
        }
        int size = locsInRange.size();
        if (size > 0)
        {
            return locsInRange.get((int) (Math.random() * size));
        }
        return null;
    }

}
