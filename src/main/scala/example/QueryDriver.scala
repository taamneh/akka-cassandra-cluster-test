package example

import com.datastax.driver.core.Row
import com.typesafe.scalalogging.slf4j.Logging
import com.datastax.driver.core.querybuilder.QueryBuilder


object QueryDriver extends Logging {
  val db = new CassandraConnector("API Query Driver", true)
  val session = db.cluster.connect("factorial")

  def keyExist( key : Int) : Boolean = {
    session.execute(s"SELECT status FROM results WHERE input=${key};").all().size > 0
  }

  def createEntry( key : Int ) : Option[Result] = {
    try {
      session.execute(s"INSERT INTO results (input, status) VALUES ($key, 'accepted')")
      logger.info("\n CREATE" + key + " DONE \n\n")
      Some(Result(input = key, status = "accepted"))
    } catch {
      case ex : Exception => {
        logger.error(ex + s"Failed Query : INSERT INTO results (input, status) VALUES ($key, 'accepted')")
        None
      }
    }
  }

  def delete( key : Int ) : Boolean = {
    try {
      session.execute(s"DELETE FROM results WHERE input=$key")
      logger.info("\n DELETE" + key + " DONE \n\n")
      true
    } catch {
      case ex : Exception => {
        logger.error(ex + s"Failed Query : DELETE FROM results WHERE input=$key")
        false
      }
    }
  }

  def getResult( key : Int ) : Option[Result] = {
    val t0 = System.currentTimeMillis()
    try {
      session.execute(s"SELECT * FROM results WHERE input=${key};").one() match {
        case r: Row => {
          val t1 = System.currentTimeMillis()
          val res = mapResult(r)
          val t2 = System.currentTimeMillis()
          logger.info(s"\n RETURN FROM GET ${res.input}:${res.status}:${res.result.isDefined}:${res.calcTime.isDefined}" +
            s"\n querytime:${(t1-t0)/1000000.0}  maptime:${(t2-t1)/1000000.0}\n")
          Some(res)
        }
        case _ => {
          logger.warn(s"Did not find a entry for key:${key} in the data store")
          None
        }
      }
    } catch {
      case ex : Exception => {
        logger.error(ex + s"Failed Query : SELECT * FROM results WHERE input=${key}")
        None
      }
    }
  }

  def close() = db.cluster.close()

  def mapResult( row : Row ) : Result = {
    Result(
      input = row.getInt("input"),
      status = row.getString("status"),
      result = row.isNull("result") match {
        case true => None
        case false => Some( row.getString("result") )
      },
      calcTime = row.isNull("calctime") match{
        case true => None
        case false => Some(row.getDouble("calctime").toString + "ms")
      }
    )
  }
}
