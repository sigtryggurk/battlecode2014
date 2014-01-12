package trialplayer;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

/**
 * There are several types of Soldiers:
 * 
 * ATTACKER: Move in swarms of 5 attackers and attack enemy. Moves toward enemy
 * (how to pick?) and attacks him
 * 
 * PASTR: Finds a good cow field and constructs a PASTR
 * 
 * HERDER: Herds cows into PASTRs
 * 
 * NOISETOWER: Finds the furthest location such that many enemy pastrs are
 * included in its range
 */

public class Soldier
{
    RobotController rc;
    static Random random;
    private int role; // 1: ATTACKER, 2:PASTR, 3:HERDER, 4:NOISETOWER
    private int robotID;
    static int directionalLooks[] = new int[]
    { 0, 1, -1, 2, -2, 3, -3, 4
    };
    static int directionalBugLooks[] = new int[]
    { 0, 1, -1
    };
    private MapLocation target;
    private static ArrayList<MapLocation> snailTrail = new ArrayList<MapLocation>();
    private Direction[] allDirections = Direction.values();
    private boolean boundaryFollowing = false;
    private Direction forwardDirection;
    private MapLocation boundaryHitLoc;

    public Soldier(RobotController rc) throws GameActionException
    {

        this.rc = rc;
        robotID = rc.getRobot().getID();
        rc.broadcast(0, getChannel()); // Let HQ know your channel
        random = new Random(robotID);
    }

    private int getRole() throws GameActionException
    {
        return VectorTools.intToRoleAndTarget(rc.readBroadcast(getChannel()))[0];
    }

    private MapLocation getPASTRTarget() throws GameActionException
    {
        if (target == null)
        {
            target = VectorTools.intToLocation(VectorTools.intToRoleAndTarget(rc.readBroadcast(getChannel()))[1]);
        }
        return target;
    }

    private int getChannel()
    {
        return robotID;
    }

    public void run()
    {

        while (true)
        {
            try
            {
                role = getRole(); // Check role in case HQ changed it
                rc.setIndicatorString(0, "ROLE: " + role);

                switch (role)
                {
                case SoldierRole.ATTACKER:
                    runAttacker();
                case SoldierRole.PASTR:
                    runPASTR();
                case SoldierRole.HERDER:
                    runHerder();
                case SoldierRole.NOISETOWER:
                    runNoisetower();
                }

            } catch (Exception e)
            {
                e.printStackTrace();
            }
            rc.yield();
        }

    }

    private void runAttacker() throws GameActionException
    {
        int data = rc.readBroadcast(getChannel());
        int target = VectorTools.intToRoleAndTarget(data)[1];
        MapLocation loc = VectorTools.intToLocation(target);

        tryDefend();
        bugMove(loc);

    }

