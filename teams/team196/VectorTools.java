package team196;

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

    public static int coordToInt(int x, int y)
    {
        return x * 100 + y;
    }

    public static int[] intToCoord(int num)
    {
        int pair[] = new int[2];
        pair[0] = num / 100;
        pair[1] = num % 100;
        return pair;
    }

    public static MapLocation subtract(MapLocation loc1, MapLocation loc2)
    {
        return new MapLocation(loc1.x - loc2.x, loc1.y - loc2.y);
    }

    public static MapLocation divide(MapLocation loc, int n)
    {
        return new MapLocation(loc.x / n, loc.y / n);
    }

    public static MapLocation add(MapLocation loc1, MapLocation loc2)
    {
        return new MapLocation(loc2.x + loc1.x, loc2.y + loc1.y);
    }

}
