package example.repository.scalike

import scalikejdbc._

trait WalletRepository {

  def addWalletRequest(
      requestId: String,
      session: ScalikeJdbcSession): Unit
  def walletRequestExists(
      requestId: String,
      session: ScalikeJdbcSession): Boolean

}

class WalletRepositoryImpl extends WalletRepository {

  def addWalletRequest(
      requestId: String,
      session: ScalikeJdbcSession): Unit = {
    session.db.withinTx { implicit dbSession =>
      sql"""
			INSERT INTO
			    wallet_request (request_id)
				VALUES ($requestId)
			   ON CONFLICT (requestId) DO NOTHING
			""".executeUpdate().apply()
    }

  }

  override def walletRequestExists(
      requestId: String,
      session: ScalikeJdbcSession): Boolean = {
    session.db.readOnly { implicit dbSession =>
      sql"""
            SELECT requestId FROM wallet_request WHERE requestId = $requestId
         """
        .map(rs => true)
        .single()
        .apply()
        .getOrElse(false)
    }
  }
}
