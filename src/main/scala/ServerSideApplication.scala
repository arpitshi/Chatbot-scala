//import java.io.IOException
//import java.net.{ServerSocket, Socket}
//
//class Server(serverSocket: ServerSocket) {
//  def startServer(): Unit = {
//    try {
//      while (!serverSocket.isClosed) {
//        val socket: Socket = serverSocket.accept()
//        println("Someone has connected to the server!")
//        val clientHandler = new ClientHandler(socket)
//        new Thread(clientHandler).start()
//      }
//    } catch {
//      case e:IOException => println("Input output Exception: " + e.getMessage)
//      case e:Exception => println("Exception")
//    }
//    finally {
//      serverSocket.close()
//    }
//  }
//}
//object ServerSideApplication extends App {
//  val serverSocket = new ServerSocket(5555)
//  val server = new Server(serverSocket)
//  server.startServer()
//}


import java.io.IOException
import java.net.{ServerSocket, Socket}
import org.slf4j.LoggerFactory

class Server(serverSocket: ServerSocket) {
  private val logger = LoggerFactory.getLogger(classOf[Server])
  @volatile private var isRunning = true
  private val clientHandlers = scala.collection.mutable.Set[ClientHandler]()

  def startServer(): Unit = {
    try {
      while (isRunning && !serverSocket.isClosed) {
        try {
          val socket: Socket = serverSocket.accept()
          logger.info("Someone has connected to the server!")
          val clientHandler = new ClientHandler(socket)
          clientHandlers.synchronized {
            clientHandlers += clientHandler
          }
          new Thread(clientHandler).start()
        } catch {
          case e: IOException =>
            logger.error("I/O Exception: " + e.getMessage, e)
            shutdownServer()
        }
      }
    } catch {
      case e: Exception =>
        logger.error("Exception in server: ", e)
        shutdownServer()
    } finally {
      if (!serverSocket.isClosed) serverSocket.close()
    }
  }

  def shutdownServer(): Unit = {
    isRunning = false
    logger.info("Shutting down the server and all client connections...")

    // Close all client connections
    clientHandlers.synchronized {
      clientHandlers.foreach(c =>{
        c.sendMessageToClient("[ServerDown]")
        c.closeAll()
      })
      clientHandlers.clear()
    }

    // Close the server socket
    try {
      if (!serverSocket.isClosed) serverSocket.close()
    } catch {
      case e: IOException =>
        logger.error("Error closing the server socket: " + e.getMessage, e)
    }
  }
}

object ServerSideApplication extends App {
  val serverSocket = new ServerSocket(5555)
  val server = new Server(serverSocket)

  // Register a shutdown hook to ensure resources are cleaned up on JVM exit
  Runtime.getRuntime.addShutdownHook(new Thread(() => server.shutdownServer()))

  server.startServer()
}
