package lila.puzzle

import play.api.data.Form

class ThemeShow(
    val id: PuzzleId,
    val searchForm: Form[_],
    val markTags: Set[String],
    val rnf: Boolean,
    val showDrawer: Boolean
)

object ThemeShow {

  def apply(
    id: PuzzleId,
    searchForm: Form[_] = null,
    markTags: Set[String] = Set.empty,
    rnf: Boolean = false,
    showDrawer: Boolean = false
  ): Option[ThemeShow] = new ThemeShow(id, searchForm, markTags, rnf, showDrawer).some
}
