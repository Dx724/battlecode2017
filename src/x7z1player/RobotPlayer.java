package x7z1player;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    
    static float degreeOffset = 32.0f;
    static float cosDeg = (float) Math.cos(degreeOffset);
    static float sinDeg = (float) Math.sin(degreeOffset);
    
    static Direction targetDir = new Direction(degreeOffset);

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case SCOUT:
            	runScout();
            	break;
            case TANK:
            	runTank();
            	break;
        }
	}

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");
        

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection(); //TODO: Greedy robot hires/builds -- Span multiple turns if necessary

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .01) {
                    rc.hireGardener(dir);
                }

                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();  //TODO: Should this happen? Allows others to read position
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);
                
                if (rc.getTeamBullets() > 350) {
                	rc.donate(70);
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");
        
        int createNum = 0; //Scout, Soldier, Tank, Scout, Lumberjack, Soldier, Soldier, Tank, Soldier, Soldier, Tank, etc.
        int buildFailTurns = 0;
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a soldier or lumberjack in this direction
                /*if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                } else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }*/
                Direction rDir = randomDirection();
                if (createNum == 0 || createNum == 3) {
                	if (rc.getTeamBullets() >= 80) { //HC -- Scout cost
		                if (rc.canBuildRobot(RobotType.SCOUT, rDir)) {
		                	rc.buildRobot(RobotType.SCOUT, rDir);
		                	createNum += 1;
		                }
		                else {
		                	buildFailTurns += 1;
		                	if (buildFailTurns > 10) {
		                		createNum += 1;
		                		buildFailTurns = 0;
		                	}
		                }
                	}
                }
                else if (createNum == 1 || (createNum > 4 && (createNum - 1) % 3 != 0)) {
                	if (rc.getTeamBullets() >= 100) { //HC -- Soldier cost
	                	if (rc.canBuildRobot(RobotType.SOLDIER, rDir)) {
	                		rc.buildRobot(RobotType.SOLDIER, rDir);
	                		createNum += 1;
	                	}
	                	else {
		                	buildFailTurns += 1;
		                	if (buildFailTurns > 10) {
		                		createNum += 1;
		                		buildFailTurns = 0;
		                	}
		                }
                	}
                }
                else if (createNum == 4) {
                	if (rc.getTeamBullets() >= 100) {
	                	if (rc.canBuildRobot(RobotType.LUMBERJACK, rDir)) {
	                		rc.buildRobot(RobotType.LUMBERJACK, rDir);
	                		createNum += 1;
	                	}
	                	else {
		                	buildFailTurns += 1;
		                	if (buildFailTurns > 10) {
		                		createNum += 1;
		                		buildFailTurns = 0;
		                	}
		                }
                	}
                }
                else {
                	if (rc.getTeamBullets() > 300) {
	                	if (rc.canBuildRobot(RobotType.TANK, rDir)) {
	                		rc.buildRobot(RobotType.TANK, rDir);
	                		createNum += 1;
	                	}
	                	else {
		                	buildFailTurns += 1;
		                	if (buildFailTurns > 10) {
		                		createNum += 1;
		                		buildFailTurns = 0;
		                	}
		                }
                	}
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }
	
	static void runScout() throws GameActionException {
		MapLocation targetTreeLocation = null;
		int failTurns = 0;
		
		Team enemy = rc.getTeam().opponent();
		
		while (true) {
			try {
				if (targetTreeLocation == null) {
					targetTreeLocation = scout_getClosestShakeableTreeLocation();
					if (targetTreeLocation == null) { //No Tree Found
                    	if (!dodgeBullets(rc)) {
							if(!tryMove(targetDir)) {
								targetDir = targetDir.rotateRightDegrees(90);
								if (!tryMove(targetDir)) {
									targetDir = targetDir.rotateRightDegrees(90);
									if (!tryMove(targetDir)) {
										targetDir = targetDir.rotateRightDegrees(90);
										if (!tryMove(targetDir)) {
											targetDir = targetDir.rotateRightDegrees(45);
										}
									}
								}
							}
                    	}
					}
				}
				else {
					if (rc.canShake(targetTreeLocation)) {
						rc.shake(targetTreeLocation);
						targetTreeLocation = null;
						failTurns = 0;
					}
					else {
						tryMove(new Direction(rc.getLocation(), targetTreeLocation));
						failTurns += 1;
						if (failTurns >= 8) { //HC -- Scout sight radius divided by scout stride radius times two
							targetTreeLocation = null;
							failTurns = 0;
						}
					}
				}
				if (targetTreeLocation != null) { //DEBUG
					rc.setIndicatorDot(targetTreeLocation, 15, 15, 200);
				}
				
				 // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }
				
				Clock.yield();
			}
			catch (Exception e) {
				System.out.println("Scout Exception");
				e.printStackTrace();
			}
		}
	}
	
	static MapLocation scout_getClosestShakeableTreeLocation() throws GameActionException { //Returns *null* when no trees are found
		//1.2.2 -- "Robots/Trees/Bullets from senseNearbyRobots/Trees/Bullets() are now returned in order of increasing distance from the specified center point."
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		float shortestDistance = Float.MAX_VALUE;
		MapLocation closestTreeLocation = null;
		for (TreeInfo tree : trees) {
			if (tree.getContainedBullets() == 0 || tree.getContainedRobot() == null) {
				continue;
			}
			float distance = rc.getLocation().distanceTo(tree.location);
			if (distance < shortestDistance) {
				shortestDistance = distance;
				closestTreeLocation = tree.location;
			}
		}
		return closestTreeLocation;
	}

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
            	if (!dodgeBullets(rc)) tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }
    
    static void runTank() throws GameActionException {
    	Team enemy = rc.getTeam().opponent();

        while (true) { //From Soldier Code
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
            	if (!dodgeBullets(rc)) tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else {
                        // Move Randomly
                    	if (!dodgeBullets(rc)) tryMove(randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }
    
    static boolean dodgeBullets(RobotController rc) throws GameActionException {
    	BulletInfo[] bullets = rc.senseNearbyBullets();
    	for (BulletInfo bullet : bullets) {
	    	if (willCollideWithMe(bullet)) {
	    		float radiansBetween = bullet.dir.radiansBetween(bullet.getLocation().directionTo(rc.getLocation()));
	    		if (radiansBetween >= 0) {
	    			return tryMove(bullet.dir.rotateRightDegrees(90));
	    		}
	    		else {
	    			return tryMove(bullet.dir.rotateLeftDegrees(90));
	    		}
	    	}
	    	else {
	    		return false;
	    	}
    	}
    	return false;
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}
