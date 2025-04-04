package betting.house.projection.proto

import akka.actor.typed.{ ActorSystem, DispatcherSelector }
import scala.concurrent.{ ExecutionContext, Future }

import example.repository.scalike.{
  BetRepository,
  ScalikeJdbcSession
}

class BetProjectionServiceImpl(
    system: ActorSystem[_],
    betRepository: BetRepository)
    extends BetProjectionService {

  implicit private val jdbcExecutor: ExecutionContext =
    system.dispatchers.lookup(
      DispatcherSelector.fromConfig(
        "akka.projection.jdbc.blocking-jdbc-dispatcher"))

  def getBetByMarket(
      in: MarketIdsBet): scala.concurrent.Future[SumStakes] = {
    Future {
      ScalikeJdbcSession.withSession { session =>
        val sumStakes = betRepository
          .getBetPerMarketTotalStake(in.marketId, session)
          .map { each =>
            SumStake(each.sum, each.result)
          }
        SumStakes(sumStakes)
      }
    }
  }

  def getBetsForMarket(
      in: MarketIdsBet): scala.concurrent.Future[BetDataList] = {
    Future {
      ScalikeJdbcSession.withSession { session =>
        val betDataList = betRepository
          .getBetsForMarket(in.marketId, session)
          .map { each =>
            BetData(
              each.betId,
              each.walletId,
              each.marketId,
              each.marketName,
              each.odds,
              each.stake,
              each.result)
          }
        BetDataList(betDataList)
      }
    }
  }

  def getBetsForPlayer(
      in: WalletIdsBet): scala.concurrent.Future[BetDataList] = {
    Future {
      ScalikeJdbcSession.withSession { session =>
        val betDataList = betRepository
          .getBetsForPlayer(in.walletId, session)
          .map { each =>
            BetData(
              each.betId,
              each.walletId,
              each.marketId,
              each.marketName,
              each.odds,
              each.stake,
              each.result)
          }
        BetDataList(betDataList)
      }
    }
  }
}
