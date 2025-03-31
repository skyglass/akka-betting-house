package example.market.grpc

import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity
}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.google.protobuf.empty.Empty
import example.betting.{ Market, Wallet }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class MarketServiceImplSharding(
    implicit sharding: ClusterSharding,
    system: ActorSystem[_])
    extends MarketService {

  implicit val timeout: Timeout = 30.seconds
  implicit val executionContext: ExecutionContext =
    ExecutionContext.global

  sharding.init(Entity(Market.typeKey)(entityContext =>
    Market(entityContext.entityId)))

  override def cancel(in: example.market.grpc.CancelMarket)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.typeKey, in.marketId)
    def auxCancel(reason: String)(
        replyTo: ActorRef[Market.Response]) =
      Market.Cancel(in.reason, replyTo)

    market
      .ask(auxCancel(in.reason))
      .mapTo[Market.Response]
      .map { response =>
        response match {
          case Market.Accepted =>
            example.market.grpc.Response("initialized")
          case Market.RequestUnaccepted(reason) =>
            example.market.grpc
              .Response(s"market NOT cancelled because [$reason]")
        }
      }
  }

  override def closeMarket(in: example.market.grpc.CloseMarketMessage)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.typeKey, in.marketId)

    def auxClose(result: Int)(
        replyTo: ActorRef[Market.Response]): Market.Close = {
      Market.Close(result, replyTo)
    }

    market
      .ask(auxClose(in.result))
      .mapTo[Market.Response]
      .map { response =>
        response match {
          case Market.Accepted =>
            example.market.grpc.Response("initialized")
          case Market.RequestUnaccepted(reason) =>
            example.market.grpc
              .Response(s"market NOT closed because [$reason]")
        }
      }
  }

  override def getState(in: example.market.grpc.MarketId)
      : scala.concurrent.Future[example.market.grpc.MarketData] = {
    val market = sharding.entityRefFor(Market.typeKey, in.marketId)

    market.ask(Market.GetState).mapTo[Market.CurrentState].map {
      state =>
        val (
          marketId,
          Market.Fixture(id, homeTeam, awayTeam),
          Market.Odds(winHome, winAway, draw),
          result,
          opensAt,
          open) = (
          state.status.marketId,
          state.status.fixture,
          state.status.odds,
          state.status.result,
          state.status.opensAt,
          state.status.open)

        MarketData(
          marketId,
          Some(FixtureData(id, homeTeam, awayTeam)),
          Some(OddsData(winHome, winAway, draw)),
          MarketData.Result.fromValue(result),
          opensAt,
          open)
    }

  }

  override def open(in: example.market.grpc.MarketData)
      : scala.concurrent.Future[example.market.grpc.Response] = {
    val market = sharding.entityRefFor(Market.typeKey, in.marketId)

    def auxInit(in: MarketData)(
        replyTo: ActorRef[Market.Response]) = {

      val fixture = in.fixture match {
        case Some(FixtureData(id, homeTeam, awayTeam, _)) =>
          Market.Fixture(id, homeTeam, awayTeam)
        case None =>
          throw new IllegalArgumentException(
            "Fixture is empty. Not allowed")
      }

      val odds = in.odds match {
        case Some(OddsData(winHome, winAway, tie, _)) =>
          Market.Odds(winHome, winAway, tie)
        case None =>
          throw new IllegalArgumentException(
            "Odds are empty. Not allowed")
      }

      val opensAt = in.opensAt

      Market.Open(fixture, odds, opensAt, replyTo)

    }

    market
      .ask(auxInit(in))
      .mapTo[Market.Response]
      .map { response =>
        response match {
          case Market.Accepted =>
            example.market.grpc.Response("initialized")
          case Market.RequestUnaccepted(reason) =>
            example.market.grpc
              .Response(s"market NOT initialized because [$reason]")
        }
      }
  }

  override def update(in: example.market.grpc.MarketData)
      : scala.concurrent.Future[example.market.grpc.Response] = {

    def auxUpdate(marketData: MarketData)(
        replyTo: ActorRef[Market.Response]): Market.Update = {

      val odds = marketData.odds.map(m =>
        Market.Odds(m.winHome, m.winAway, m.tie))

      //TODO: conversion from marketData.opensAt Long to scala.Long doesn't work, temporarily disabled setting opensAt Long value in update method until further research (market open date is not used anywhere yet)
      val opensAt = Some(marketData.opensAt)

      Market.Update(odds, opensAt, replyTo)

    }

    val market = sharding.entityRefFor(Market.typeKey, in.marketId)

    market
      .ask(auxUpdate(in))
      .mapTo[Market.Response]
      .map { response =>
        response match {
          case Market.Accepted =>
            example.market.grpc.Response("Updated")
          case Market.RequestUnaccepted(reason) =>
            example.market.grpc
              .Response(s"market NOT updated because [$reason]")
        }
      }

  }

  override def getAllMarkets(in: Empty): Future[MarketList] = {
    val readJournal = PersistenceQuery(system)
      .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

    val marketIdsFuture: Future[Seq[String]] =
      readJournal
        .currentPersistenceIds()
        .runWith(Sink.seq)
        .map(_.filter(_.startsWith(Market.typeKey.name + "|")))

    marketIdsFuture.flatMap { marketIds =>
      val marketFutures = marketIds.map { marketId =>
        val sanitizedMarketId =
          marketId.replace(Market.typeKey.name + "|", "")
        val entityRef =
          sharding.entityRefFor(Market.typeKey, sanitizedMarketId)
        entityRef.ask(Market.GetState).mapTo[Market.CurrentState]
      }

      Future.sequence(marketFutures).map { marketStates =>
        val markets = marketStates.map { state =>
          val (marketId, fixture, odds, result, opensAt, open) = (
            state.status.marketId,
            state.status.fixture,
            state.status.odds,
            state.status.result,
            state.status.opensAt,
            state.status.open)

          MarketData(
            marketId,
            Some(
              FixtureData(
                fixture.id,
                fixture.homeTeam,
                fixture.awayTeam)),
            Some(OddsData(odds.winHome, odds.winAway, odds.draw)),
            MarketData.Result.fromValue(result),
            opensAt,
            open)
        }
        MarketList(markets)
      }
    }
  }
}
