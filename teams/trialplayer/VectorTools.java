package trialplayer;

import battlecode.common.MapLocation;

public class VectorTools
{

    public static int roleAndTargetToInt(int role, int target)
    {
        // TODO Auto-generated method stub
        return role + 10 * target;
    }

    public static int[] intToRoleAndTarget(int rt)
    {
        int role = rt % 10;
        int target = rt / 10;
        int roleAndTarget[] =
        { role, target
        };
        return roleAndTarget;
    }

    public static int locationToInt(MapLocation loc)
    {
        return loc.x * 100 + loc.y;
    }

    public static MapLocation intToLocation(int loc)
    {
        int x = loc / 100;
        int y = loc % 100;
        return new MapLocation(x, y);
    }

}
