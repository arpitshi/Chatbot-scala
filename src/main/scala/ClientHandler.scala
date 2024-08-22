import java.io._
import java.net.Socket
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.mutable
import scala.util.control.NonFatal

class ClientHandler(socket: Socket) extends Runnable {

  // Logger setup using SLF4J
  private val logger: Logger = LoggerFactory.getLogger(classOf[ClientHandler])

  // Initialize input and output streams
  private val bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
  private val bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream))
  private val clientUsername: String = bufferedReader.readLine()

  // Add the new client to the clientHandlers set and notify others
  ClientHandler.clientHandlers.synchronized {
    ClientHandler.clientHandlers += this
    val joinMessage = s"Server: $clientUsername has entered the chat room."
    broadcastMessage(joinMessage)
    sendActiveClientsList()
    logger.info(joinMessage) // Log the event
  }

  // Run method to handle client communication
  override def run(): Unit = {
    try {
      var messageFromClient: String = null
      while (socket.isConnected && {
        messageFromClient = bufferedReader.readLine()
        messageFromClient != null
      }) {
        if (messageFromClient.contains(":")) {
          handleMessage(messageFromClient)
        } else {
          broadcastMessage(messageFromClient)
        }
      }
    } catch {
      case ex: IOException =>
        logger.warn(s"IO Exception in ClientHandler for $clientUsername", ex)
      case NonFatal(ex) =>
        logger.error(s"Non-fatal exception in ClientHandler for $clientUsername", ex)
    } finally {
      removeClientHandler()
      closeAll()
    }
  }

  // Handle incoming messages
  private def handleMessage(message: String): Unit = {
    val messageParts = message.split(":", 3)
    val messageType = messageParts(1).trim.toLowerCase

    messageType match {
      case "private" => sendPrivateMessage(message)
      case _         => broadcastMessage(message)
    }
  }

  // Send a private message to a specific client
  private def sendPrivateMessage(message: String): Unit = {
    val parts = message.split(":", 4)
    if (parts.length == 4) {
      val targetUser = parts(2).trim
      val privateMessage = parts(3).trim

      ClientHandler.clientHandlers.synchronized {
        ClientHandler.clientHandlers.find(_.clientUsername == targetUser) match {
          case Some(clientHandler) =>
            try {
              clientHandler.sendMessageToClient(s"[Private Message] $clientUsername: $privateMessage")
              logger.info(s"$clientUsername sent a private message to $targetUser") // Log the event
            } catch {
              case ex: IOException =>
                logger.warn(s"IO Exception when sending private message from $clientUsername to $targetUser", ex)
                closeAll()
              case NonFatal(ex) =>
                logger.error(s"Non-fatal exception when sending private message from $clientUsername to $targetUser", ex)
                closeAll()
            }
          case None =>
            sendMessageToClient(s"Server: No user found with the username $targetUser")
        }
      }
    } else {
      sendMessageToClient("Server: Invalid private message format. Use: username:private:targetUser:message")
    }
  }

  // Broadcast a message to all clients except the sender
  private def broadcastMessage(message: String): Unit = {
    ClientHandler.clientHandlers.synchronized {
      ClientHandler.clientHandlers
        .filterNot(_.clientUsername == clientUsername)
        .foreach { clientHandler =>
          try {
            clientHandler.sendMessageToClient(message)
          } catch {
            case ex: IOException =>
              logger.warn(s"IO Exception when broadcasting message from $clientUsername", ex)
              closeAll()
            case NonFatal(ex) =>
              logger.error(s"Non-fatal exception when broadcasting message from $clientUsername", ex)
              closeAll()
          }
        }
    }
  }

  // Send the list of active clients to the newly connected client
  private def sendActiveClientsList(): Unit = {
    val activeClients = ClientHandler.clientHandlers.map(_.clientUsername).mkString(", ")
    sendMessageToClient(s"Active clients: $activeClients")
    logger.info(s"$clientUsername received the active clients list") // Log the event
  }

  // Send a message to this client
  private def sendMessageToClient(message: String): Unit = {
    try {
      bufferedWriter.write(message)
      bufferedWriter.newLine()
      bufferedWriter.flush()
    } catch {
      case ex: IOException =>
        logger.warn(s"IO Exception when sending message to $clientUsername", ex)
        closeAll()
      case NonFatal(ex) =>
        logger.error(s"Non-fatal exception when sending message to $clientUsername", ex)
        closeAll()
    }
  }

  // Remove this client from the set and notify others
  private def removeClientHandler(): Unit = {
    ClientHandler.clientHandlers.synchronized {
      ClientHandler.clientHandlers -= this
      val leaveMessage = s"Server: $clientUsername has left the chat room."
      broadcastMessage(leaveMessage)
      logger.info(leaveMessage) // Log the event
    }
  }

  // Close all resources
  private def closeAll(): Unit = {
    try {
      Option(bufferedReader).foreach(_.close())
      Option(bufferedWriter).foreach(_.close())
      Option(socket).foreach(_.close())
      logger.info(s"Resources closed for $clientUsername") // Log the event
    } catch {
      case e: Exception =>
        logger.error(s"Exception when closing resources for $clientUsername", e)
    }
  }
}

object ClientHandler {
  val clientHandlers: mutable.Set[ClientHandler] = mutable.Set.empty
}

