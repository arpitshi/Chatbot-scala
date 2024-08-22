import java.io._
import java.net.Socket
import scala.io.StdIn

class Client(socket: Socket, username: String) {

  private val bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream))
  private val bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream))

  def sendMessage(): Unit = {
    try {
      // Send the username to the server upon connection
      sendToServer(username)

      // Continuously read input from the user and send it to the server
      while (socket.isConnected) {
        val messageToSend = StdIn.readLine()
        sendToServer(s"$username: $messageToSend")
      }
    } catch {
      case _: IOException => closeAll()
      case _: Exception   => closeAll()
    }
  }

  def listenForMessage(): Unit = {
    new Thread(() => {
      try {
        while (socket.isConnected) {
          val msgFromServer = bufferedReader.readLine()
          if (msgFromServer != null) {
            println(msgFromServer)
          }
        }
      } catch {
        case _: IOException => closeAll()
        case _: Exception   => closeAll()
      }
    }).start()
  }

  private def sendToServer(message: String): Unit = {
    try {
      bufferedWriter.write(message)
      bufferedWriter.newLine()
      bufferedWriter.flush()
    } catch {
      case _: IOException => closeAll()
      case _: Exception   => closeAll()
    }
  }

  private def closeAll(): Unit = {
    try {
      Option(bufferedReader).foreach(_.close())
      Option(bufferedWriter).foreach(_.close())
      Option(socket).foreach(_.close())
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }
}

object ClientSideApplication extends App {
  println("Enter your username: ")
  val username = StdIn.readLine()
  val socket = new Socket("localhost", 5555)
  val client = new Client(socket, username)

  client.listenForMessage()
  client.sendMessage()
}
