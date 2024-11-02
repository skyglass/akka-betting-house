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
    session.db.autoCommit { implicit dbSession =>
      sql"""
			INSERT INTO
			    wallet_request (requestId)
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
            SELECT EXISTS(SELECT 1 FROM wallet_request WHERE requestId = $requestId) AS "walletRequestExists"
         """
        .map(rs => rs.boolean("walletRequestExists"))
        .single()
        .apply()
        .getOrElse(false)
    }
  }
}
