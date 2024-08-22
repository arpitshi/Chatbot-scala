import java.io.IOException
import java.net.{ServerSocket, Socket}

class Server(serverSocket: ServerSocket) {
  def startServer(): Unit = {
    try {
      while (!serverSocket.isClosed) {
        val socket: Socket = serverSocket.accept()
        println("Someone has connected to the server!")
        val clientHandler = new ClientHandler(socket)
        new Thread(clientHandler).start()
      }
    } catch {
      case e:IOException => println("Input output Exception: " + e.getMessage)
      case e:Exception => println("Exception")
    }
    finally {
      serverSocket.close()
    }
  }
}
object ServerSideApplication extends App {
  val serverSocket = new ServerSocket(5555)
  val server = new Server(serverSocket)
  server.startServer()
}