    private void tryDefend() throws GameActionException
    {
        if (rc.isActive() && rc.getActionDelay()<1)
        {
            Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared, rc
                    .getTeam().opponent());
            if (nearbyEnemies.length > 0)
            {
                RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
                rc.attackSquare(robotInfo.location);
            }
        }

    }

    private void runPASTR() throws GameActionException
    {
        MapLocation buildLoc = getPASTRTarget();

        int distance = rc.getLocation().distanceSquaredTo(buildLoc);
        boolean sneak = distance < 25;
        // tryAttack();
        moveTo(buildLoc, sneak);

        if (rc.isActive())
        {
            rc.construct(RobotType.PASTR);
        }
    }

    private void moveTo(MapLocation loc, boolean sneak) throws GameActionException
    {
        if (rc.isActive())
        {
            if (!(rc.getLocation().equals(loc) || rc.getLocation().isAdjacentTo(loc)))
            {
                tryToMove(rc.getLocation().directionTo(loc), true, sneak, rc, directionalLooks, Direction.values());
            }
        }
    }

    public boolean canMove(Direction dir, boolean selfAvoiding)
    {
        if (selfAvoiding)
        {
            MapLocation resultingLocation = rc.getLocation().add(dir);
            for (int i = 0; i < snailTrail.size(); i++)
            {
                MapLocation m = snailTrail.get(i);
                if (!m.equals(rc.getLocation()))
                {
                    if (resultingLocation.isAdjacentTo(m) || resultingLocation.equals(m))
                    {
                        return false;
                    }
                }
            }
        }
        // if you get through the loop, then dir is not adjacent to snail trail
        return rc.canMove(dir);
    }

    public void tryToMove(Direction chosenDirection, boolean selfAvoiding, boolean sneak, RobotController rc,
            int[] directionalLooks, Direction[] allDirections) throws GameActionException
    {
        // boolean tryBug = true;
        while (snailTrail.size() < 2)
            snailTrail.add(new MapLocation(-1, -1));
        if (rc.isActive())
        {
            snailTrail.remove(0);
            for (int directionalOffset : directionalLooks)
            {
                int forwardInt = chosenDirection.ordinal();
                Direction trialDir = allDirections[(forwardInt + directionalOffset + 8) % 8];
                if (canMove(trialDir, selfAvoiding))
                {
                    if (!sneak)
                    {
                        rc.move(trialDir);
                    } else
                    {
                        rc.sneak(trialDir);
                    }
                    snailTrail.add(rc.getLocation());
                    // tryBug = false;

                    break;
                }
            }
            // // If stuck use bug
            // if (tryBug)
            // {
            // Direction[] cardinals =
            // { Direction.NORTH, Direction.EAST, Direction.SOUTH,
            // Direction.WEST
            // };
            // for (Direction direction : cardinals)
            // {
            // MapLocation trialLocation = rc.getLocation().add(direction);
            // if (rc.senseTerrainTile(trialLocation) == TerrainTile.VOID
            // || rc.senseObjectAtLocation(trialLocation) != null)
            // {
            // Direction trialDir = direction.rotateLeft().rotateLeft();
            // if (canMove(trialDir, false))
            // {
            // if (!sneak)
            // {
            // rc.move(trialDir);
            // } else
            // {
            // rc.sneak(trialDir);
            // }
            // snailTrail.add(rc.getLocation());
            // break;
            // }
            // }
            //
            // }
            // }
        }

    }

    private void bugMove(MapLocation goal) throws GameActionException
    {
        if (rc.isActive())
        {
            if (!boundaryFollowing)
            {
                boolean canMove = false;
                Direction dir = rc.getLocation().directionTo(goal);
                for (int directionalOffset : directionalBugLooks)
                {
                    int forwardInt = dir.ordinal();
                    Direction trialDir = allDirections[(forwardInt + directionalOffset + 8) % 8];
                    if (rc.canMove(trialDir))
                    {
                        canMove = true;
                        boundaryFollowing = false;
                        rc.move(trialDir);
                        break;
                    }
                }
                if (!canMove)
                {
                    boundaryFollowing = true;
                    boundaryHitLoc = rc.getLocation().add(dir);
                    forwardDirection = dir;
                    while (!rc.canMove(forwardDirection))
                    {
                        forwardDirection = forwardDirection.rotateLeft();
                    }
                    rc.move(forwardDirection);
                }
            } else
            {
                if (isOnMLine(rc.getLocation(), goal) && !rc.getLocation().equals(boundaryHitLoc))
                {
                    boundaryFollowing = false;
                } else
                {
                    Direction rightDirection = forwardDirection.rotateRight().rotateRight();
                    if (rc.canMove(rightDirection))
                    {
                        rc.move(rightDirection);
                        forwardDirection = rightDirection;
                    }
                    if (rc.isActive())
                    {
                        while (!rc.canMove(forwardDirection))
                        {
                            forwardDirection = forwardDirection.rotateLeft();
                        }
                        rc.move(forwardDirection);
                    }

                }
            }
        }

    }

    private boolean isOnMLine(MapLocation testLoc, MapLocation goal)
    {

        // if (Math.abs((boundaryHitLoc.x - testLoc.x) * (boundaryHitLoc.y -
        // testLoc.y) - (testLoc.x - goal.x)
        // * (testLoc.y - goal.y)) ==0 &&
        // || boundaryHitLoc.equals(testLoc) || goal.equals(testLoc))
        // {
        float slope_x = ((float) (goal.x - boundaryHitLoc.x)) / (goal.y - boundaryHitLoc.y);
        float slope_y = ((float) (goal.y - boundaryHitLoc.y)) / (goal.x - boundaryHitLoc.x);
        float intercept_x = goal.x - slope_x * goal.y;
        float intercept_y = goal.y - slope_y * goal.x;
        int x = (int) (slope_x * testLoc.y + intercept_x);
        int y = (int) (slope_y * testLoc.x + intercept_y);
        if (x == testLoc.x || y == testLoc.y)
        {
            return true;
        }
        return false;

    }

    private void runHerder() throws GameActionException
    {
        int data = rc.readBroadcast(getChannel());
        int target = VectorTools.intToRoleAndTarget(data)[1];
        MapLocation loc = VectorTools.intToLocation(target);

        while (!(rc.getLocation().isAdjacentTo(loc) || rc.getLocation().equals(loc)))
        {
            tryDefend();
            moveTo(loc, false);
        }
        // shepherd(loc);

    }

    private void shepherd(MapLocation loc) throws GameActionException
    {

        Direction dir = loc.directionTo(rc.getLocation());
        MapLocation awayLoc = loc.add(dir, 4);
        while (!(rc.getLocation().isAdjacentTo(awayLoc) || rc.getLocation().equals(awayLoc)))
        {
            tryDefend();
            moveTo(awayLoc, true);
        }

    }

    private void runNoisetower()
    {
        // TODO Auto-generated method stub

    }

}
