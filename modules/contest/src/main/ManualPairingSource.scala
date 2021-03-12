package lila.contest

case class ManualPairingSource(
    board: Option[Board],
    color: Option[chess.Color],
    player: Option[Player],
    isBye: Boolean
) {

  def board_ = board.get
  def color_ = color.get
  def player_ = player.get

}
