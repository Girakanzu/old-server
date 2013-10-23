/**
 * For copyright information see the LICENSE document.
 */

package entice.server.pathfinding

import entice.protocol.Coord2D
import play.api.libs.json._
import info.akshaal.json.jsonmacro._

import Geometry._


/**
 * Extend me if necessary, but this should only hold references to
 * Geometry objects
 */
sealed trait Edge {
    def p1: Coord2D
    def p2: Coord2D
    val segment = new Segment2D(p1, p2)

    def location(pos: Coord2D) = segment.location(pos)
    def distance(pos: Coord2D) = segment.distance(pos)
    def crossed(pos: Coord2D, dir: Coord2D) = segment.walkOver(pos, dir)
}


/**
 * Companion with deserialisation stuff
 */
object Edge {
    import entice.protocol.Utils._
    implicit def horizontalBorderFactory            = factory[HorizontalBorder]           ('fromJson)
    implicit def simpleHorizontalConnectionFactory  = factory[SimpleHorizontalConnection] ('fromJson)
    implicit def verticalBorderFactory              = factory[VerticalBorder]             ('fromJson)
}


/**
 * This is a horizontally aligned line
 */
sealed trait HorizontalEdge extends Edge {
    def west: Float
    def east: Float
    def y:    Float

    val p1 = Coord2D(west, y)
    val p2 = Coord2D(east, y)
}
case class HorizontalBorder    (west: Float, east: Float, y: Float) extends HorizontalEdge
case class HorizontalConnection(west: Float, east: Float, y: Float, northern: Trapezoid, southern: Trapezoid) extends HorizontalEdge {
    def adjacentOf(that: Trapezoid) = if (northern == that) southern else northern
}

case class SimpleHorizontalConnection(west: Float, east: Float, y: Float, north: Int, south: Int)


/**
 * This is just NOT horizontally aligned, but not necessarily
 * vertical aligned! Hence this line is given by two points.
 */
sealed trait VerticalEdge extends Edge{
    def north: Coord2D
    def south: Coord2D

    val p1 = south
    val p2 = north
}
case class VerticalBorder(north: Coord2D, south: Coord2D) extends VerticalEdge
