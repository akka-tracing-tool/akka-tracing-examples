package akka

import java.io.File

import akka.actor._
import akka_debugging.DistributedStackTrace
import akka_debugging.collector.Collector.{RelationMessage, CollectorMessage}
import akka_debugging.collector.DatabaseCollector
import com.typesafe.config.ConfigFactory
import org.aspectj.lang.{JoinPoint, ProceedingJoinPoint}
import org.aspectj.lang.annotation._

import scala.util.Random


@Aspect
class MethodBang {
  val configFile = getClass.getClassLoader.getResource("remote_application.conf").getFile
  val config = ConfigFactory.parseFile(new File(configFile))
  val system = ActorSystem("RemoteSystem", config)
  val collector = system.actorOf(DatabaseCollector.props(config), name = "collector")

  //todo it only works for trait DistributedStackTrace
  @Pointcut("call(* akka_debugging.DistributedStackTrace$class.aroundReceive(..))")
  def aroundReceivePointcut(): Unit = {}

  @Around("akka.MethodBang.aroundReceivePointcut()")
  def aspectAroundReceive(joinPoint: ProceedingJoinPoint): AnyRef = {
    val actor = joinPoint.getArgs()(0)
//    println(actor.getClass)
    val receive = joinPoint.getArgs()(1)
    val message = joinPoint.getArgs()(2) match {
      case msgWrapper: MessageWrapper =>
        println("RECEIVED: " + msgWrapper.id + " -> " + msgWrapper.msg)
        actor match {
          case act: DistributedStackTrace => act.ZMIENNA = msgWrapper.id
          case _ =>
        }
        collector ! CollectorMessage(msgWrapper.id, None, Some(actor.toString))
        msgWrapper.msg
      case msg =>
        println("NEW MESSAGE: " + msg)
        msg
    }

//    println("RECEIVED: " + actor + " " + receive + " " + joinPoint.getArgs()(2))


    val newArgsArray = Array(actor, receive, message)
    joinPoint.proceed(newArgsArray)
//    joinPoint.proceed()
  }

  //todo - what when scheduler send messages?
//  @Pointcut("call(* akka.actor.ScalaActorRef.$bang(..)) && (within(com.example.actors.FirstActor) || within(com.example.actors.SecondActor) || within(com.example.actors.ThirdActor))")// ") // && within(User) doesn't work
  @Pointcut("call(* akka.actor.ScalaActorRef.$bang(..)) && <<<ACTORS>>>")
  def withinUnreliable(): Unit = {}

  @Around("akka.MethodBang.withinUnreliable()") //actorRef is sender!
  def aspectA(joinPoint: ProceedingJoinPoint): Any = {
    val msg = joinPoint.getArgs()(0)
    val actorRef = joinPoint.getArgs()(1)
//    println(actorRef.asInstanceOf[RepointableActorRef].underlying.asInstanceOf[ActorCell].actor)
    val actor = actorRef.asInstanceOf[RepointableActorRef].underlying.asInstanceOf[ActorCell].actor

    val random = Random.nextInt()
    println("SENT: " + random + " -> " + joinPoint.getArgs()(0))

    val zm = actor.asInstanceOf[DistributedStackTrace].ZMIENNA
    collector ! RelationMessage(zm, random)
//    println("before message: " + zm)

    collector ! CollectorMessage(random, Some(actor.toString), None)

    val newArgsArray = Array[AnyRef](MessageWrapper(random, msg), actorRef)
    joinPoint.proceed(newArgsArray)

//    println("bang method within UnreliableWorker: " + msg + " " + actorRef)
//    collector ! CollectorMessage(callee, UUID.randomUUID(), msg, Thread.currentThread().getStackTrace.drop(2))
  }

//  @AfterThrowing(pointcut = "execution(* akka.actor.Actor$class.aroundReceive(..))", throwing = "error")
//  def afterThrowingMethod(joinPoint: JoinPoint, error: Throwable): Unit = {
//    println("AFTER THROWING METHOD: \n" + error)
//  }
}