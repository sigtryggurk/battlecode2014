package trialplayer;

import battlecode.common.RobotController;

public class Noisetower
{
    RobotController rc;

    public Noisetower(RobotController rc)
    {
        this.rc = rc;
    }

    public void run()
    {
        while (true)
        {
            rc.yield();
        }

    }

}
