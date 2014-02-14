/**
 * For copyright information see the LICENSE document.
 */

package entice.server.world.systems

import entice.server._
import entice.server.utils._
import entice.server.world._
import entice.server.pathfinding._, Geometry._
import entice.protocol._, MoveState._
import akka.actor._
import shapeless._


class MovementSystem extends System[Position :: Movement :: HNil] with Actor with Subscriber with Worlds {

    val subscriptions = classOf[Tick] :: Nil
    override def preStart { register }

    val stopWatch = StopWatch()

    def receive = {
        case Tick => worlds.getAll foreach update
    }


    override def update(world: World) {
        val timeDiff = stopWatch.current

        entities(world)
            .filter  { e => 
                val curPos  = e[Position].pos
                val curGoal = e[Movement].goal
                (e[Movement].moveState != NotMoving.toString &&
                 curGoal - curPos      != Coord2D(0, 0)) 
            }
            .foreach { e =>
                val curPos  = e[Position].pos
                val curGoal = e[Movement].goal
                val curDir  = curGoal - curPos

                // TODO: add movementspeed here, add trapezoid
                // the 0.288 is actually distance per milliseconds, so
                // it would be 288 terrain tiles per second
                val nextPos = curPos + ((curDir.unit * 0.288F) * timeDiff)

                // check out the next position and set it
                world.pmap.nextValidPosition(curPos, nextPos) match {
                    case Some(pos) if (pos != curPos) => 
                        // set the new position
                        e.set[Position](e[Position].copy(pos = pos))
                        // get a new goal
                        world.pmap.farthestPosition(pos, curDir) match {
                            case Some(goal) if (goal != pos) =>
                                e.set[Movement](e[Movement].copy(goal = goal))
                            case _ =>
                                e.set[Movement](e[Movement].copy(goal = pos, state = NotMoving.toString))
                        }
                    case _ => 
                        e.set[Movement](e[Movement].copy(goal = curPos, state = NotMoving.toString))
                }
                
            }
    }
}
