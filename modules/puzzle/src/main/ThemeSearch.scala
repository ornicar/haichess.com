package lila.puzzle

import play.api.data.Form

class ThemeShow(
    val id: PuzzleId,
    val searchForm: Form[_],
    val markTags: Set[String],
    val rnf: Boolean,
    val history: Option[ThemeRecord],
    val showDrawer: Boolean
)

object ThemeShow {

  def apply(
    id: PuzzleId,
    searchForm: Form[_] = null,
    markTags: Set[String] = Set.empty,
    rnf: Boolean = false,
    history: Option[ThemeRecord] = None,
    showDrawer: Boolean = false
  ): Option[ThemeShow] = new ThemeShow(id, searchForm, markTags, rnf, history, showDrawer).some
}
