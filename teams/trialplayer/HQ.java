package trialplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

//BROADCAST:
//0 for role assignment
//

public class HQ
{
    private Direction spawnDirection;
    private RobotController rc;
    private Map<Integer, List<Integer>> rolesToSoldiers = new HashMap<Integer, List<Integer>>();// 0:
                                                                                                // ATTACKER,
                                                                                                // 1:PASTR,
                                                                                                // 2:HERDER,
                                                                                                // 3:NOISETOWER
    private MapLocation[] PASTRLocs;

    public HQ(RobotController rc)
    {
        this.rc = rc;
    }

    public void run() throws GameActionException
    {
        spawnDirection = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
        Pathing.initializePathing(rc);
        tryToSpawn();
        // Pathing.prepareInternalMap(rc);

        while (true)
        {
            try
            {
                tryToSpawn();

            } catch (Exception e)
            {
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    private void tryToSpawn() throws GameActionException
    {
        if (rc.isActive() && rc.senseRobotCount() < GameConstants.MAX_ROBOTS)
        {
            rc.broadcast(0, -1);
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
            rc.yield();
            assignRole();
        }

    }

    private void assignRole() throws GameActionException
    {
        int robotID = rc.readBroadcast(0);
        System.out.println(robotID);
        if (robotID != -1)
        {
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

    }

    private int nextRole()
    {
        // return SoldierRole.ATTACKER;
        int numberOfPASTR = rc.sensePastrLocations(rc.getTeam()).length;

        if (numberOfPASTR > 0
                && (!rolesToSoldiers.containsKey(SoldierRole.HERDER) || rolesToSoldiers.get(SoldierRole.HERDER).size() < 1))
        {
            return SoldierRole.HERDER;
        }
        if (numberOfPASTR > 6)
        {
            return SoldierRole.ATTACKER;
        }
        return SoldierRole.PASTR;
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
        MapLocation enemyHQ = Pathing.getRandomLocation(); // TODO set
                                                           // rallypoint, if 5
                                                           // attack
        return VectorTools.locationToInt(enemyHQ);
    }

    private int nextPASTRTarget()
    {
        MapLocation loc = Pathing.findNewClosestMaxCowGrowth(rc);
        return VectorTools.locationToInt(loc);
    }

    private int nextHerderTarget()
    {

        PASTRLocs = rc.sensePastrLocations(rc.getTeam());
        int i = (int) (Math.random() * PASTRLocs.length);

        int result = VectorTools.locationToInt(PASTRLocs[i]);
        return result;
    }

    private int nextNoisetowerTarget()
    {
        // TODO Auto-generated method stub
        return 0;
    }
}
