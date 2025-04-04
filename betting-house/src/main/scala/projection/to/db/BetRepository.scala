package example.repository.scalike

import scalikejdbc._

final case class StakePerResult(sum: Double, result: Int)

final case class BetData(
    betId: String,
    walletId: String,
    marketId: String,
    marketName: String,
    odds: Double,
    stake: Int,
    result: Int)

trait BetRepository {

  def addBet(
      betId: String,
      walletId: String,
      marketId: String,
      marketName: String,
      odds: Double,
      stake: Int,
      result: Int,
      session: ScalikeJdbcSession): Unit
  def getBetPerMarketTotalStake(
      marketId: String,
      session: ScalikeJdbcSession): List[StakePerResult]

  def getBetsForMarket(
      marketId: String,
      session: ScalikeJdbcSession): List[BetData]

  def getBetsForPlayer(
      walletId: String,
      session: ScalikeJdbcSession): List[BetData]

}

class BetRepositoryImpl extends BetRepository {

  override def addBet(
      betId: String,
      walletId: String,
      marketId: String,
      marketName: String,
      odds: Double,
      stake: Int,
      result: Int,
      session: ScalikeJdbcSession): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
			INSERT INTO
			    bet_wallet_market (betId, walletId, marketId, marketName, odds, stake, result)
				VALUES ($betId, $walletId, $marketId, $marketName, $odds, $stake, $result)
			   ON CONFLICT (betId) DO NOTHING
			""".executeUpdate().apply()
    }

  }

  override def getBetPerMarketTotalStake(
      marketId: String,
      session: ScalikeJdbcSession): List[StakePerResult] = {
    session.db.readOnly { implicit dbSession =>
      sql"""SELECT sum(stake * odds), result FROM bet_wallet_market WHERE marketId = $marketId GROUP BY marketId, result"""
        .map(rs => StakePerResult(rs.double("sum"), rs.int("result")))
        .list
        .apply()
    }
  }

  override def getBetsForMarket(
      marketId: String,
      session: ScalikeJdbcSession): List[BetData] = {
    session.db.readOnly { implicit dbSession =>
      sql"""SELECT betId, walletId, marketId, marketName, odds, stake, result FROM bet_wallet_market WHERE marketId = $marketId"""
        .map(
          rs =>
            BetData(
              rs.string("betId"),
              rs.string("walletId"),
              rs.string("marketId"),
              rs.string("marketName"),
              rs.double("odds"),
              rs.int("stake"),
              rs.int("result")))
        .list
        .apply()
    }
  }

  override def getBetsForPlayer(
      walletId: String,
      session: ScalikeJdbcSession): List[BetData] = {
    session.db.readOnly { implicit dbSession =>
      sql"""SELECT betId, walletId, marketId, marketName, odds, stake, result FROM bet_wallet_market WHERE walletId = $walletId"""
        .map(
          rs =>
            BetData(
              rs.string("betId"),
              rs.string("walletId"),
              rs.string("marketId"),
              rs.string("marketName"),
              rs.double("odds"),
              rs.int("stake"),
              rs.int("result")))
        .list
        .apply()
    }
  }

}
