package lila.offlineContest

case class OffManualPairingSource(
    board: Option[OffBoard],
    color: Option[chess.Color],
    player: Option[OffPlayer],
    isBye: Boolean
) {

  def board_ = board.get
  def color_ = color.get
  def player_ = player.get

}
