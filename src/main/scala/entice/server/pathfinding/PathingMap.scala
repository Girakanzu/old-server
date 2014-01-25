/**
 * For copyright information see the LICENSE document.
 */

package entice.server.pathfinding

import entice.protocol.Coord2D
import play.api.libs.json._
import info.akshaal.json.jsonmacro._
import scala.io._
import scala.util._

import Geometry._


/**
 * Simplified non-associative version, for serialization purposes.
 */
case class SimplePathingMap(
    trapezoids: Array[SimpleTrapezoid],
    connections: Array[SimpleHorizontalConnection])


/**
 * Consists of the walkable trapezoids and their connections.
 */
class PathingMap(
    val trapezoids: List[Trapezoid],
    val connections: Set[HorizontalConnection]) {


    /**
     * Checks which trapezoid the position is in
     */
    def trapezoidFor(pos: Coord2D): Option[Trapezoid] = {
        for (trap <- trapezoids) {
            if (trap.contains(pos)) return Some(trap)
        }
        None
    }


    /**
     * Checks if there is a linear path from a position in a direction
     * to a certain trapezoid
     */
    def hasDirectPath(pos: Coord2D, dir: Coord2D, goal: Trapezoid, current: Option[Trapezoid] = None): Boolean = {
        val currentTrap = if (current.isDefined) current else trapezoidFor(pos)
        if (!currentTrap.isDefined) return false
        if (!currentTrap.get.contains(pos)) return false
        if (goal == currentTrap.get) return true

        currentTrap.get.crossedConnection(pos, dir) match {
            case Some((con, loc)) =>
                // go to the border of the other trapezoid, then start the search anew
                hasDirectPath(loc, dir, goal, Some(con.adjacentOf(currentTrap.get)))
            case None => 
                false
        }
    }


    /**
     * If i would walk from my position onward in a direction, check where i would get to a border
     */
    def farthestPosition(pos: Coord2D, dir: Coord2D, current: Option[Trapezoid] = None): Option[Coord2D] = {
        val currentTrap = if (current.isDefined) current else trapezoidFor(pos)
        if (!currentTrap.isDefined) return None
        if (!currentTrap.get.contains(pos)) return None
        // get the trapezoid that will be the last one before we collide with a border
        def lastTrap(curPos: Coord2D, curDir: Coord2D, curTrap: Trapezoid): (Trapezoid, Coord2D) = {
            curTrap.crossedConnection(curPos, curDir) match {
                case Some((con, loc)) => lastTrap(loc, curDir, con.adjacentOf(curTrap))
                case None             => (curTrap, curPos)
            }
        }
        val (newTrap, newPos) = lastTrap(pos, dir, currentTrap.get)
        newTrap.crossedBorder(newPos, dir)
    }


    /**
     * Is our next position valid?
     */
     def nextValidPosition(pos: Coord2D, nextPos: Coord2D, current: Option[Trapezoid] = None): Option[Coord2D] = {
        val currentTrap = if (current.isDefined) current else trapezoidFor(pos)
        if (!currentTrap.isDefined) return None
        if (!currentTrap.get.contains(pos)) return None

        val farPos = farthestPosition(pos, (nextPos - pos), currentTrap)
        if (!farPos.isDefined) return None
        // our next pos must be between the current and the maximum possible pos to be valid
        // else we return the max possible position
        if (nextPos.x.within(pos.x, farPos.get.x) && nextPos.y.within(pos.y, farPos.get.y)) {
            return Some(nextPos)
        } else  {
            return farPos
        }

     }
}


/**
 * Companion with convenience methods.
 */
object PathingMap {
    // deserialization
    import Edge._
    import Trapezoid._
    implicit def simplePathingMapFactory = factory[SimplePathingMap]('fromJson)


    /**
     * Try to load from a JSON string. (can fail with None)
     */
    def fromString(jsonMap: String): Option[PathingMap] = {
        Try(Json.fromJson[SimplePathingMap](Json.parse(jsonMap)).get) match {
            case pmap: Success[_] => Some(PathingMap.sophisticate(pmap.get))
            case pmap: Failure[_] => None
        }
    }


    /**
     * Try to load from a JSON file. (can fail with None)
     */
    def fromFile(file: String): Option[PathingMap] = {
        Try(Source.fromFile(file).mkString.trim) match {
            case pmap: Success[_] => fromString(pmap.get)
            case pmap: Failure[_] => None
        }
    }


    /**
     * Creates associations from the IDs used in the simple versions of connections
     */
    private def sophisticate(simple: SimplePathingMap) = {
        val traps: Map[Int, Trapezoid] = 
            (for (t <- simple.trapezoids) yield 
                (t.id -> Trapezoid.sophisticate(t)))
            .toMap
        val conns: Set[HorizontalConnection] =
            (for (c <- simple.connections) yield 
                new HorizontalConnection(c.west, c.east, c.y, traps(c.north), traps(c.south)))
            .toSet

        // update the connections on the trapezoids
        conns foreach { c => 
            c.northern.connections = c :: c.northern.connections
            c.southern.connections = c :: c.southern.connections 
        }

        new PathingMap(traps.values.toList, conns)
    }
}