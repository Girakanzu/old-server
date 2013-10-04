/**
 * For copyright information see the LICENSE document.
 */

package entice.server.systems

import entice.server._
import entice.server.world._
import entice.server.utils._
import entice.protocol._
import akka.actor._
import shapeless._
import scala.concurrent.duration._
import scala.language.postfixOps


class AnimationSystem extends System[Animation :: HNil] with Actor with Subscriber {

    val subscriptions = classOf[Animate] :: Nil
    override def preStart { register }


    def receive = {
        case MessageEvent(_, Animate(entity, ani)) =>
            entity.set(Animation(ani))
            publish(Schedule(Animate(entity, "none"), 1 second))
    }
}