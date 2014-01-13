package team196;

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
    private RobotController rc;
    private Random random;
    private static int role; // 1: ATTACKER, 2:PASTR, 3:HERDER, 4:NOISETOWER
    private static int robotID;
    static final int directionalLooks[] = new int[]
    { 0, 1, -1, 2, -2, 3, -3, 4
    };
    static final int directionalBugLooks[] = new int[]
    { 0, 1, -1
    };
    private static MapLocation target;
    private static ArrayList<MapLocation> snailTrail = new ArrayList<MapLocation>();
    private static Direction[] allDirections = Direction.values();
    private static boolean boundaryFollowing = false;
    private static Direction forwardDirection;
    private static MapLocation boundaryHitLoc;

    public Soldier(RobotController rc) throws GameActionException
    {

        this.rc = rc;
        random = new Random(robotID);
        robotID = rc.readBroadcast(0);
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

    private MapLocation getTarget() throws GameActionException
    {
        int data = rc.readBroadcast(getChannel());
        int target = VectorTools.intToRoleAndTarget(data)[1];
        return VectorTools.intToLocation(target);
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
                role = getRole(); // Check role in case HQ reassigned you
                rc.setIndicatorString(0, "ROLE: " + role);

                switch (role)
                {
                case SoldierRole.PASTR:
                    runPASTR();
                    break;
                case SoldierRole.HERDER:
                    runHerder();
                    break;
                case SoldierRole.NOISETOWER:
                    runNoisetower();
                    break;
                case SoldierRole.ATTACKER:
                    runAttacker();
                default:
                    break;
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
        MapLocation rallyPoint = getTarget();

        int attackInt = rc.readBroadcast(100);

        tryAttack();

        if (attackInt == -1 && rc.getLocation().distanceSquaredTo(rallyPoint) > 16)
        {
            bugMove(rallyPoint);

        } else
        {
            bugMove(VectorTools.intToLocation(attackInt));
        }

    }

    private void tryAttack() throws GameActionException
    {
        if (rc.isActive() && rc.getActionDelay() < 1)
        {
            Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared, rc
                    .getTeam().opponent());
            if (nearbyEnemies.length > 0)
            {
                RobotInfo robotInfo = closestEnemyNotHQ(nearbyEnemies);
                if (robotInfo != null)
                {
                    rc.broadcast(200, VectorTools.locationToInt(robotInfo.location));
                    rc.attackSquare(robotInfo.location);
                }
            }
        }

    }

    private RobotInfo closestEnemyNotHQ(Robot[] nearbyEnemies) throws GameActionException
    {
        int closestDist = 200;
        RobotInfo closestEnemy = null;
        for (Robot enemy : nearbyEnemies)
        {
            RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
            if (!(enemyInfo.type == RobotType.HQ))
            {
                int dist = rc.getLocation().distanceSquaredTo(enemyInfo.location);
                if (dist < closestDist)
                {
                    closestDist = dist;
                    closestEnemy = enemyInfo;
                }
            }
        }
        return closestEnemy;
    }

    private void runPASTR() throws GameActionException
    {
        MapLocation buildLoc = getPASTRTarget(); // TODO reuse getTarget method

        int distance = rc.getLocation().distanceSquaredTo(buildLoc);
        boolean sneak = distance < 25;

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
                tryToMove(rc.getLocation().directionTo(loc), true, sneak);
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

    public void tryToMove(Direction chosenDirection, boolean selfAvoiding, boolean sneak) throws GameActionException
    {
        while (snailTrail.size() < 4)
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
                    break;
                }
            }

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
                    
                    if(dir.equals(Direction.OMNI)){
                        dir = allDirections[random.nextInt(8)]; //Because WTF
                    }
                    forwardDirection = dir;

                    while (!rc.canMove(forwardDirection))
                    {
                        forwardDirection = forwardDirection.rotateLeft();
                    }
                    rc.move(forwardDirection);
                }
            } else
            {
                if (isOnMLine(rc.getLocation(), goal) && closerToGoal(goal))
                {
                    boundaryFollowing = false;
                } else
                {
                    Direction rightDirection = allDirections[(forwardDirection.ordinal() + 2) % 8];
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

    private boolean closerToGoal(MapLocation goal)
    {
        int hitLocToGoal = boundaryHitLoc.distanceSquaredTo(goal);
        int currentLocToGoal = rc.getLocation().distanceSquaredTo(goal);
        return currentLocToGoal < hitLocToGoal;
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
        MapLocation loc;
        MapLocation[] pastrLoc = rc.sensePastrLocations(rc.getTeam());
        if(pastrLoc==null){
            loc = getTarget();
        }
         loc = pastrLoc[random.nextInt(pastrLoc.length)];

        while (rc.getLocation().distanceSquaredTo(loc) > 16)
        {
            tryAttack();
            moveTo(loc, false);
            rc.yield();
        }

        shepherd(loc);

    }

    private void shepherd(MapLocation loc) throws GameActionException
    {

        Direction dir = allDirections[(int) (Math.random() * 8)];
        MapLocation awayLoc = loc.add(dir, 6);
        while (rc.getLocation().distanceSquaredTo(awayLoc) > 9)
        {
            tryAttack();
            moveTo(awayLoc, true);
            rc.yield();
        }

    }

    private void runNoisetower() throws GameActionException
    {
        MapLocation buildLoc = getTarget();

        moveTo(buildLoc, false);

        if (rc.isActive())
        {
            rc.construct(RobotType.NOISETOWER);
        }

    }

}
