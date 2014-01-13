package team196;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

//BROADCAST:
//0 for role assignment
//

public class HQ
{
    private Direction spawnDirection;
    private MapLocation rallyPoint;
    private RobotController rc;
    private int robotIDCounter = 1;
    private Map<Integer, List<Integer>> rolesToSoldiers = new HashMap<Integer, List<Integer>>();
    // 1:ATTACKER,
    // 2:PASTR,
    // 3:HERDER,
    // 4:NOISETOWER
    private MapLocation[] PASTRLocs;

    public HQ(RobotController rc)
    {
        this.rc = rc;
    }

    public void run() throws GameActionException
    {
        spawnDirection = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
        rallyPoint = VectorTools.add(
                VectorTools.divide(VectorTools.subtract(rc.senseEnemyHQLocation(), rc.senseHQLocation()), 3),
                rc.senseHQLocation());
        Pathing.initializePathing(rc);
        rc.broadcast(100, -1);
        // tryDefend();
        // trySpawn();
        // Pathing.prepareInternalMap(rc);

        while (true)
        {
            try
            {
                tryDefend();
                tryDispatch();
                trySpawn();

            } catch (Exception e)
            {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    private void tryDispatch() throws GameActionException
    {
        if (rc.isActive())
        {
            int pastrDanger = rc.readBroadcast(200);
            if (pastrDanger != 0)
            {
                rc.broadcast(100, pastrDanger);
            } else if (rolesToSoldiers.containsKey(SoldierRole.ATTACKER))
            {
                List<Integer> attackers = rolesToSoldiers.get(SoldierRole.ATTACKER);
                if (attackers.size() > 5)
                {
                    tryBroadCastTarget();
                }
            }
        }
    }

    private void tryBroadCastTarget() throws GameActionException
    {
        MapLocation[] PASTRLocs = rc.sensePastrLocations(rc.getTeam().opponent());

        if (PASTRLocs.length > 0)
        {
            rc.broadcast(100, VectorTools.locationToInt(PASTRLocs[0]));
        } else
        {
            rc.broadcast(100, -1);
        }
        // for(int i=PASTRLocs.length-1;i>-1;i--){
        // int channel = 100+i;
        // int data = VectorTools.locationToInt(PASTRLocs[i]);
        // rc.broadcast(channel, data);
        // }
        // TODO Auto-generated method stub

    }

    private void tryDefend() throws GameActionException
    {
        if (rc.isActive() && rc.getActionDelay() < 1)
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

    private void trySpawn() throws GameActionException
    {
        if (rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS)
        {
            rc.broadcast(0, robotIDCounter);
            Direction trialDirection = spawnDirection;
            for (int i = 0; i < 8; i++)
            {
                if (rc.canMove(trialDirection))
                {
                    rc.spawn(trialDirection);
                    break;
                }
                trialDirection = trialDirection.rotateRight();
            }
            trainSoldier();
        }

    }

    private void trainSoldier() throws GameActionException
    {
        int robotID = robotIDCounter++;
        int channel = robotID;
        int role = nextRole();
        int target = nextTarget(role);

        int data = VectorTools.roleAndTargetToInt(role, target);
        rc.broadcast(channel, data);

        if (rolesToSoldiers.containsKey(role))
        {
            rolesToSoldiers.get(role).add(robotID);
        } else
        {
            List<Integer> soldiers = new ArrayList<Integer>();
            soldiers.add(robotID);
            rolesToSoldiers.put(role, soldiers);
        }
    }

    private int nextRole()
    {
        if (!rolesToSoldiers.containsKey(SoldierRole.PASTR) || rolesToSoldiers.get(SoldierRole.PASTR).size() < 2)
        { // First two must be PASTR
            return SoldierRole.PASTR;

        } else if (!rolesToSoldiers.containsKey(SoldierRole.HERDER) && rc.sensePastrLocations(rc.getTeam()).length != 0)
        {
            return SoldierRole.HERDER;
        } else if (!rolesToSoldiers.containsKey(SoldierRole.NOISETOWER)
                && rc.sensePastrLocations(rc.getTeam()).length > 3)
        {
            return SoldierRole.NOISETOWER;
        } else
        {
            return (rc.sensePastrLocations(rc.getTeam()).length  < rc.sensePastrLocations(rc.getTeam().opponent()).length) ? SoldierRole.PASTR
                    : SoldierRole.ATTACKER; 
            // Alternate
            // between
            // the
            // two
        }
        // return SoldierRole.ATTACKER;
        // int numberOfPASTR = rc.sensePastrLocations(rc.getTeam()).length;
        //
        // if (numberOfPASTR > 0
        // && (!rolesToSoldiers.containsKey(SoldierRole.HERDER) ||
        // rolesToSoldiers.get(SoldierRole.HERDER).size() < 1))
        // {
        // return SoldierRole.HERDER;
        // }
        // if (numberOfPASTR > 6)
        // {
        // return SoldierRole.ATTACKER;
        // }
        // return SoldierRole.PASTR;
    }

    private int nextTarget(int role)
    {
        switch (role)
        {
        case SoldierRole.ATTACKER:
            return nextAttackerTarget();
        case SoldierRole.PASTR:
            return nextPASTRTarget();
        case SoldierRole.HERDER:
            return nextHerderTarget();
        case SoldierRole.NOISETOWER:
            return nextNoisetowerTarget();
        default:
            return -1;// shouldn't get here
        }
    }

    private int nextAttackerTarget()
    {
        return VectorTools.locationToInt(rallyPoint);
    }

    private int nextPASTRTarget()
    {
        MapLocation loc = Pathing.findNewClosestMaxCowGrowth(rc);
        return VectorTools.locationToInt(loc);
    }

    private int nextHerderTarget()
    {

        // PASTRLocs = rc.sensePastrLocations(rc.getTeam());
        // int i = (int) (Math.random() * PASTRLocs.length);
        //
        // int result = VectorTools.locationToInt(PASTRLocs[i]);
        return VectorTools.locationToInt(rallyPoint);
    }

    private int nextNoisetowerTarget()
    {
        return VectorTools.locationToInt(rallyPoint);
    }
}
