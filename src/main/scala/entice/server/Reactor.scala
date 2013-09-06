/**
 * For copyright information see the LICENSE document.
 */

package entice.server

import akka.actor.{ Actor, ActorRef, ActorLogging, ActorSystem, Props }

import java.net.InetSocketAddress

import entice.protocol._
import entice.protocol.utils._


object ReactorActor {
    case class Subscribe(me: ActorRef, forMsg: Class[_ <: Message])
    case class Publish(sender: ActorRef, msg: Message)
}


trait Subscriber {
    this: Actor =>

    import ReactorActor._

    def reactor: ActorRef
    def subscriptions: List[Class[_ <: Message]]

    def register {
        subscriptions map { reactor ! Subscribe(self, _) }
    }
}


/**
 * Encapsulates a pub/sub message bus.
 */
class ReactorActor(messageBus: MessageBus) extends Actor with ActorLogging {

    import ReactorActor._
    import MessageBus._


    def receive = {
        case Publish(sender, message) =>
            log.debug(s"Pub: ${sender.toString} -> ${message.`type`}")
            messageBus.publish(MessageEvent(sender, message))
        case Subscribe(actor, message) =>
            log.debug(s"Sub: ${actor.toString} for ${message.getSimpleName}")
            messageBus.subscribe(actor, message)
    }
}