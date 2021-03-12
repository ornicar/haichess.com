package lila

import lila.socket.WithSocket

package object home extends PackageObject with WithSocket {

  private[home] def logger = lila.log("home")
}
