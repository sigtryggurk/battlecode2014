package trialplayer;

import battlecode.common.RobotController;

public class PASTR
{
    RobotController rc;

    public PASTR(RobotController rc)
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
