package example.betting

import akka.actor.testkit.typed.scaladsl.{
  LogCapturing,
  ScalaTestWithActorTestKit
}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import com.typesafe.config.ConfigFactory
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior
}
import akka.persistence.typed.PersistenceId
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityRef,
  EntityTypeKey
}
import akka.cluster.typed.{ Cluster, Join }
import example.betting.Bet.OpenState

import java.time.OffsetDateTime
import scala.concurrent.duration._
import org.scalatest.concurrent.Eventually

object IntegrationSpec {
  val config = ConfigFactory.parseString(
    """
      akka.actor.provider = cluster
      akka.actor.serialization-bindings {
        "example.betting.CborSerializable" = jackson-cbor
      } 
      akka.remote.classic.netty.tcp.port = 0
      akka.remote.artery.canonical.port = 12345
      akka.remote.artery.canonical.hostname = 127.0.0.1
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.journal.inmem.test-serialization = on
      """)

}

class IntegrationSpec
    extends ScalaTestWithActorTestKit(IntegrationSpec.config)
    with AnyWordSpecLike
    with Matchers
    with Eventually
    with LogCapturing {

  private val sharding = ClusterSharding(system)

  override def beforeAll() {
    super.beforeAll()
    Cluster(system).manager ! Join(Cluster(system).selfMember.address)

    sharding.init(
      Entity(Wallet.typeKey)(createBehavior = entityContext =>
        Wallet(entityContext.entityId)))

    sharding.init(
      Entity(Market.typeKey)(createBehavior = entityContext =>
        Market(entityContext.entityId)))

    sharding.init(
      Entity(Bet.typeKey)(createBehavior = entityContext =>
        Bet(entityContext.entityId)))
  }

  "a bet" should {
    "fail if the odds from the market are less than the bet ones" in {

      val walletProbe = createTestProbe[Wallet.Response]

      val wallet = sharding.entityRefFor(Wallet.typeKey, "walletId1")

      wallet ! Wallet.AddFunds(100, walletProbe.ref)

      walletProbe.expectMessage(10.seconds, Wallet.Accepted)

      val marketProbe = createTestProbe[Market.Response]

      val market = sharding.entityRefFor(Market.typeKey, "marketId1")

      market ! Market.Open(
        Market.Fixture("fixtureId1", "RM", "MU"),
        Market.Odds(1.25, 1.75, 1.05),
        OffsetDateTime.now.toInstant.toEpochMilli,
        marketProbe.ref)

      marketProbe.expectMessage(10.seconds, Market.Accepted)

      val bet = sharding.entityRefFor(Bet.typeKey, "betId1")

      val betProbe = createTestProbe[Bet.Response]

      bet ! Bet.Open(
        "walletId1",
        "marketId1",
        1.26,
        100,
        0,
        betProbe.ref)

      eventually {

        bet ! Bet.GetState(betProbe.ref)

        val expected = Bet.FailedState(
          Bet
            .Status(
              "betId1",
              "walletId1",
              "marketId1",
              1.26,
              100,
              0,
              false,
              false),
          "market odds [Some(1.25)] are less than the bet odds")

        betProbe.expectMessage(Bet.CurrentState(expected))
      }

      wallet ! Wallet.CheckFunds(walletProbe.ref)
      walletProbe.expectMessage(Wallet.CurrentBalance(100))
    }
  }

  "a bet" should {
    "pass if the odds from the market are equal to the bet ones" in {

      val walletId = "walletId2"
      val marketId = "marketId2"
      val betId = "betId2"

      val walletProbe = createTestProbe[Wallet.Response]

      val wallet = sharding.entityRefFor(Wallet.typeKey, walletId)

      wallet ! Wallet.AddFunds(100, walletProbe.ref)

      walletProbe.expectMessage(10.seconds, Wallet.Accepted)

      val marketProbe = createTestProbe[Market.Response]

      val market = sharding.entityRefFor(Market.typeKey, marketId)

      market ! Market.Open(
        Market.Fixture("fixtureId2", "RM", "MU"),
        Market.Odds(1.25, 1.75, 1.05),
        OffsetDateTime.now.toInstant.toEpochMilli,
        marketProbe.ref)

      marketProbe.expectMessage(10.seconds, Market.Accepted)

      val bet = sharding.entityRefFor(Bet.typeKey, betId)

      val betProbe = createTestProbe[Bet.Response]

      bet ! Bet.Open(walletId, marketId, 1.25, 100, 0, betProbe.ref)

      eventually {

        bet ! Bet.GetState(betProbe.ref)

        val expected = Bet.OpenState(
          Bet
            .Status(
              betId,
              walletId,
              marketId,
              1.25,
              100,
              0,
              true,
              true))

        betProbe.expectMessage(Bet.CurrentState(expected))
      }

      wallet ! Wallet.CheckFunds(walletProbe.ref)
      walletProbe.expectMessage(Wallet.CurrentBalance(0))
    }
  }

  "a bet" should {
    "pass if the odds from the market are higher than the bet ones" in {

      val walletId = "walletId3"
      val marketId = "marketId3"
      val betId = "betId3"

      val walletProbe = createTestProbe[Wallet.Response]

      val wallet = sharding.entityRefFor(Wallet.typeKey, walletId)

      wallet ! Wallet.AddFunds(100, walletProbe.ref)

      walletProbe.expectMessage(Wallet.Accepted)

      val marketProbe = createTestProbe[Market.Response]

      val market = sharding.entityRefFor(Market.typeKey, marketId)

      market ! Market.Open(
        Market.Fixture("fixtureId3", "RM", "MU"),
        Market.Odds(1.25, 1.75, 1.05),
        OffsetDateTime.now.toInstant.toEpochMilli,
        marketProbe.ref)

      marketProbe.expectMessage(10.seconds, Market.Accepted)

      val bet = sharding.entityRefFor(Bet.typeKey, betId)

      val betProbe = createTestProbe[Bet.Response]

      bet ! Bet.Open(walletId, marketId, 1.05, 100, 0, betProbe.ref)

      eventually {

        bet ! Bet.GetState(betProbe.ref)

        val expected = Bet.OpenState(
          Bet
            .Status(
              betId,
              walletId,
              marketId,
              1.05,
              100,
              0,
              true,
              true))

        betProbe.expectMessage(Bet.CurrentState(expected))
      }

      wallet ! Wallet.CheckFunds(walletProbe.ref)
      walletProbe.expectMessage(Wallet.CurrentBalance(0))
    }
  }

  "a bet" should {
    "fail if market is already closed" in {

      val walletProbe = createTestProbe[Wallet.Response]

      val wallet = sharding.entityRefFor(Wallet.typeKey, "walletId4")

      wallet ! Wallet.AddFunds(100, walletProbe.ref)

      walletProbe.expectMessage(10.seconds, Wallet.Accepted)

      val marketProbe = createTestProbe[Market.Response]

      val market = sharding.entityRefFor(Market.typeKey, "marketId4")

      market ! Market.Open(
        Market.Fixture("fixtureId4", "RM", "MU"),
        Market.Odds(1.25, 1.75, 1.05),
        OffsetDateTime.now.toInstant.toEpochMilli,
        marketProbe.ref)

      marketProbe.expectMessage(10.seconds, Market.Accepted)

      market ! Market.Close(0, marketProbe.ref)

      val bet = sharding.entityRefFor(Bet.typeKey, "betId4")

      val betProbe = createTestProbe[Bet.Response]

      bet ! Bet.Open(
        "walletId4",
        "marketId4",
        1.26,
        100,
        0,
        betProbe.ref)

      eventually {

        bet ! Bet.GetState(betProbe.ref)

        val expected = Bet.FailedState(
          Bet
            .Status(
              "betId4",
              "walletId4",
              "marketId4",
              1.26,
              100,
              0,
              true,
              true),
          "market [marketId4] is closed, no more bets allowed")

        betProbe.expectMessage(Bet.CurrentState(expected))
      }

      wallet ! Wallet.CheckFunds(walletProbe.ref)
      walletProbe.expectMessage(Wallet.CurrentBalance(100))
    }
  }

}
