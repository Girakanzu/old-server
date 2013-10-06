/**
 * For copyright information see the LICENSE document.
 * Adapted from: https://gist.github.com/DeLongey/3757237
 */

package entice.server.utils

import entice.protocol._
import akka.event.ActorEventBus
import akka.event.LookupClassification
import akka.actor.{ ActorRef, Extension }
 

/**
 * Encapsulates a single message. Messages must be classifyable by their
 * class name, given as a 'type' property.
 */
case class MessageEvent(sender: ActorRef, message: Typeable)


/**
 * Message bus to route messages to their appropriate handler actors.
 * This is an implementation of the Reactor design pattern.
 *
 * Details:
 * When subscribing to a message, you actually subscribe to the classname of it.
 * This is because messages provide their classname as a 'type' field anyway, so we
 * can classify them that way easily. The class name does not need be read out by 
 * the usage of reflection at runtime when a message gets published.
 *
 * Usage:
 * This might be used with appropriate message handler actors. The message event carries
 * an additional field just for the purpose of giving the handler some kind of information
 * about the sender.
 * The sender actor is additionally wrapped to make it possible to identify it.
 * (Note that it needs to pass its identifier itself, so the sender is responsible for
 * any conflicts that might occur.)
 */
class MessageBus extends ActorEventBus with LookupClassification with Extension {

    type Event = MessageEvent
    type Classifier = String


    protected val mapSize = 10


    protected def classify(event: Event): Classifier = event.message.`type`


    def subscribe(subscriber: Subscriber, msgClazz: Class[_]) {
        super.subscribe(subscriber, msgClazz.getSimpleName)
    }


    protected def publish(event: Event, subscriber: Subscriber) {
        subscriber ! event
    }
}